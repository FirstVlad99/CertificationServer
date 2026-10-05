package ru.cert.certificationserver.exception;

import ru.cert.certificationserver.model.enums.AuthErrorEnum;
import ru.cert.certificationserver.model.enums.ErrorCodeEnum;

public class AuthException extends RuntimeException {
  private final ErrorCodeEnum code;
  private final AuthErrorEnum message;

  public AuthException(ErrorCodeEnum code, AuthErrorEnum message) {
    super(message.name());
    this.code = code;
    this.message = message;
  }

  public ErrorCodeEnum getErrorCode() {
    return code;
  }

  public AuthErrorEnum getAuthError() {
    return message;
  }
}
