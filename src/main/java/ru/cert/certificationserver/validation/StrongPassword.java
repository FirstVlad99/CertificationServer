package ru.cert.certificationserver.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = StrongPasswordValidator.class)
public @interface StrongPassword {
  String message() default "Password must contain at least one letter, one digit, one "
      + "special character, and only Latin letters/digits/special characters (no "
      + "Cyrillic or other non-Latin scripts)";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}