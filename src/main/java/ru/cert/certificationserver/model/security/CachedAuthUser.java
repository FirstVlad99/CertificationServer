package ru.cert.certificationserver.model.security;

import java.util.Set;

// Плоский снимок данных пользователя, из которых JwtAuthenticationFilter собирает UserPrincipal.
// Кэшируется по userId (AuthUserCache). sessionId сюда НЕ входит - он из JWT, свой на каждый запрос;
// фильтр доштамповывает его сам, иначе одна сессия «протекла» бы в кэш другой.
public record CachedAuthUser(
    Long id,
    String email,
    String passwordHash,
    Boolean enabled
) {}
