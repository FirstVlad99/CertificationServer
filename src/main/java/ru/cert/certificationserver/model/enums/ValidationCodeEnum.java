package ru.cert.certificationserver.model.enums;

public enum ValidationCodeEnum {
  REQUIRED_FIELD,
  INVALID_LENGTH,
  INVALID_FORMAT,
  WEAK_PASSWORD,
  INVALID_VALUE,
  ALREADY_EXISTS,
  VALIDATION_FAILED,
  INVALID_DATE_RANGE;

  public static ValidationCodeEnum getCodeBySpringCode(String springCode) {
    return switch (springCode) {
      case "NotBlank", "NotNull" -> ValidationCodeEnum.REQUIRED_FIELD;
      case "Size", "Length"      -> ValidationCodeEnum.INVALID_LENGTH;
      case "Email", "Pattern"    -> ValidationCodeEnum.INVALID_FORMAT;
      default                    -> ValidationCodeEnum.VALIDATION_FAILED;
    };
  }
}
