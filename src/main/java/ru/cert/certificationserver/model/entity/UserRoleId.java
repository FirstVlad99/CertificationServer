package ru.cert.certificationserver.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import ru.cert.certificationserver.model.enums.RoleNameEnum;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class UserRoleId implements Serializable {

  @Column(name = "user_id")
  private Long userId;

  @Enumerated(EnumType.STRING)
  @Column(name = "role_name")
  private RoleNameEnum roleName;

  public UserRoleId() {
  }

  public UserRoleId(Long userId, RoleNameEnum roleName) {
    this.userId = userId;
    this.roleName = roleName;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    UserRoleId that = (UserRoleId) o;
    return Objects.equals(userId, that.userId) && roleName == that.roleName;
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, roleName);
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public RoleNameEnum getRoleName() {
    return roleName;
  }

  public void setRoleName(RoleNameEnum roleName) {
    this.roleName = roleName;
  }
}
