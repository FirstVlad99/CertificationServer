package ru.cert.certificationserver.filter;

import io.jsonwebtoken.Claims;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.cert.certificationserver.model.security.CachedAuthUser;
import ru.cert.certificationserver.model.security.UserPrincipal;
import ru.cert.certificationserver.repository.redis.JwtProvider;
import ru.cert.certificationserver.repository.redis.RedisSessionStore;
import ru.cert.certificationserver.service.AuthUserCache;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
  private final JwtProvider jwtProvider;
  private final AuthUserCache authUserCache;
  private final RedisSessionStore sessionStore;
  private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String BEARER_PREFIX = "Bearer ";

  public JwtAuthenticationFilter(
      JwtProvider jwtProvider,
      AuthUserCache authUserCache,
      RedisSessionStore sessionStore
  ) {
    this.jwtProvider = jwtProvider;
    this.authUserCache = authUserCache;
    this.sessionStore = sessionStore;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {

    String access = extractBearerToken(request);

    if (access != null) {
      try {
        Claims claims = jwtProvider.parse(access);
        Long userId = Long.valueOf(claims.getSubject());
        String sessionId = claims.get("sid", String.class);
        String acitveRole = claims.get("role", String.class);

        if (!sessionStore.exists(userId, sessionId)) {
          SecurityContextHolder.clearContext();
        } else {
          // Снимок из кэша (Caffeine, TTL 10с + evict на write-path'ах), а не запрос в БД на каждый
          // запрос. sessionId в снимок не входит - доштамповываем его тут, он свой на каждый запрос.
          CachedAuthUser user = authUserCache.load(userId)
              .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));

          // CachedAuthUser вернет все роли, что есть у пользователя в БД
          // а нам интересна лишь активная роль (под кого заходит пользователь: сотрудник или заказчик)
          List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(acitveRole));

          UserPrincipal principal = new UserPrincipal(
              user.id(),
              user.email(),
              user.passwordHash(),
              authorities,
              sessionId,
              user.enabled()
          );

          if (!principal.isEnabled()) {
            // Деактивированный аккаунт отклоняем структурно. enabled берётся из снимка (evict на смене
            // статуса + TTL 10с как потолок), но мгновенную деактивацию гарантирует не он, а проверка
            // сессии выше: деактивация вызывает terminateAllSessions -> exists() == false -> контекст
            // очищен ещё до этой ветки. Итог тот же, что при отсутствующей сессии (401 на entry point).
            SecurityContextHolder.clearContext();
          } else {
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
            );

            SecurityContextHolder.getContext().setAuthentication(auth);
          }
        }
      } catch (Exception e) {
        log.warn("JWT authentication error", e);
        SecurityContextHolder.clearContext();
      }
    }
    filterChain.doFilter(request, response);
  }

  private String extractBearerToken(HttpServletRequest request) {
    String header = request.getHeader(AUTHORIZATION_HEADER);
    if (header != null && header.startsWith(BEARER_PREFIX)) {
      // считываю accesToken, следующий после "Bearer "
      return header.substring(BEARER_PREFIX.length());
    }
    return null;
  }
}

