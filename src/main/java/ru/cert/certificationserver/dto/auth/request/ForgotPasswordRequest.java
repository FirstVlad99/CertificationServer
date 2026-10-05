package ru.cert.certificationserver.dto.auth.request;

import jakarta.validation.constraints.NotBlank;
import ru.cert.certificationserver.validation.JmailEmail;

public record ForgotPasswordRequest(@NotBlank(message = "Email must not be blank") @JmailEmail String email) { }
