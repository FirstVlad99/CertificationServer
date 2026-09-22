package ru.cert.certificationserver.model.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import ru.cert.certificationserver.model.enums.UploadStatusEnum;

import java.time.OffsetDateTime;

// BatchSize(100) для избежания n+1 при получении пачки файлов
@Entity
@Table(name = "repository_files")
@BatchSize(size = 100)
@EntityListeners(AuditingEntityListener.class)
public class RepositoryFileEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "s3_key", nullable = false, unique = true)
  private String s3Key;

  @Column(name = "name", nullable = false)
  private String name;

  // nullable: у presigned размер неизвестен до confirm (HeadObject Content-Length).
  @Column(name = "size_bytes")
  private Long sizeBytes;

  @Column(name = "mime_type", nullable = false)
  private String mime_type;

  @Enumerated(EnumType.STRING)
  @Column(name = "upload_status", nullable = false)
  private UploadStatusEnum uploadStatus;

  @CreatedDate
  @Column(name = "created_at", updatable = false)
  private OffsetDateTime createdAt;

  @Column(name = "uploaded_by", nullable = false)
  private Long uploadedBy;

  public RepositoryFileEntity() {
  }

  public RepositoryFileEntity(Long id, String s3Key, String name, Long sizeBytes, String mime_type, UploadStatusEnum uploadStatus, OffsetDateTime createdAt, Long uploadedBy) {
    this.id = id;
    this.s3Key = s3Key;
    this.name = name;
    this.sizeBytes = sizeBytes;
    this.mime_type = mime_type;
    this.uploadStatus = uploadStatus;
    this.createdAt = createdAt;
    this.uploadedBy = uploadedBy;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getS3Key() {
    return s3Key;
  }

  public void setS3Key(String s3Key) {
    this.s3Key = s3Key;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Long getSizeBytes() {
    return sizeBytes;
  }

  public void setSizeBytes(Long sizeBytes) {
    this.sizeBytes = sizeBytes;
  }

  public String getMime_type() {
    return mime_type;
  }

  public void setMime_type(String mime_type) {
    this.mime_type = mime_type;
  }

  public UploadStatusEnum getUploadStatus() {
    return uploadStatus;
  }

  public void setUploadStatus(UploadStatusEnum uploadStatus) {
    this.uploadStatus = uploadStatus;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public Long getUploadedBy() {
    return uploadedBy;
  }

  public void setUploadedBy(Long uploadedBy) {
    this.uploadedBy = uploadedBy;
  }
}
