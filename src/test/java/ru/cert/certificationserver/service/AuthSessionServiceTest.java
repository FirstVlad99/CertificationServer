package ru.cert.certificationserver.service;

import io.jsonwebtoken.Claims;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.cert.certificationserver.dto.auth.event.ActivationEmailEvent;
import ru.cert.certificationserver.dto.auth.event.ResetPasswordEmailEvent;
import ru.cert.certificationserver.dto.auth.request.*;
import ru.cert.certificationserver.exception.AuthException;
import ru.cert.certificationserver.kafka.email.MailProducer;
import ru.cert.certificationserver.model.entity.UserEntity;
import ru.cert.certificationserver.model.entity.UserSystemStatusEntity;
import ru.cert.certificationserver.model.enums.AuthErrorEnum;
import ru.cert.certificationserver.model.enums.ErrorCodeEnum;
import ru.cert.certificationserver.model.enums.RoleNameEnum;
import ru.cert.certificationserver.model.enums.UserSystemStatusNameEnum;
import ru.cert.certificationserver.model.redis.AuthSession;
import ru.cert.certificationserver.model.redis.TokenPair;
import ru.cert.certificationserver.repository.UserRepository;
import ru.cert.certificationserver.repository.redis.JwtProvider;
import ru.cert.certificationserver.repository.redis.RedisSessionStore;
import ru.cert.certificationserver.util.HashUtils;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthSessionServiceTest {
  @Mock
  private JwtProvider jwtProvider;

  @Mock
  private RedisSessionStore sessionStore;

  @Mock
  private UserRepository userRepository;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Mock
  private MailProducer mailProducer;

  @Mock
  private AuthUserCache authUserCache;

  @Mock
  private UserReferenceResolver userReferenceResolver;

  @Mock
  private ApplicationEventPublisher applicationEventPublisher;

  @InjectMocks
  private AuthSessionService authService;

  private UserEntity user;

  @BeforeEach
  void setUp() {
    user = new UserEntity();
    user.setId(1L);
    user.setPasswordHash("encoded-password");
    user.setUserStatus(TestUserStatuses.active());
  }

  /*
  Пароль считается верным, если он содержит одну ЛАТИНСКУЮ букву, одну цифру, один спец. символ (длина - минимум 8)
   */
  @Test
  @DisplayName("register: должен создать пользователя при верном email и password")
  void register_shouldCreateUser() {
    String email = "test@test.com";
    RegisterRequest request = new RegisterRequest(email, "password");
    UserSystemStatusEntity pendingStatus = TestUserStatuses.pendingConfirmation();

    when(userRepository.existsByEmail(email))
        .thenReturn(false);
    when(passwordEncoder.encode("password"))
        .thenReturn("encoded-password");
    when(userReferenceResolver.userSystemStatus(UserSystemStatusNameEnum.PENDING_CONFIRMATION))
        .thenReturn(pendingStatus);
    when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> {
      UserEntity u = inv.getArgument(0);
      u.setId(1L);   // имитирую присвоение ID
      return u;
    });

    // нужно замокать, т.к. redisTemplate null
    when(sessionStore.saveIfAbsentActivationCode(anyString(), eq(1L))).thenReturn(true);

    authService.register(request);

    ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
    verify(userRepository).save(captor.capture());

    UserEntity saved = captor.getValue();
    assertEquals(email, saved.getEmail());
    assertEquals("encoded-password", saved.getPasswordHash());
    assertEquals(pendingStatus, saved.getUserStatus());

    verify(userRepository).flush();
    verify(applicationEventPublisher).publishEvent(
        argThat((Object e) -> ((ActivationEmailEvent) e).email().equals(email))
    );
    verify(sessionStore).saveIfAbsentActivationCode(any(), any());
  }

  @Test
  @DisplayName("register: должен создать пользователя при верном email с тэгом и password")
  void register_shouldCreateUserIfEmailContainsTag() {
    String email = "test+certification@test.com";
    String normalized = "test@test.com";
    RegisterRequest request = new RegisterRequest(email, "password");
    UserSystemStatusEntity pendingStatus = TestUserStatuses.pendingConfirmation();

    // после нормализации из email убрался tag
    when(userRepository.existsByEmail(normalized))
        .thenReturn(false);
    when(passwordEncoder.encode("password"))
        .thenReturn("encoded-password");
    when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> {
      UserEntity u = inv.getArgument(0);
      u.setId(1L);   // имитирую присвоение ID
      return u;
    });
    when(userReferenceResolver.userSystemStatus(UserSystemStatusNameEnum.PENDING_CONFIRMATION))
        .thenReturn(pendingStatus);

    // нужно замокать, т.к. redisTemplate null
    when(sessionStore.saveIfAbsentActivationCode(anyString(), eq(1L))).thenReturn(true);

    authService.register(request);

    ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
    verify(userRepository).save(captor.capture());

    UserEntity saved = captor.getValue();
    assertEquals(normalized, saved.getEmail());
    assertEquals("encoded-password", saved.getPasswordHash());
    assertEquals(pendingStatus, saved.getUserStatus());

    verify(userRepository).flush();
    verify(applicationEventPublisher).publishEvent(
        argThat((Object e) -> ((ActivationEmailEvent) e).email().equals(normalized))
    );
    verify(sessionStore).saveIfAbsentActivationCode(any(), any());

  }

  @Test
  @DisplayName("register: должен выбрасывать ошибку, если email уже есть в системе")
  void register_shouldCreateUserIfEmailExists() {
    RegisterRequest request = new RegisterRequest("test+certification@test.com", "password");

    // почта нормализована была
    when(userRepository.existsByEmail("test@test.com"))
        .thenReturn(true);

    AuthException thrown = assertThrows(AuthException.class,
        () -> authService.register(request));

    assertEquals(AuthErrorEnum.EMAIL_ALREADY_EXISTS, thrown.getAuthError());
    assertEquals(ErrorCodeEnum.CONFLICT, thrown.getErrorCode());
  }

  @Test
  @DisplayName("resendActivationCode: не должен ничего делать, если пользователь не найден")
  void resendActivationCode_shouldDoNothingIfUserNotFound() {
    ResendActivationRequest request = new ResendActivationRequest("test@test.com");

    when(userRepository.findByEmail("test@test.com"))
        .thenReturn(Optional.empty());

    authService.resendActivationCode(request);

    verify(sessionStore, never()).saveIfAbsentActivationCode(any(), any());
    verify(applicationEventPublisher, never()).publishEvent(any());
  }

  @Test
  @DisplayName("resendActivationCode: yt должен слать письмо, если статус НЕ PENDING_CONFIRMATION")
  void resendActivationCode_shouldNotSendIfStatusPendingConfirmation() {
    ResendActivationRequest request = new ResendActivationRequest("test@test.com");
    user.setUserStatus(TestUserStatuses.active());

    when(userRepository.findByEmail("test@test.com"))
        .thenReturn(Optional.of(user));

    authService.resendActivationCode(request);

    verify(sessionStore, never()).saveIfAbsentActivationCode(any(), any());
    verify(applicationEventPublisher, never()).publishEvent(any());
  }

  @Test
  @DisplayName("resendActivationCode: должен сгенерировать код и отправить письмо, если кода нет")
  void resendActivationCode_shouldGenerateCodeAndPublishEventIfCodeAbsent() {
    String email = "test@test.com";
    ResendActivationRequest request = new ResendActivationRequest(email);
    user.setUserStatus(TestUserStatuses.pendingConfirmation());
    when(userRepository.findByEmail(email))
        .thenReturn(Optional.of(user));
    when(sessionStore.findActivationCodeByUserId(1L))
        .thenReturn(Optional.empty());

    // нужно замокать, т.к. redisTemplate null
    when(sessionStore.saveIfAbsentActivationCode(anyString(), eq(1L))).thenReturn(true);

    authService.resendActivationCode(request);

    verify(sessionStore).saveIfAbsentActivationCode(any(), eq(1L));
    verify(applicationEventPublisher).publishEvent(
        argThat((Object e) -> e instanceof ActivationEmailEvent(String email1, String code)
            && email1.equals(email)
            && code != null)
    );
  }

  @Test
  @DisplayName("resendActivationCode: должен использовать существующий код, если он уже есть")
  void resendActivationCode_shouldReuseExistingCode() {
    String email = "test@test.com";
    String existingCode = "existing-code";
    ResendActivationRequest request = new ResendActivationRequest(email);
    user.setUserStatus(TestUserStatuses.pendingConfirmation());

    when(userRepository.findByEmail(email))
        .thenReturn(Optional.of(user));
    when(sessionStore.findActivationCodeByUserId(1L))
        .thenReturn(Optional.of(existingCode));

    authService.resendActivationCode(request);

    // новый код НЕ генерируется и НЕ сохраняется
    verify(sessionStore, never()).saveIfAbsentActivationCode(any(), any());
    verify(applicationEventPublisher).publishEvent(
        argThat((Object e) -> e instanceof ActivationEmailEvent(String email1, String code)
            && email1.equals(email)
            && code.equals(existingCode))
    );
  }

  @Test
  @DisplayName("resendActivationCode: должен нормализовать email с тэгом перед поиском")
  void resendActivationCode_shouldNormalizeEmailWithTag() {
    String rawEmail = "test+certification@test.com";
    String normalized = "test@test.com";
    ResendActivationRequest request = new ResendActivationRequest(rawEmail);
    user.setUserStatus(TestUserStatuses.pendingConfirmation());

    when(userRepository.findByEmail(normalized))
        .thenReturn(Optional.of(user));
    when(sessionStore.findActivationCodeByUserId(1L))
        .thenReturn(Optional.of("code"));

    authService.resendActivationCode(request);

    verify(userRepository).findByEmail(normalized);
    verify(applicationEventPublisher).publishEvent(
        argThat((Object e) -> e instanceof ActivationEmailEvent ev
            && ev.email().equals(normalized))
    );
  }

  @Test
  @DisplayName("authenticate: должен вернуть token pair при успешной аутентификации")
  void authenticate_shouldReturnTokenPair() {
    LoginRequest request = new LoginRequest("test@test.com", "password",RoleNameEnum.CUSTOMER.name());

    when(userRepository.findByEmail(any()))
        .thenReturn(Optional.of(user));
    when(passwordEncoder.matches("password", "encoded-password"))
        .thenReturn(true);
    when(jwtProvider.generateAccess(eq(user), anyString(),eq(RoleNameEnum.CUSTOMER.name())))
        .thenReturn("access-token");
    when(jwtProvider.generateRefresh(eq(user), anyString(),eq(RoleNameEnum.CUSTOMER.name())))
        .thenReturn("refresh-token");

    TokenPair result = authService.authenticate(request, "Chrome");

    assertNotNull(result);

    verify(sessionStore).save(eq(user.getId()), any(AuthSession.class));
  }

  @Test
  @DisplayName("authenticate: неизвестная почта неотличима от неверного пароля (иначе почты перебираются злоумышленником)")
  void authenticate_shouldThrowIfUserNotFound() {
    LoginRequest request = new LoginRequest("test@test.com", "password", RoleNameEnum.CUSTOMER.name());

    when(userRepository.findByEmail(any()))
        .thenReturn(Optional.empty());

    AuthException thrown = assertThrows(AuthException.class,
        () -> authService.authenticate(request, "Chrome"));

    assertEquals(AuthErrorEnum.BAD_CREDENTIALS, thrown.getAuthError());
    assertEquals(ErrorCodeEnum.BAD_REQUEST, thrown.getErrorCode());
  }

  @Test
  @DisplayName("authenticate: должен выбросить ошибку если аккаунт не активирован")
  void authenticate_shouldThrowIfAccountNotActivated() {
    user.setPasswordHash("encoded-password");
    UserSystemStatusEntity confirmationStatus = TestUserStatuses.pendingConfirmation();
    user.setUserStatus(confirmationStatus);

    LoginRequest request = new LoginRequest("test@test.com", "password",RoleNameEnum.CUSTOMER.name());

    when(userRepository.findByEmail(any()))
        .thenReturn(Optional.of(user));
    when(passwordEncoder.matches("password", "encoded-password"))
        .thenReturn(true);

    AuthException thrown = assertThrows(AuthException.class,
        () -> authService.authenticate(request, "Chrome"));

    assertEquals(AuthErrorEnum.ACCOUNT_NOT_ACTIVATED, thrown.getAuthError());
    assertEquals(ErrorCodeEnum.BAD_REQUEST, thrown.getErrorCode());
  }

  @Test
  @DisplayName("authenticate: должен выбросить ошибку при неверном пароле")
  void authenticate_shouldThrowIfBadCredentials() {
    LoginRequest request = new LoginRequest("test@test.com", "wrong",RoleNameEnum.CUSTOMER.name());

    when(userRepository.findByEmail(any()))
        .thenReturn(Optional.of(user));
    when(passwordEncoder.matches(any(), any()))
        .thenReturn(false);

    AuthException thrown = assertThrows(AuthException.class,
        () -> authService.authenticate(request, "Chrome"));

    assertEquals(AuthErrorEnum.BAD_CREDENTIALS, thrown.getAuthError());
    assertEquals(ErrorCodeEnum.BAD_REQUEST, thrown.getErrorCode());
  }

  @Test
  @DisplayName("authenticate: должен выбросить ошибку если аккаунт деактивирован")
  void authenticate_shouldThrowIfAccountInactive() {
    UserSystemStatusEntity inactiveStatus = TestUserStatuses.inactive();
    user.setUserStatus(inactiveStatus);

    LoginRequest request = new LoginRequest("test@test.com", "password",RoleNameEnum.CUSTOMER.name());

    when(userRepository.findByEmail(any()))
        .thenReturn(Optional.of(user));
    when(passwordEncoder.matches("password", "encoded-password"))
        .thenReturn(true);

    AuthException thrown = assertThrows(AuthException.class,
        () -> authService.authenticate(request, "Chrome"));

    assertEquals(AuthErrorEnum.ACCOUNT_INACTIVE, thrown.getAuthError());
    assertEquals(ErrorCodeEnum.BAD_REQUEST, thrown.getErrorCode());
  }

  @Test
  @DisplayName("authenticate: неверный пароль важнее INACTIVE - не говорю статус чужого аккаунта тому, кто не доказал знание пароля")
  void authenticate_shouldThrowBadCredentialsBeforeCheckingInactiveStatus() {
    UserSystemStatusEntity inactiveStatus = TestUserStatuses.inactive();
    user.setUserStatus(inactiveStatus);

    LoginRequest request = new LoginRequest("test@test.com", "wrong", RoleNameEnum.CUSTOMER.name());

    when(userRepository.findByEmail(any()))
        .thenReturn(Optional.of(user));
    when(passwordEncoder.matches("wrong", "encoded-password"))
        .thenReturn(false);

    AuthException thrown = assertThrows(AuthException.class,
        () -> authService.authenticate(request, "Chrome"));

    assertEquals(AuthErrorEnum.BAD_CREDENTIALS, thrown.getAuthError());
    assertEquals(ErrorCodeEnum.BAD_REQUEST, thrown.getErrorCode());
  }

  @Test
  @DisplayName("refreshSession: должен обновить access и refresh токены")
  void refreshSession_shouldRefreshTokens() {
    Claims claims = mock(Claims.class);

    AuthSession session = AuthSession.builder()
        .setSessionId("sid")
        .setRefreshHash("hash")
        .setActiveRole(RoleNameEnum.CUSTOMER.name())
        .build();

    when(jwtProvider.parse("refresh")).thenReturn(claims);
    when(claims.getSubject()).thenReturn("1");
    when(claims.get("sid", String.class)).thenReturn("sid");
    when(claims.get("role", String.class)).thenReturn(RoleNameEnum.CUSTOMER.name());

    when(sessionStore.find(1L, "sid"))
        .thenReturn(Optional.of(session));

    when(userRepository.findById(1L))
        .thenReturn(Optional.of(user));

    when(jwtProvider.generateAccess(user, "sid", RoleNameEnum.CUSTOMER.name()))
        .thenReturn("access");

    when(jwtProvider.generateRefresh(user, "sid", RoleNameEnum.CUSTOMER.name()))
        .thenReturn("refresh-new");

    session.setRefreshHash(
        HashUtils.sha256("refresh")
    );

    TokenPair result = authService.refreshSession("refresh");

    assertNotNull(result);

    verify(sessionStore).save(eq(1L), any(AuthSession.class));
  }

  @Test
  @DisplayName("refreshSession: должен выбросить ошибку если сессия не найдена")
  void refreshSession_shouldThrowIfSessionNotFound() {
    Claims claims = mock(Claims.class);

    when(jwtProvider.parse("refresh")).thenReturn(claims);
    when(claims.getSubject()).thenReturn("1");
    when(claims.get("sid", String.class)).thenReturn("sid");

    when(sessionStore.find(1L, "sid"))
        .thenReturn(Optional.empty());

    assertThrows(EntityNotFoundException.class,
        () -> authService.refreshSession("refresh"));
  }

  @Test
  @DisplayName("refreshSession: должен выбрасывать INVALID_REFRESH_TOKEN, если хеш не совпадает")
  void refreshSession_shouldThrowIfRefreshHashMismatch() {
    String refreshToken = "some-refresh-token";
    Long userId = 1L;
    String sessionId = "session-1";
    String activeRole = RoleNameEnum.CUSTOMER.name();

    Claims claims = mock(Claims.class);
    when(claims.getSubject()).thenReturn(String.valueOf(userId));
    when(claims.get("sid", String.class)).thenReturn(sessionId);
    when(claims.get("role", String.class)).thenReturn(activeRole);

    when(jwtProvider.parse(refreshToken)).thenReturn(claims);

    AuthSession session = AuthSession.builder()
        .setSessionId("sid")
        .setRefreshHash("hash")
        .setActiveRole(RoleNameEnum.CUSTOMER.name())
        .build();
    session.setRefreshHash("completely-different-hash");

    when(sessionStore.find(userId, sessionId)).thenReturn(Optional.of(session));

    AuthException thrown = assertThrows(AuthException.class,
        () -> authService.refreshSession(refreshToken));

    assertEquals(AuthErrorEnum.INVALID_REFRESH_TOKEN, thrown.getAuthError());
    assertEquals(ErrorCodeEnum.BAD_REQUEST, thrown.getErrorCode());

    // до userRepository дело не дошло
    verify(userRepository, never()).findById(any());
  }

  @Test
  @DisplayName("refreshSession: должен выбрасывать ACCOUNT_INACTIVE, если аккаунт деактивирован")
  void refreshSession_shouldThrowIfAccountInactive() {
    String refreshToken = "some-refresh-token";
    Long userId = user.getId();
    String sessionId = "session-1";
    String activeRole = RoleNameEnum.CUSTOMER.name();

    Claims claims = mock(Claims.class);
    when(claims.getSubject()).thenReturn(String.valueOf(userId));
    when(claims.get("sid", String.class)).thenReturn(sessionId);
    when(claims.get("role", String.class)).thenReturn(activeRole);

    when(jwtProvider.parse(refreshToken)).thenReturn(claims);

    AuthSession session = AuthSession.builder()
        .setSessionId("sid")
        .setRefreshHash("hash")
        .setActiveRole(RoleNameEnum.CUSTOMER.name())
        .build();
    session.setRefreshHash(HashUtils.sha256(refreshToken));
    when(sessionStore.find(userId, sessionId)).thenReturn(Optional.of(session));

    user.setUserStatus(TestUserStatuses.inactive());

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    AuthException thrown = assertThrows(AuthException.class,
        () -> authService.refreshSession(refreshToken));

    assertEquals(AuthErrorEnum.ACCOUNT_INACTIVE, thrown.getAuthError());
    assertEquals(ErrorCodeEnum.BAD_REQUEST, thrown.getErrorCode());

    // новые токены не генерировались, сессия не сохранялась
    verify(jwtProvider, never()).generateAccess(any(), any(), any());
    verify(jwtProvider, never()).generateRefresh(any(), any(), any());
    verify(sessionStore, never()).save(any(), any());
  }

  @Test
  @DisplayName("invalidateSession: должен удалить сессию")
  void invalidateSession_shouldDeleteSession() {
    Claims claims = mock(Claims.class);

    when(jwtProvider.parse("access")).thenReturn(claims);
    when(claims.getSubject()).thenReturn("1");
    when(claims.get("sid", String.class)).thenReturn("sid");

    authService.invalidateSession("access");

    verify(sessionStore).deleteSession(1L, "sid");
  }

  @Test
  @DisplayName("terminateSession: должен удалить сессию по userId и sessionId")
  void terminateSession_shouldDeleteSession() {
    authService.terminateSession(1L, "sid");

    verify(sessionStore).deleteSession(1L, "sid");
  }

  @Test
  @DisplayName("terminateAllSessions: должен удалить все сессии пользователя")
  void terminateAllSessions_shouldDeleteAllSessions() {
    authService.terminateAllSessions(1L);

    verify(sessionStore).deleteAllSessions(1L);
  }

  @Test
  @DisplayName("activateAccount: должен активировать аккаунт")
  void activateAccount_shouldActivateUser() {
    ActivateAccountRequest request = new ActivateAccountRequest("new-pass");

    user.setUserStatus(TestUserStatuses.pendingConfirmation());
    UserSystemStatusEntity activeStatus = TestUserStatuses.active();
    when(sessionStore.findUserIdByActivationCode("code"))
        .thenReturn(Optional.of(1L));
    when(userRepository.findById(1L))
        .thenReturn(Optional.of(user));
    when(jwtProvider.generateAccess(eq(user), anyString(),eq(RoleNameEnum.CUSTOMER.name())))
        .thenReturn("access-token");
    when(jwtProvider.generateRefresh(eq(user), anyString(), eq(RoleNameEnum.CUSTOMER.name())))
        .thenReturn("refresh-token");
    when(userReferenceResolver.userSystemStatus(UserSystemStatusNameEnum.ACTIVE))
        .thenReturn(activeStatus);
    TokenPair result =
        authService.activateAccount(request, "Chrome", "code");

    assertNotNull(result);
    assertEquals(activeStatus, user.getUserStatus());

    verify(sessionStore).deleteActivationCodeByUserId(1L);
  }

  @Test
  @DisplayName("activateAccount: должен выбросить ошибку если аккаунт уже активирован")
  void activateAccount_shouldThrowIfAlreadyActivated() {
    when(sessionStore.findUserIdByActivationCode("token"))
        .thenReturn(Optional.of(1L));
    when(userRepository.findById(1L))
        .thenReturn(Optional.of(user));

    AuthException thrown = assertThrows(AuthException.class,
        () -> authService.activateAccount(
            new ActivateAccountRequest("123"),
            "Chrome",
            "token"
        ));

    assertEquals(AuthErrorEnum.ALREADY_ACTIVATED, thrown.getAuthError());
    assertEquals(ErrorCodeEnum.BAD_REQUEST, thrown.getErrorCode());
  }

  @Test
  @DisplayName("activateAccount: должен выбросить ошибку если пользователь не найден по токену")
  void activateAccount_shouldThrowIfUserNotFound() {
    when(sessionStore.findUserIdByActivationCode("token"))
        .thenReturn(Optional.of(1L));
    when(userRepository.findById(1L))
        .thenReturn(Optional.empty());

    assertThrows(EntityNotFoundException.class,
        () -> authService.activateAccount(
            new ActivateAccountRequest("new-pass"),
            "Chrome",
            "token"
        ));
  }

  @Test
  @DisplayName("forgotPassword: должен отправить письмо если пользователь найден")
  void forgotPassword_shouldSendEmail() {
    ForgotPasswordRequest request =
        new ForgotPasswordRequest("test@test.com");

    when(userRepository.findByEmail(any()))
        .thenReturn(Optional.of(user));

    // нужно замокать, т.к. redisTemplate null
    when(sessionStore.saveResetCode(anyString(), eq(1L))).thenReturn(true);

    authService.forgotPassword(request);

    verify(sessionStore).saveResetCode(anyString(), eq(1L));
    verify(applicationEventPublisher).publishEvent(
        argThat((Object e) -> e instanceof ResetPasswordEmailEvent ev
            && ev.email().equals("test@test.com"))
    );
  }

  @Test
  @DisplayName("forgotPassword: ничего не делает если пользователь не найден")
  void forgotPassword_shouldDoNothing() {
    ForgotPasswordRequest request =
        new ForgotPasswordRequest("test@test.com");

    when(userRepository.findByEmail(any()))
        .thenReturn(Optional.empty());

    authService.forgotPassword(request);

  }

  @Test
  @DisplayName("resetPassword: должен обновить пароль и удалить все сессии")
  void resetPassword_shouldResetPassword() {
    ResetPasswordRequest request =
        new ResetPasswordRequest("code","new-password");

    when(sessionStore.findUserIdByResetCode("code"))
        .thenReturn(Optional.of(1L));

    when(userRepository.findById(1L))
        .thenReturn(Optional.of(user));

    when(passwordEncoder.encode("new-password"))
        .thenReturn("encoded");

    authService.resetPassword(request, "code");

    verify(sessionStore).deleteResetCodeByUserId(1L);
    verify(sessionStore).deleteAllSessions(1L);
  }

  @Test
  @DisplayName("resetPassword: должен выбросить ошибку если reset token не найден")
  void resetPassword_shouldThrowIfTokenNotFound() {
    when(sessionStore.findUserIdByResetCode("code"))
        .thenReturn(Optional.empty());

    assertThrows(EntityNotFoundException.class,
        () -> authService.resetPassword(
            new ResetPasswordRequest("123","123"),
            "code"
        ));
  }

  @Test
  @DisplayName("resetPassword: должен выбросить ошибку если пользователь не найден по токену")
  void resetPassword_shouldThrowIfUserNotFound() {
    when(sessionStore.findUserIdByResetCode("code"))
        .thenReturn(Optional.of(1L));
    when(userRepository.findById(1L))
        .thenReturn(Optional.empty());

    assertThrows(EntityNotFoundException.class,
        () -> authService.resetPassword(
            new ResetPasswordRequest("112365","new-password"),
            "code"
        ));
  }

  @Test
  @DisplayName("verifyActivationToken: должен пройти валидацию")
  void verifyActivationToken_shouldPass() {
    user.setUserStatus(TestUserStatuses.pendingConfirmation());

    when(sessionStore.findUserIdByActivationCode("code"))
        .thenReturn(Optional.of(1L));

    when(userRepository.findById(1L))
        .thenReturn(Optional.of(user));

    assertDoesNotThrow(
        () -> authService.verifyActivationToken("code")
    );
  }

  @Test
  @DisplayName("verifyActivationToken: должен выбросить ошибку если код истек")
  void verifyActivationToken_shouldThrowIfExpired() {
    when(sessionStore.findUserIdByActivationCode("code"))
        .thenReturn(Optional.empty());

    assertThrows(AuthException.class,
        () -> authService.verifyActivationToken("code"));
  }

  @Test
  @DisplayName("verifyActivationToken: должен выбросить ошибку если пользователь не найден по коду")
  void verifyActivationToken_shouldThrowIfUserNotFound() {
    when(sessionStore.findUserIdByActivationCode("code"))
        .thenReturn(Optional.of(1L));
    when(userRepository.findById(1L))
        .thenReturn(Optional.empty());

    assertThrows(EntityNotFoundException.class,
        () -> authService.verifyActivationToken("code"));
  }

  @Test
  @DisplayName("verifyResetToken: должен пройти валидацию")
  void verifyResetToken_shouldPass() {
    when(sessionStore.findUserIdByResetCode("code"))
        .thenReturn(Optional.of(1L));

    assertDoesNotThrow(
        () -> authService.verifyResetToken("code")
    );
  }

  @Test
  @DisplayName("verifyResetToken: должен выбросить ошибку если код истек")
  void verifyResetToken_shouldThrowIfExpired() {
    when(sessionStore.findUserIdByResetCode("code"))
        .thenReturn(Optional.empty());

    assertThrows(AuthException.class,
        () -> authService.verifyResetToken("code"));
  }

  @Test
  @DisplayName("generateAndSaveCode: должен вернуть код при успешном сохранении с первой попытки")
  void generateAndSaveCode_shouldReturnCodeOnFirstAttempt() {
    BiPredicate<String, Long> saveAttempt = (code, userId) -> true;

    String code = authService.generateAndSaveCode(1L, saveAttempt);

    assertNotNull(code);
    assertEquals(6, code.length());
    assertTrue(code.matches("\\d{6}"));
  }

  @Test
  @DisplayName("generateAndSaveCode: должен вернуть код при успехе со второй попытки")
  void generateAndSaveCode_shouldRetryUntilSuccess() {
    AtomicInteger attempts = new AtomicInteger(0);
    BiPredicate<String, Long> saveAttempt = (code, userId) -> attempts.incrementAndGet() >= 2;

    String code = authService.generateAndSaveCode(1L, saveAttempt);

    assertNotNull(code);
    assertEquals(2, attempts.get());
  }

  @Test
  @DisplayName("generateAndSaveCode: должен бросить исключение после 5 неудачных попыток")
  void generateAndSaveCode_shouldThrowAfterFiveFailedAttempts() {
    AtomicInteger attempts = new AtomicInteger(0);
    BiPredicate<String, Long> saveAttempt = (code, userId) -> {
      attempts.incrementAndGet();
      return false;
    };

    IllegalStateException thrown = assertThrows(IllegalStateException.class,
        () -> authService.generateAndSaveCode(1L, saveAttempt));

    assertEquals("Could not generate unique activation code", thrown.getMessage());
    assertEquals(5, attempts.get());  // ровно 5 попыток
  }

  @Test
  @DisplayName("generateAndSaveCode: должен передавать userId в saveAttempt")
  void generateAndSaveCode_shouldPassUserIdToSaveAttempt() {
    AtomicReference<Long> capturedUserId = new AtomicReference<>();
    BiPredicate<String, Long> saveAttempt = (code, userId) -> {
      capturedUserId.set(userId);
      return true;
    };

    authService.generateAndSaveCode(42L, saveAttempt);

    assertEquals(42L, capturedUserId.get());
  }

  public static final class TestUserStatuses {
    public static UserSystemStatusEntity pendingConfirmation() {
      return new UserSystemStatusEntity(1, UserSystemStatusNameEnum.PENDING_CONFIRMATION);
    }
    public static UserSystemStatusEntity active() {
      return new UserSystemStatusEntity(2, UserSystemStatusNameEnum.ACTIVE);
    }
    public static UserSystemStatusEntity inactive() {
      return new UserSystemStatusEntity(3, UserSystemStatusNameEnum.INACTIVE);
    }

    private TestUserStatuses() {}
  }
}
