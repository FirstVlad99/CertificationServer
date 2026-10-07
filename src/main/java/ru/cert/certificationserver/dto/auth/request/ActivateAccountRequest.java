package ru.cert.certificationserver.dto.auth.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import ru.cert.certificationserver.model.enums.RoleNameEnum;
import ru.cert.certificationserver.model.enums.UserSystemStatusNameEnum;

// Не указан role, т.к. по умолчанию все новые пользователи не могут состоять в организации
public class ActivateAccountRequest{
  @NotBlank(message = "Code must not be blank") String code;
  static final String role = RoleNameEnum.CUSTOMER.name();

  public ActivateAccountRequest(String code) {
    this.code = code;
  }

  public String getCode() {
    return code;
  }

  public String getRole() {
    return role;
  }
}
