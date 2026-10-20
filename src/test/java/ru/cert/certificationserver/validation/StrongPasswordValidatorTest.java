package ru.cert.certificationserver.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class StrongPasswordValidatorTest {
  private final StrongPasswordValidator validator = new StrongPasswordValidator();

  @ParameterizedTest
  @DisplayName("должен принять пароль с буквой, цифрой, спецсимволом и только латиницей")
  @ValueSource(strings = {
      "Passw0rd!",
      "abc123!@#",
      "A1!aaaaaaaaaa",
      "back\\slash1!"
  })
  void shouldAcceptValidPasswords(String password) {
    assertTrue(validator.isValid(password, null));
  }

  @ParameterizedTest
  @DisplayName("должен отклонить пароль без цифры")
  @ValueSource(strings = {"Password!", "abcdef!@#"})
  void shouldRejectPasswordWithoutDigit(String password) {
    assertFalse(validator.isValid(password, null));
  }

  @ParameterizedTest
  @DisplayName("должен отклонить пароль без спецсимвола")
  @ValueSource(strings = {"Password123", "abc12345"})
  void shouldRejectPasswordWithoutSpecialChar(String password) {
    assertFalse(validator.isValid(password, null));
  }

  @ParameterizedTest
  @DisplayName("должен отклонить пароль без единой буквы")
  @ValueSource(strings = {"12345678!", "!@#12345"})
  void shouldRejectPasswordWithoutAnyLetters(String password) {
    assertFalse(validator.isValid(password, null));
  }

  @ParameterizedTest
  @DisplayName("должен отклонить пароль с кириллицей или другой нелатинской раскладкой")
  @ValueSource(strings = {
      "Пароль123!",
      "Password123!а",
      "密码Password1!"
  })
  void shouldRejectNonLatinCharacters(String password) {
    assertFalse(validator.isValid(password, null));
  }

  @ParameterizedTest
  @DisplayName("пустая/пробельная строка не должна падать здесь")
  @ValueSource(strings = {"", " "})
  void shouldPassThroughBlankValues(String password) {
    assertTrue(validator.isValid(password, null));
  }

  @Test
  @DisplayName("null не должен падать здесь")
  void shouldPassThroughNullValue() {
    assertTrue(validator.isValid(null, null));
  }

  @Test
  @DisplayName("должен отклонить пароль с пробелом внутри")
  void shouldRejectPasswordWithSpace() {
    assertFalse(validator.isValid("Pass 123!", null));
  }
}