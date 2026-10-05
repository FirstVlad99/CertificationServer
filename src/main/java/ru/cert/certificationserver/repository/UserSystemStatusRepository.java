package ru.cert.certificationserver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.cert.certificationserver.model.entity.UserSystemStatusEntity;

import java.util.Optional;

public interface UserSystemStatusRepository extends JpaRepository<UserSystemStatusEntity, Integer> {
  Optional<UserSystemStatusEntity> findByName(String status);
}
