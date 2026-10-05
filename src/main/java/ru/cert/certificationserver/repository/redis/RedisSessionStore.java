package ru.cert.certificationserver.repository.redis;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;
import ru.cert.certificationserver.model.redis.AuthSession;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

@Repository
public class RedisSessionStore {
  private final RedisTemplate<String, Object> redisTemplate;
  private final RedisTemplate<String, String> stringRedisTemplate;

  private static final Duration TTL = Duration.ofDays(30);

  public RedisSessionStore(RedisTemplate<String, Object> redisTemplate, RedisTemplate<String, String> stringRedisTemplate) {
    this.redisTemplate = redisTemplate;
    this.stringRedisTemplate = stringRedisTemplate;
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
        TTL
    );

    stringRedisTemplate.opsForSet().add(
        userSessionsKey(userId),
        session.getSessionId()
    );
  }

  public void saveActivationToken(String token, Long userId) {
    stringRedisTemplate.opsForValue()
        .set(
            "activation:" + token,
            userId.toString(),
            Duration.ofDays(7)
        );
  }

  public void saveResetToken(String token, Long userId) {
    stringRedisTemplate.opsForValue()
        .set(
            "reset:" + token,
            userId.toString(),
            Duration.ofHours(24)
        );
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

  public void delete(Long userId, String sessionId) {
    redisTemplate.delete(sessionKey(userId, sessionId));
    stringRedisTemplate.opsForSet()
        .remove(userSessionsKey(userId), sessionId);
  }

  public void deleteAll(Long userId) {
    Set<String> sessions = getUserSessions(userId);

    if (sessions != null) {
      for (String sid : sessions) {
        redisTemplate.delete(sessionKey(userId, sid));
      }
    }

    stringRedisTemplate.delete(userSessionsKey(userId));
  }

  public Optional<Long> findActivationToken(String token) {
    String value = stringRedisTemplate.opsForValue()
        .get("activation:" + token);

    return Optional.ofNullable(value)
        .map(Long::valueOf);
  }

  public void deleteActivationToken(String token) {
    stringRedisTemplate.delete("activation:" + token);
  }

  public Optional<Long> findResetToken(String token) {
    String value = stringRedisTemplate.opsForValue()
        .get("reset:" + token);

    return Optional.ofNullable(value)
        .map(Long::valueOf);
  }

  public void deleteResetToken(String token) {
    stringRedisTemplate.delete("reset:" + token);
  }
}
