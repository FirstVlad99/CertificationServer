package ru.cert.certificationserver.model.redis;

import java.time.Instant;

public class AuthSession {
  private String sessionId;
  private String refreshHash;
  private Instant createdAt;
  private Instant lastUsedAt;
  private String userAgent;
  private String activeRole;

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String sessionId;
    private String refreshHash;
    private Instant createdAt;
    private Instant lastUsedAt;
    private String userAgent;
    private String activeRole;

    public Builder setSessionId(String sessionId) {
      this.sessionId = sessionId;
      return this;
    }

    public Builder setRefreshHash(String refreshHash) {
      this.refreshHash = refreshHash;
      return this;
    }

    public Builder setCreatedAt(Instant createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Builder setLastUsedAt(Instant lastUsedAt) {
      this.lastUsedAt = lastUsedAt;
      return this;
    }

    public Builder setUserAgent(String userAgent) {
      this.userAgent = userAgent;
      return this;
    }

    public Builder setActiveRole(String activeRole) {
      this.activeRole = activeRole;
      return this;
    }

    public AuthSession build() {
      return new AuthSession(sessionId, refreshHash, createdAt, lastUsedAt, userAgent, activeRole);
    }
  }

  public AuthSession(String sessionId, String refreshHash, Instant createdAt, Instant lastUsedAt, String userAgent, String activeRole) {
    this.sessionId = sessionId;
    this.refreshHash = refreshHash;
    this.createdAt = createdAt;
    this.lastUsedAt = lastUsedAt;
    this.userAgent = userAgent;
    this.activeRole = activeRole;
  }

  public String getSessionId() {
    return sessionId;
  }

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  public String getRefreshHash() {
    return refreshHash;
  }

  public void setRefreshHash(String refreshHash) {
    this.refreshHash = refreshHash;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getLastUsedAt() {
    return lastUsedAt;
  }

  public void setLastUsedAt(Instant lastUsedAt) {
    this.lastUsedAt = lastUsedAt;
  }

  public String getUserAgent() {
    return userAgent;
  }

  public void setUserAgent(String userAgent) {
    this.userAgent = userAgent;
  }

  public String getActiveRole() {
    return activeRole;
  }

  public void setActiveRole(String userAgent) {
    this.activeRole = activeRole;
  }
}

