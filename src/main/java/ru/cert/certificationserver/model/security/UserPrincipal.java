package ru.cert.certificationserver.model.security;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

public class UserPrincipal implements UserDetails {
  private final Long id;
  private final String email;
  private final String password;
  private final String sessionId;
  private final List<GrantedAuthority> authorities;
  private final boolean enabled;

  public UserPrincipal(
      Long id,
      String email,
      String password,
      List<GrantedAuthority> authorities,
      String sessionId,
      boolean enabled
  ) {
    this.id = id;
    this.email = email;
    this.password = password;
    this.authorities = authorities;
    this.sessionId = sessionId;
    this.enabled = enabled;
  }

  public Long getId() {
    return id;
  }

  public String getEmail() {
    return email;
  }

  public List<GrantedAuthority> getAuthorities() {
    return authorities;
  }

  @Override
  public @Nullable String getPassword() {
    return password;
  }

  @Override
  public String getUsername() {
    return email;
  }

  @Override
  public boolean isAccountNonExpired() {
    return UserDetails.super.isAccountNonExpired();
  }

  @Override
  public boolean isAccountNonLocked() {
    return UserDetails.super.isAccountNonLocked();
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return UserDetails.super.isCredentialsNonExpired();
  }

  /**
   * {@code false} для деактивированного аккаунта (system_status == INACTIVE). В отличие от
   * дефолта фреймворка это реальное значение: фильтр собирает токен вручную (без цепочки
   * AuthenticationProvider), поэтому проверяет это явно, а не полагается на
   * автоматический AccountStatusUserDetailsChecker из Spring.
   */
  @Override
  public boolean isEnabled() {
    return enabled;
  }

  public String getSessionId() {
    return sessionId;
  }
}
