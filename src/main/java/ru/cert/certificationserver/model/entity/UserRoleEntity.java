package ru.cert.certificationserver.model.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "user_roles")
public class UserRoleEntity {
  @EmbeddedId
  private UserRoleId id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  @MapsId("userId")
  private UserEntity userEntity;

  @Column(name = "granted_at")
  private OffsetDateTime grantedAt;

  public UserRoleEntity() {
  }

  public UserRoleEntity(UserRoleId id, UserEntity userEntity, OffsetDateTime grantedAt) {
    this.id = id;
    this.userEntity = userEntity;
    this.grantedAt = grantedAt;
  }

  public UserRoleId getId() {
    return id;
  }

  public void setId(UserRoleId id) {
    this.id = id;
  }

  public UserEntity getUserEntity() {
    return userEntity;
  }

  public void setUserEntity(UserEntity userEntity) {
    this.userEntity = userEntity;
  }

  public OffsetDateTime getGrantedAt() {
    return grantedAt;
  }

  public void setGrantedAt(OffsetDateTime grantedAt) {
    this.grantedAt = grantedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    UserRoleEntity that = (UserRoleEntity) o;
    return Objects.equals(id, that.id) && Objects.equals(userEntity, that.userEntity) && Objects.equals(grantedAt, that.grantedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, userEntity, grantedAt);
  }
}
