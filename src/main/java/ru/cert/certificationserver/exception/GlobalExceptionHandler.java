package ru.cert.certificationserver.exception;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestValueException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import ru.cert.certificationserver.config.properties.MultipartProperties;
import ru.cert.certificationserver.dto.error.response.ErrorResponse;
import ru.cert.certificationserver.dto.error.response.FieldsErrorResponse;
import ru.cert.certificationserver.model.enums.AuthErrorEnum;
import ru.cert.certificationserver.model.enums.ErrorCodeEnum;
import ru.cert.certificationserver.model.enums.ValidationCodeEnum;
import tools.jackson.databind.DatabindException;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@RestControllerAdvice
public class GlobalExceptionHandler {
  private final MultipartProperties multipartProperties;

  public GlobalExceptionHandler(MultipartProperties multipartProperties) {
    this.multipartProperties = multipartProperties;
  }

  @ExceptionHandler(AuthException.class)
  public ResponseEntity<ErrorResponse> handleAuthException(AuthException ex) {
    ErrorResponse body = new ErrorResponse(
        ex.getErrorCode(),
        toReadableMessage(ex.getAuthError()),
        ex.getAuthError().name()
    );

    return ResponseEntity.status(toHttpStatus(ex.getErrorCode())).body(body);
  }

  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<ErrorResponse> handleBadCredentialsException(BadCredentialsException ex) {
    ErrorResponse errorBody = new ErrorResponse(
        ErrorCodeEnum.UNAUTHORIZED,
        "Incorrect email or password.",
        "BAD_CREDENTIALS"
    );
    return ResponseEntity.status(toHttpStatus(errorBody.code())).body(errorBody);
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ErrorResponse> handleNoResourceFoundException(NoResourceFoundException ex) {
    ErrorResponse errorBody = new ErrorResponse(
        ErrorCodeEnum.NOT_FOUND,
        String.format("Endpoint '%s' not found", ex.getResourcePath())
    );
    return ResponseEntity.status(toHttpStatus(errorBody.code())).body(errorBody);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<FieldsErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
    Map<String, ValidationCodeEnum> errors = new HashMap<>();

    ex.getBindingResult().getAllErrors().forEach((error) -> {
      // Безопасно проверяем, что ошибка относится именно к полю
      if (error instanceof FieldError fieldError) {
        // Имя поля - проводное, а не Java-шное: фронт ищет ошибку по ключу из тела запроса.
        String fieldName = JsonFieldPath.of(ex.getBindingResult().getTarget(), fieldError.getField());
        String springCode = error.getCode(); // Получаем имя аннотации (NotBlank, Size)

        // Маппим имя аннотации на подходящие константы из ErrorCodeEnum
        ValidationCodeEnum errorEnum = mapSpringCodeToValidationCode(springCode);

        errors.put(fieldName, errorEnum);
      }
    });

    FieldsErrorResponse errorBody = new FieldsErrorResponse(
        "Input data failed validation.",
        errors
    );

    return ResponseEntity.status(toHttpStatus(errorBody.getCode())).body(errorBody);
  }

  @ExceptionHandler(HandlerMethodValidationException.class)
  public ResponseEntity<FieldsErrorResponse> handleHandlerMethodValidation(
      HandlerMethodValidationException ex
  ) {
    Map<String, ValidationCodeEnum> errors = new HashMap<>();

    ex.getParameterValidationResults().forEach(result -> {
      String paramName = result.getMethodParameter().getParameterName();
      if (paramName == null) {
        paramName = "parameter";
      }
      for (var error : result.getResolvableErrors()) {
        if (error instanceof DefaultMessageSourceResolvable resolvable) {
          String springCode = resolvable.getCode();
          errors.put(paramName, mapSpringCodeToValidationCode(springCode));
        }
      }
    });

    FieldsErrorResponse errorBody = new FieldsErrorResponse(
        "Input data failed validation.",
        errors
    );
    return ResponseEntity.status(toHttpStatus(errorBody.getCode())).body(errorBody);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<FieldsErrorResponse> handleTypeMismatchException(MethodArgumentTypeMismatchException ex) {
    String requiredTypeName = ex.getRequiredType() != null
        ? ex.getRequiredType().getSimpleName()
        : "unknown type";

    Map<String, ValidationCodeEnum> fields = new HashMap<>();
    fields.put(ex.getName(), ValidationCodeEnum.INVALID_FORMAT);

    FieldsErrorResponse errorBody = new FieldsErrorResponse(
        ErrorCodeEnum.BAD_REQUEST,
        String.format("Parameter '%s' must be of type %s", ex.getName(), requiredTypeName),
        fields
    );
    return ResponseEntity.status(toHttpStatus(errorBody.getCode())).body(errorBody);
  }

  // Корректно возвращаем 405 вместо 500, если перепутали метод (GET на POST, например)
  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleMethodNotSupportedException(HttpRequestMethodNotSupportedException ex) {
    ErrorResponse errorBody = new ErrorResponse(
        ErrorCodeEnum.METHOD_NOT_ALLOWED,
        String.format("Method %s is not supported for this route", ex.getMethod())
    );
    return ResponseEntity.status(toHttpStatus(errorBody.code())).body(errorBody);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
    ErrorResponse errorBody = new ErrorResponse(
        ErrorCodeEnum.BAD_REQUEST,
        ex.getMessage()
    );
    return ResponseEntity.status(toHttpStatus(errorBody.code())).body(errorBody);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<FieldsErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
    Map<String, ValidationCodeEnum> fields = new HashMap<>();

    String fieldName = extractFieldName(ex);
    if (fieldName != null) {
      fields.put(fieldName, ValidationCodeEnum.INVALID_VALUE);
    }

    FieldsErrorResponse errorBody = new FieldsErrorResponse(
        ErrorCodeEnum.BAD_REQUEST,
        "Invalid request body: check the JSON format, values, and field names.",
        fields
    );

    return ResponseEntity.status(toHttpStatus(errorBody.getCode())).body(errorBody);
  }

  @ExceptionHandler(MissingRequestValueException.class)
  public ResponseEntity<FieldsErrorResponse> handleMissingRequestValueException(MissingRequestValueException ex) {
    Map<String, ValidationCodeEnum> fields = new HashMap<>();

    // имя параметра/cookie/заголовка везде в одинарных кавычках.
    String fieldName = extractField(ex.getMessage(), "'([a-zA-Z_]+)'");
    if (fieldName != null) {
      fields.put(fieldName, ValidationCodeEnum.REQUIRED_FIELD);
    }

    FieldsErrorResponse errorBody = new FieldsErrorResponse(
        ErrorCodeEnum.BAD_REQUEST,
        "Missing mandatory request parameter: " + ex.getMessage(),
        fields
    );
    return ResponseEntity.status(toHttpStatus(errorBody.getCode())).body(errorBody);
  }

  private static String extractField(String message, String regex) {
    if (message == null) {
      return null;
    }

    var matcher = Pattern.compile(regex).matcher(message);
    return matcher.find() ? matcher.group(1) : null;
  }

  private static String extractFieldName(HttpMessageNotReadableException ex) {
    if (!(ex.getCause() instanceof DatabindException databindException)) {
      return null;
    }

    StringBuilder sb = new StringBuilder();
    for (var ref : databindException.getPath()) {
      if (ref.getPropertyName() != null) {
        if (!sb.isEmpty()) {
          sb.append('.');
        }
        sb.append(ref.getPropertyName());
      } else if (ref.getIndex() >= 0) {
        sb.append('[').append(ref.getIndex()).append(']');
      }
    }

    return !sb.isEmpty() ? sb.toString() : null;
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleAllUnexpectedErrors(Exception ex) {
    ErrorResponse errorBody = new ErrorResponse(
        ErrorCodeEnum.INTERNAL_SERVER_ERROR,
        "An internal server error occurred."
    );

    return ResponseEntity.status(toHttpStatus(errorBody.code())).body(errorBody);
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceeded(
      MaxUploadSizeExceededException ex) {

    String message = ex.getMostSpecificCause().getMessage();

    if (message != null && message.contains("Header section has more than")) {
      ErrorResponse errorBody = new ErrorResponse(
          ErrorCodeEnum.REQUEST_HEADER_FIELDS_TOO_LARGE,
          "Filename is too long"
      );

      return ResponseEntity
          .status(toHttpStatus(errorBody.code()))
          .body(errorBody);
    }

    ErrorResponse errorBody = new ErrorResponse(
        ErrorCodeEnum.CONTENT_TOO_LARGE,
        "The file size exceeds the limit: " + multipartProperties.maxFileSize() + "MB"
    );

    return ResponseEntity
        .status(toHttpStatus(errorBody.code()))
        .body(errorBody);
  }

  @ExceptionHandler(MultipartException.class)
  public ResponseEntity<ErrorResponse> handleMultipartException(
      MultipartException ex) {

    ErrorResponse errorBody = new ErrorResponse(
        ErrorCodeEnum.BAD_REQUEST,
        "The request must contain multipart/form-data"
    );

    return ResponseEntity
        .status(toHttpStatus(errorBody.code()))
        .body(errorBody);
  }

  @ExceptionHandler(MissingServletRequestPartException.class)
  public ResponseEntity<ErrorResponse> handleMissingServletRequestPart(
      MissingServletRequestPartException ex) {

    String missingPartName = ex.getRequestPartName();

    String message = String.format(
        "A mandatory part of the request is missing.: '%s'",
        missingPartName
    );

    ErrorResponse errorBody = new ErrorResponse(
        ErrorCodeEnum.BAD_REQUEST,
        message
    );

    return ResponseEntity
        .status(toHttpStatus(errorBody.code()))
        .body(errorBody);
  }

  @ExceptionHandler(EntityNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleEntityNotFoundException(EntityNotFoundException ex) {
    ErrorResponse errorBody = new ErrorResponse(
        ErrorCodeEnum.NOT_FOUND,
        ex.getMessage()
    );
    return ResponseEntity.status(toHttpStatus(errorBody.code())).body(errorBody);
  }


  private static String toReadableMessage(AuthErrorEnum error) {
    return switch (error) {
      case ACCOUNT_NOT_ACTIVATED -> "Account not yet activated.";
      case BAD_CREDENTIALS -> "Invalid email or password.";
      case INVALID_REFRESH_TOKEN -> "Invalid refresh token.";
      case ALREADY_ACTIVATED -> "Account already activated.";
      case TOKEN_EXPIRED -> "Token expired.";
      case ACCOUNT_INACTIVE -> "Account deactivated.";
      case EMAIL_ALREADY_EXISTS -> "Email already exists.";
    };
  }

  private static HttpStatus toHttpStatus(ErrorCodeEnum errorCode) {
    return switch (errorCode) {
      case NOT_FOUND -> HttpStatus.NOT_FOUND;
      case BAD_REQUEST, VALIDATION_FAILED -> HttpStatus.BAD_REQUEST;
      case METHOD_NOT_ALLOWED -> HttpStatus.METHOD_NOT_ALLOWED;
      case INTERNAL_SERVER_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
      case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
      case FORBIDDEN -> HttpStatus.FORBIDDEN;
      case CONFLICT -> HttpStatus.CONFLICT;
      case CONTENT_TOO_LARGE -> HttpStatus.CONTENT_TOO_LARGE;
      case REQUEST_HEADER_FIELDS_TOO_LARGE -> HttpStatus.REQUEST_HEADER_FIELDS_TOO_LARGE;
    };
  }

  private static ValidationCodeEnum mapSpringCodeToValidationCode(String springCode) {
    return switch (springCode != null ? springCode : "") {
      case "NotBlank", "NotNull", "NotEmpty" -> ValidationCodeEnum.REQUIRED_FIELD;
      case "Size", "Length" -> ValidationCodeEnum.INVALID_LENGTH;
      case "Email", "Pattern", "JmailEmail" -> ValidationCodeEnum.INVALID_FORMAT;
      case "StrongPassword" -> ValidationCodeEnum.WEAK_PASSWORD;
      default -> ValidationCodeEnum.VALIDATION_FAILED;
    };
  }
}
