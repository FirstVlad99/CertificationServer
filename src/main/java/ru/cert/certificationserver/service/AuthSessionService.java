package ru.cert.certificationserver.service;

import io.jsonwebtoken.Claims;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.cert.certificationserver.dto.auth.event.ActivationEmailEvent;
import ru.cert.certificationserver.dto.auth.event.ResetPasswordEmailEvent;
import ru.cert.certificationserver.dto.auth.request.*;
import ru.cert.certificationserver.exception.AuthException;
import ru.cert.certificationserver.model.entity.UserEntity;
import ru.cert.certificationserver.model.enums.AuthErrorEnum;
import ru.cert.certificationserver.model.enums.ErrorCodeEnum;
import ru.cert.certificationserver.model.enums.UserSystemStatusNameEnum;
import ru.cert.certificationserver.model.redis.AuthSession;
import ru.cert.certificationserver.model.redis.TokenPair;
import ru.cert.certificationserver.repository.UserRepository;
import ru.cert.certificationserver.repository.redis.JwtProvider;
import ru.cert.certificationserver.repository.redis.RedisSessionStore;
import ru.cert.certificationserver.util.EmailNormalizer;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiPredicate;

import static ru.cert.certificationserver.util.HashUtils.sha256;

@Service
public class AuthSessionService {
  private final JwtProvider jwtProvider;
  private final RedisSessionStore sessionStore;
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthUserCache authUserCache;
  private final UserReferenceResolver userReferenceResolver;
  private final ApplicationEventPublisher eventPublisher;
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  public AuthSessionService(JwtProvider jwtProvider, RedisSessionStore sessionStore, UserRepository userRepository, PasswordEncoder passwordEncoder, AuthUserCache authUserCache, UserReferenceResolver userReferenceResolver, ApplicationEventPublisher eventPublisher) {
    this.jwtProvider = jwtProvider;
    this.sessionStore = sessionStore;
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.authUserCache = authUserCache;
    this.userReferenceResolver = userReferenceResolver;
    this.eventPublisher = eventPublisher;
  }

  @Transactional
  public void register(RegisterRequest request) {
    String normalizedEmail = normalizeEmailOrThrow(request.email());
    if (userRepository.existsByEmail(normalizedEmail)) {
      throw new AuthException(ErrorCodeEnum.CONFLICT, AuthErrorEnum.EMAIL_ALREADY_EXISTS);
    }
    UserEntity user = new UserEntity();
    user.setEmail(normalizedEmail);
    user.setPasswordHash(passwordEncoder.encode(request.password()));
    user.setUserStatus(userReferenceResolver.userSystemStatus(UserSystemStatusNameEnum.PENDING_CONFIRMATION));
    userRepository.save(user);
    userRepository.flush();
    // outbox-pattern не использую, потеря письма не критична, т.к. есть resend-activation
    // если бд упала, а кафка нет - транзакция откатится и ActivationEmailListener не отправит письмо
    String code = generateAndSaveCode(
        user.getId(),
        sessionStore::saveIfAbsentActivationCode
    );
    eventPublisher.publishEvent(new ActivationEmailEvent(normalizedEmail, code));
  }

  public void resendActivationCode(ResendActivationRequest request) {
    String normalizedEmail = normalizeEmailOrThrow(request.email());
    userRepository.findByEmail(
        normalizedEmail
    ).ifPresent(user -> {
      // если аккаунт активирован - ничего не отсылаю
      if (user.getUserStatus().getName() != UserSystemStatusNameEnum.PENDING_CONFIRMATION)
        return;
      String code = sessionStore.findActivationCodeByUserId(user.getId())
          .orElseGet(() -> generateAndSaveCode(
              user.getId(),
              sessionStore::saveIfAbsentActivationCode
          ));
      eventPublisher.publishEvent(new ActivationEmailEvent(normalizedEmail, code));
    });
  }

  @Transactional(readOnly = true)
  public TokenPair authenticate(LoginRequest request, String userAgent) {
    String email = request.email();
    String activeRole = request.role();

    UserEntity user = userRepository.findByEmail(email)
        .orElseThrow(() -> new AuthException(ErrorCodeEnum.BAD_REQUEST, AuthErrorEnum.BAD_CREDENTIALS));

    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw new AuthException(
          ErrorCodeEnum.BAD_REQUEST,
          AuthErrorEnum.BAD_CREDENTIALS
      );
    }

    if (user.getUserStatus().getName() == UserSystemStatusNameEnum.PENDING_CONFIRMATION) {
      throw new AuthException(
          ErrorCodeEnum.BAD_REQUEST,
          AuthErrorEnum.ACCOUNT_NOT_ACTIVATED
      );
    }

    if (user.getUserStatus().getName() == UserSystemStatusNameEnum.INACTIVE) {
      throw new AuthException(
          ErrorCodeEnum.BAD_REQUEST,
          AuthErrorEnum.ACCOUNT_INACTIVE
      );
    }

    return createSession(user, userAgent, activeRole);
  }

  @Transactional(readOnly = true)
  public TokenPair refreshSession(String refreshToken) {
    Claims claims = jwtProvider.parse(refreshToken);
    Long userId = Long.valueOf(claims.getSubject());
    String sessionId = claims.get("sid", String.class);
    String activeRole = claims.get("role", String.class);

    AuthSession session = sessionStore.find(userId, sessionId)
        .orElseThrow(() -> new EntityNotFoundException("Session not found"));

    String refreshHash = sha256(refreshToken);
    if (!session.getRefreshHash().equals(refreshHash)) {
      throw new AuthException(
          ErrorCodeEnum.BAD_REQUEST,
          AuthErrorEnum.INVALID_REFRESH_TOKEN
      );
    }

    UserEntity user = userRepository.findById(userId).orElseThrow();

    // Согласовано с проверкой isEnabled() в JwtAuthenticationFilter и с authenticate():
    // деактивированный аккаунт не должен получать новые рабочие токены через refresh (вплоть
    // до 30-дневного срока refresh) только потому, что его refresh-токен не инвалидировали.
    if (user.getUserStatus().getName() == UserSystemStatusNameEnum.INACTIVE) {
      throw new AuthException(
          ErrorCodeEnum.BAD_REQUEST,
          AuthErrorEnum.ACCOUNT_INACTIVE
      );
    }

    String newAccess = jwtProvider.generateAccess(user, sessionId, activeRole);
    String newRefresh = jwtProvider.generateRefresh(user, sessionId, activeRole);
    String newRefreshHash = sha256(newRefresh);

    session.setRefreshHash(newRefreshHash);
    session.setLastUsedAt(Instant.now());

    sessionStore.save(userId, session);

    return new TokenPair(newAccess, newRefresh);
  }

  public void invalidateSession(String accessToken) {
    Claims claims = jwtProvider.parse(accessToken);

    Long userId = Long.valueOf(claims.getSubject());
    String sessionId = claims.get("sid", String.class);

    sessionStore.deleteSession(userId, sessionId);
  }

  public void terminateSession(Long userId, String sessionId) {
    sessionStore.deleteSession(userId, sessionId);
  }

  public void terminateAllSessions(Long userId) {
    sessionStore.deleteAllSessions(userId);
  }

  @Transactional
  public TokenPair activateAccount(
      ActivateAccountRequest request,
      String userAgent,
      String activateCode
  ) {
    Long userId = sessionStore.findUserIdByActivationCode(activateCode)
        .orElseThrow(() -> new EntityNotFoundException("Activation token not found"));

    UserEntity user = userRepository.findById(userId)
        .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

    if (user.getUserStatus().getName() != UserSystemStatusNameEnum.PENDING_CONFIRMATION) {
      throw new AuthException(
          ErrorCodeEnum.BAD_REQUEST,
          AuthErrorEnum.ALREADY_ACTIVATED
      );
    }

    user.setUserStatus(userReferenceResolver.userSystemStatus(UserSystemStatusNameEnum.ACTIVE));

    userRepository.save(user);
    authUserCache.evict(userId);

    sessionStore.deleteActivationCodeByUserId(userId);

    return createSession(user, userAgent, request.getRole());
  }

  public void forgotPassword(ForgotPasswordRequest request) {
    String normalizedEmail = normalizeEmailOrThrow(request.email());
    userRepository.findByEmail(
        normalizedEmail
    ).ifPresent(user -> {

      String code = generateAndSaveCode(
          user.getId(),
          sessionStore::saveResetCode
      );
      eventPublisher.publishEvent(new ResetPasswordEmailEvent(normalizedEmail, code));
    });
  }

  @Transactional
  public void resetPassword(ResetPasswordRequest request, String resetCode) {
    Long userId = sessionStore.findUserIdByResetCode(resetCode)
        .orElseThrow(() -> new EntityNotFoundException("Reset code not found"));

    UserEntity user = userRepository.findById(userId)
        .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

    user.setPasswordHash(
        passwordEncoder.encode(request.newPassword())
    );

    userRepository.save(user);
    authUserCache.evict(userId);

    sessionStore.deleteResetCodeByUserId(userId);

    terminateAllSessions(userId);
  }

  public void verifyActivationToken(String code) {
    Long userId = sessionStore.findUserIdByActivationCode(code)
        .orElseThrow(() ->
            new AuthException(
                ErrorCodeEnum.BAD_REQUEST,
                AuthErrorEnum.TOKEN_EXPIRED
            )
        );

    UserEntity user = userRepository.findById(userId)
        .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

    if (user.getUserStatus().getName() != UserSystemStatusNameEnum.PENDING_CONFIRMATION) {
      throw new AuthException(
          ErrorCodeEnum.BAD_REQUEST,
          AuthErrorEnum.ALREADY_ACTIVATED
      );
    }
  }

  public void verifyResetToken(String code) {
    sessionStore.findUserIdByResetCode(code)
        .orElseThrow(() ->
            new AuthException(
                ErrorCodeEnum.BAD_REQUEST,
                AuthErrorEnum.TOKEN_EXPIRED
            )
        );
  }

  private TokenPair createSession(UserEntity user, String userAgent, String activeRole) {
    String sessionId = UUID.randomUUID().toString();

    String access = jwtProvider.generateAccess(user, sessionId, activeRole);
    String refresh = jwtProvider.generateRefresh(user, sessionId, activeRole);

    String refreshHash = sha256(refresh);

    sessionStore.save(
        user.getId(),
        AuthSession.builder()
            .setSessionId(sessionId)
            .setRefreshHash(refreshHash)
            .setCreatedAt(Instant.now())
            .setLastUsedAt(Instant.now())
            .setUserAgent(userAgent)
            .build()
    );

    return new TokenPair(access, refresh);
  }

  private String generateCode() {
    // Secure Random криптографически стойкий, по сравнению с Random и ThreadLocalRandom
    int number = SECURE_RANDOM.nextInt(100_000, 1_000_000);
    return String.format("%06d", number);
  }

  public String generateAndSaveCode(
      Long userId,
      BiPredicate<String, Long> saveAttempt) {
    for (int attempt = 0; attempt < 5; attempt++) {
      String code = generateCode();
      if (saveAttempt.test(code, userId)) {
        return code;
      }
    }
    // 5 попыток при 900 000 вариантов и 100 000 активных кодов:
    // вероятность 5 промахов ~~ (0.11)^5 ~~ 1.6e-5
    throw new IllegalStateException("Could not generate unique activation code");
  }


  private String normalizeEmailOrThrow(String rowEmail) {
    return EmailNormalizer.tryNormalize(rowEmail)
        .orElseThrow(() -> new IllegalStateException(
            "Email was validated but failed to normalize: " + rowEmail
        ));
  }

  private Optional<String> normalizeEmail(String rowEmail) {
    return EmailNormalizer.tryNormalize(rowEmail);
  }
}
