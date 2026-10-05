package ru.cert.certificationserver.model.entity;

import jakarta.persistence.*;
import ru.cert.certificationserver.model.enums.UserSystemStatusNameEnum;

@Entity
@Table(name = "user_system_statuses")
public class UserSystemStatusEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "name", nullable = false)
  @Enumerated(EnumType.STRING)
  private UserSystemStatusNameEnum name;

  public UserSystemStatusEntity() {
  }

  public UserSystemStatusEntity(Integer id, UserSystemStatusNameEnum name) {
    this.id = id;
    this.name = name;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public UserSystemStatusNameEnum getName() {
    return name;
  }

  public void setName(UserSystemStatusNameEnum name) {
    this.name = name;
  }
}
