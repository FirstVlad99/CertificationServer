package ru.cert.certificationserver.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Component;
import ru.cert.certificationserver.model.entity.UserSystemStatusEntity;
import ru.cert.certificationserver.model.enums.UserSystemStatusNameEnum;
import ru.cert.certificationserver.repository.UserSystemStatusRepository;

@Component
public class UserReferenceResolver {
  private final UserSystemStatusRepository userSystemStatusRepository;


  public UserReferenceResolver(UserSystemStatusRepository userSystemStatusRepository) {
    this.userSystemStatusRepository = userSystemStatusRepository;
  }

  public UserSystemStatusEntity userSystemStatus(UserSystemStatusNameEnum status) {
    return userSystemStatusRepository.findByName(status.toString())
        .orElseThrow(() ->
            new EntityNotFoundException("User system status not found: " + status)
        );
  }
}
