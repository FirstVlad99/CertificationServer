package ru.cert.certificationserver.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;
import ru.cert.certificationserver.model.entity.UserEntity;
import ru.cert.certificationserver.model.enums.UserSystemStatusNameEnum;
import ru.cert.certificationserver.model.security.CachedAuthUser;
import ru.cert.certificationserver.repository.UserRepository;

import java.time.Duration;
import java.util.*;

// Кэш auth-снимка пользователя (Caffeine, in-memory): JwtAuthenticationFilter грузил
// findByIdWithRolesAndStreamId на КАЖДЫЙ запрос - теперь только на промах/по TTL. Один бэкенд-инстанс
// (см. docker-compose), поэтому локального кэша достаточно - Redis остаётся строго под сессии.
// TTL короткий (10с) как страховка; актуальность держим write-through eviction'ом на всех путях,
// меняющих поля принципала (роли, super_admin, статус, corporate_email, пароль, поток).
@Component
public class AuthUserCache {
  private static final Duration TTL = Duration.ofSeconds(10);
  private static final long MAX_SIZE = 50_000;

  private final UserRepository userRepository;
  private final Cache<Long, CachedAuthUser> cache;

  public AuthUserCache(UserRepository userRepository) {
    this.userRepository = userRepository;
    this.cache = Caffeine.newBuilder()
        .expireAfterWrite(TTL)
        .maximumSize(MAX_SIZE)
        .build();
  }

  // Промах - грузим из БД и кладём. Отсутствие пользователя НЕ кэшируем (empty), чтобы только что
  // созданный аккаунт не завис как «нет такого».
  public Optional<CachedAuthUser> load(Long userId) {
    CachedAuthUser cached = cache.getIfPresent(userId);
    if (cached != null) {
      return Optional.of(cached);
    }
    Optional<CachedAuthUser> loaded = fetch(userId);
    loaded.ifPresent(snapshot -> cache.put(userId, snapshot));
    return loaded;
  }

  public void evict(Long userId) {
    cache.invalidate(userId);
  }

  private Optional<CachedAuthUser> fetch(Long userId) {
    // Роли приходят fetch-join'ом в этом же запросе, поэтому маппим без открытой транзакции.
    Optional<UserEntity> userOptional = userRepository.findById(userId);
    if (userOptional.isEmpty())
      return Optional.empty();

    UserEntity user = userOptional.get();
    return Optional.of(new CachedAuthUser(
        user.getId(),
        user.getEmail(),
        user.getPasswordHash(),
        user.getUserStatus().getName() != UserSystemStatusNameEnum.INACTIVE
    ));
  }
}
