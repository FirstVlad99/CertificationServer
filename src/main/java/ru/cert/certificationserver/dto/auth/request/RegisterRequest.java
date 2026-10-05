package ru.cert.certificationserver.dto.auth.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import ru.cert.certificationserver.validation.JmailEmail;
import ru.cert.certificationserver.validation.StrongPassword;

public record RegisterRequest(
    @NotBlank(message = "Email must not be blank") @JmailEmail String email,
  @NotBlank(message = "Password must not be blank") @JsonProperty("password") @Size(min = 8, message = "Password must be at least 8 characters long") @StrongPassword
    String password) {}
