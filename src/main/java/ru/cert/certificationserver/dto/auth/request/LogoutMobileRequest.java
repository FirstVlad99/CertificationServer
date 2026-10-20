package ru.cert.certificationserver.dto.auth.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record LogoutMobileRequest(
    @NotBlank(message = "Refresh token must not be blank") @JsonProperty("refresh_token") String refreshToken
) { }
