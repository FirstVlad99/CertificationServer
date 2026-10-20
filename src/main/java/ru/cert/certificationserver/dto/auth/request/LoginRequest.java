package ru.cert.certificationserver.dto.auth.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import ru.cert.certificationserver.validation.JmailEmail;
import ru.cert.certificationserver.validation.StrongPassword;

public record LoginRequest(
    @NotBlank(message = "Email must not be blank") @JmailEmail String email,
    @NotBlank(message = "Password must not be blank") @Size(min = 8, message = "Password must be at least 8 characters long") @StrongPassword String password,
    @NotBlank(message = "Role must not be blank") String role) {
}
