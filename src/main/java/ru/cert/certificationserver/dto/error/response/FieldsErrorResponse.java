package ru.cert.certificationserver.dto.error.response;

import ru.cert.certificationserver.model.enums.ErrorCodeEnum;
import ru.cert.certificationserver.model.enums.ValidationCodeEnum;

import java.util.Map;

public class FieldsErrorResponse {
  private final ErrorCodeEnum code;
  private final String message;
  // fields описывает форму полей запроса, поэтому значение тут ровно одно - ValidationCodeEnum.
  // Доменные ошибки  отдаются плоским error_code без fields
  private final Map<String, ValidationCodeEnum> fields;

  public FieldsErrorResponse(String message, Map<String, ValidationCodeEnum> fields) {
    this(ErrorCodeEnum.VALIDATION_FAILED, message, fields);
  }

  public FieldsErrorResponse(ErrorCodeEnum code, String message, Map<String, ValidationCodeEnum> fields) {
    this.code = code;
    this.message = message;
    this.fields = fields;
  }

  public ErrorCodeEnum getCode() {
    return code;
  }

  public String getMessage() {
    return message;
  }

  public Map<String, ValidationCodeEnum> getFields() {
    return fields;
  }
}

