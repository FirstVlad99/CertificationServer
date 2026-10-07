package ru.cert.certificationserver.validation;

import com.sanctionco.jmail.JMail;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class JmailEmailValidator implements ConstraintValidator<JmailEmail, String> {
  @Override
  public boolean isValid(String value, ConstraintValidatorContext ctx) {
    if (value == null || value.isBlank()) {
      return true;
    }
    return JMail.strictValidator().isValid(value);
  }
}
