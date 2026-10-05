package ru.cert.certificationserver.repository.redis;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import ru.cert.certificationserver.model.entity.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, Long>,
    JpaSpecificationExecutor<UserEntity> {

}
