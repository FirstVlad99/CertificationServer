package ru.cert.certificationserver.dto.error.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import ru.cert.certificationserver.model.enums.ErrorCodeEnum;

public record ErrorResponse(
    ErrorCodeEnum code,
    String message,
    @JsonProperty("error_code") String errorCode
) {
  public ErrorResponse(ErrorCodeEnum code, String message) {
    this(code, message, null);
  }
}
