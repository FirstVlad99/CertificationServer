package ru.cert.certificationserver.dto.auth.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import ru.cert.certificationserver.validation.StrongPassword;

public record ResetPasswordRequest(
    @NotBlank(message = "Code must not be blank") String code,
    @NotBlank(message = "Password must not be blank") @JsonProperty("new_password") @Size(min = 8, message = "Password must be at least 8 characters long") @StrongPassword
    String newPassword) {
}
