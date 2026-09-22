package ru.cert.certificationserver.repository.redis;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.cert.certificationserver.model.entity.UserEntity;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Component
public class JwtProvider {
  @Value("${jwt.secret}")
  private String secret;

  public String generateAccess(UserEntity user, String sessionId, String activeRole) {
    return Jwts.builder()
        .subject(user.getId().toString())
        .claim("sid", sessionId)
        .claim("role", activeRole)
        .issuedAt(new Date())
        .expiration(Date.from(Instant.now().plus(15, ChronoUnit.MINUTES)))
        .signWith(getKey())
        .compact();
  }

  public String generateRefresh(UserEntity user, String sessionId) {
    return Jwts.builder()
        .subject(user.getId().toString())
        .claim("sid", sessionId)
        .issuedAt(new Date())
        .expiration(Date.from(Instant.now().plus(30, ChronoUnit.DAYS)))
        .signWith(getKey())
        .compact();
  }

  public Claims parse(String token) {
    return Jwts.parser()
        .verifyWith(getKey())
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }

  private SecretKey getKey() {
    return Keys.hmacShaKeyFor(secret.getBytes());
  }
}
