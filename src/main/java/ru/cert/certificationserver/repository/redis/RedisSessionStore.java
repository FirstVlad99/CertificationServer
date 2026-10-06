package ru.cert.certificationserver.repository.redis;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;
import ru.cert.certificationserver.config.properties.TtlProperties;
import ru.cert.certificationserver.model.redis.AuthSession;

import java.util.Optional;
import java.util.Set;

@Repository
public class RedisSessionStore {
  private final RedisTemplate<String, Object> redisTemplate;
  private final RedisTemplate<String, String> stringRedisTemplate;
  // activation:code:{code} -> userId для поиска userId по code
  // activation:user:{userId} -> code для инвалидации старого кода
  private final static String ACTIVATION_CODE_KEY = "activation:code:";
  private final static String ACTIVATION_USER_KEY = "activation:user:";
  private final static String RESET_PASSWORD_CODE_KEY = "reset:code:";
  private final static String RESET_PASSWORD_USER_KEY = "reset:user:";

  private final TtlProperties ttlProperties;

  public RedisSessionStore(RedisTemplate<String, Object> redisTemplate, RedisTemplate<String, String> stringRedisTemplate, TtlProperties ttlProperties) {
    this.redisTemplate = redisTemplate;
    this.stringRedisTemplate = stringRedisTemplate;
    this.ttlProperties = ttlProperties;
  }

  private String sessionKey(Long userId, String sessionId) {
    return "auth:refresh:" + userId + ":" + sessionId;
  }

  private String userSessionsKey(Long userId) {
    return "auth:user:sessions:" + userId;
  }

  public void save(Long userId, AuthSession session) {
    redisTemplate.opsForValue().set(
        sessionKey(userId, session.getSessionId()),
        session,
        ttlProperties.session()
    );

    stringRedisTemplate.opsForSet().add(
        userSessionsKey(userId),
        session.getSessionId()
    );
  }

  public boolean saveIfAbsentActivationCode(String code, Long userId) {
    String codeKey = ACTIVATION_CODE_KEY + code;
    String userKey = ACTIVATION_USER_KEY + userId;

    Boolean saved = stringRedisTemplate.opsForValue()
        .setIfAbsent(codeKey, userId.toString(), ttlProperties.activation());

    if (!Boolean.TRUE.equals(saved)) {
      return false;
    }

    stringRedisTemplate.opsForValue()
        .set(userKey, code, ttlProperties.activation());
    return true;
  }

  public boolean saveResetCode(String code, Long userId) {
    String codeKey = RESET_PASSWORD_CODE_KEY + code;
    String userKey = RESET_PASSWORD_USER_KEY + userId;

    // Удаляю старый код, если есть
    String oldCode = stringRedisTemplate.opsForValue().get(userKey);
    if (oldCode != null) {
      stringRedisTemplate.delete(RESET_PASSWORD_CODE_KEY + oldCode);
    }

    // Cохраняю новый прямой ключ
    Boolean saved = stringRedisTemplate.opsForValue()
        .setIfAbsent(codeKey, userId.toString(), ttlProperties.passwordReset());

    if (!Boolean.TRUE.equals(saved)) {
      return false;
    }

    // Обновляю обратный индекс
    stringRedisTemplate.opsForValue()
        .set(userKey, code, ttlProperties.passwordReset());
    return true;
  }

  public Optional<AuthSession> find(Long userId, String sessionId) {
    return Optional.ofNullable(
        (AuthSession) redisTemplate.opsForValue().get(sessionKey(userId, sessionId))
    );
  }

  public boolean exists(Long userId, String sessionId) {
    return Boolean.TRUE.equals(redisTemplate.hasKey(sessionKey(userId, sessionId)));
  }

  public Set<String> getUserSessions(Long userId) {
    return stringRedisTemplate.opsForSet()
        .members(userSessionsKey(userId));
  }

  public void deleteSession(Long userId, String sessionId) {
    redisTemplate.delete(sessionKey(userId, sessionId));
    stringRedisTemplate.opsForSet()
        .remove(userSessionsKey(userId), sessionId);
  }

  public void deleteAllSessions(Long userId) {
    Set<String> sessions = getUserSessions(userId);

    if (sessions != null) {
      for (String sid : sessions) {
        redisTemplate.delete(sessionKey(userId, sid));
      }
    }

    stringRedisTemplate.delete(userSessionsKey(userId));
  }

  public Optional<String> findActivationCodeByUserId(Long userId) {
    String code = stringRedisTemplate.opsForValue()
        .get(ACTIVATION_USER_KEY + userId);

    return Optional.ofNullable(code);
  }

  public Optional<Long> findUserIdByActivationCode(String code) {
    String value = stringRedisTemplate.opsForValue()
        .get(ACTIVATION_CODE_KEY + code);

    return Optional.ofNullable(value)
        .map(Long::valueOf);
  }

  public void deleteActivationCodeByUserId(Long userId) {
    String userKey = ACTIVATION_USER_KEY + userId;
    String code = stringRedisTemplate.opsForValue().get(userKey);

    stringRedisTemplate.delete(userKey);

    if (code != null) {
      stringRedisTemplate.delete(ACTIVATION_CODE_KEY + code);
    }
  }

  public Optional<Long> findUserIdByResetCode(String code) {
    String value = stringRedisTemplate.opsForValue()
        .get(RESET_PASSWORD_CODE_KEY + code);

    return Optional.ofNullable(value)
        .map(Long::valueOf);
  }

  public Optional<String> findResetCodeByUserId(Long userId) {
    String code = stringRedisTemplate.opsForValue()
        .get(RESET_PASSWORD_USER_KEY + userId);

    return Optional.ofNullable(code);
  }

  public void deleteResetCodeByUserId(Long userId) {
    String userKey = RESET_PASSWORD_USER_KEY + userId;
    String code = stringRedisTemplate.opsForValue().get(userKey);

    stringRedisTemplate.delete(userKey);

    if (code != null) {
      stringRedisTemplate.delete(RESET_PASSWORD_CODE_KEY + code);
    }
  }
}
