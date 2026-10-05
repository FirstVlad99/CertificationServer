package ru.cert.certificationserver.model.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import ru.cert.certificationserver.model.enums.UserSystemStatusNameEnum;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
public class UserEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "username", nullable = false)
  private String username;

  @Column(name = "email", nullable = false)
  private String email;

  @Column(name = "name")
  private String name;

  @Column(name = "surname")
  private String surname;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  // @BatchSize: список пользователей мапит роли для каждой строки - без батчинга это N+1.
  @OneToMany(
      mappedBy = "userEntity",
      cascade = CascadeType.ALL,
      orphanRemoval = true
  )
  @BatchSize(size = 100)
  private Set<UserRoleEntity> roles = new HashSet<>();

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "person_type_id")
  private PersonTypeEntity personType;

  @Column(name = "bio")
  private String bio;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "photo_id")
  private RepositoryFileEntity photo;

  @Column(name = "site_url")
  private String siteUrl;

  @Column(name = "site_label")
  private String siteLabel;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "system_status_id")
  private UserSystemStatusEntity userStatus;

  @CreatedDate
  @Column(name = "created_at", updatable = false)
  private OffsetDateTime createdAt;

  @LastModifiedDate
  @Column(name = "updated_at")
  private OffsetDateTime updatedAt;

  public UserEntity() {
  }

  public UserEntity(Long id, String username, String email, String name, String surname, String passwordHash, Set<UserRoleEntity> roles, PersonTypeEntity personType, String bio, RepositoryFileEntity photo, String siteUrl, String siteLabel, UserSystemStatusEntity userStatus) {
    this.id = id;
    this.username = username;
    this.email = email;
    this.name = name;
    this.surname = surname;
    this.passwordHash = passwordHash;
    this.roles = roles;
    this.personType = personType;
    this.bio = bio;
    this.photo = photo;
    this.siteUrl = siteUrl;
    this.siteLabel = siteLabel;
    this.userStatus = userStatus;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getSurname() {
    return surname;
  }

  public void setSurname(String surname) {
    this.surname = surname;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  public Set<UserRoleEntity> getRoles() {
    return roles;
  }

  public void setRoles(Set<UserRoleEntity> roles) {
    this.roles = roles;
  }

  public PersonTypeEntity getPersonType() {
    return personType;
  }

  public void setPersonType(PersonTypeEntity personType) {
    this.personType = personType;
  }

  public String getBio() {
    return bio;
  }

  public void setBio(String bio) {
    this.bio = bio;
  }

  public RepositoryFileEntity getPhoto() {
    return photo;
  }

  public void setPhoto(RepositoryFileEntity photo) {
    this.photo = photo;
  }

  public String getSiteUrl() {
    return siteUrl;
  }

  public void setSiteUrl(String siteUrl) {
    this.siteUrl = siteUrl;
  }

  public String getSiteLabel() {
    return siteLabel;
  }

  public void setSiteLabel(String siteLabel) {
    this.siteLabel = siteLabel;
  }

  public UserSystemStatusEntity getUserStatus() {
    return userStatus;
  }

  public void setUserStatus(UserSystemStatusEntity userStatus) {
    this.userStatus = userStatus;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }
}
