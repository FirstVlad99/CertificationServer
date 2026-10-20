package ru.cert.certificationserver.dto.auth.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TokensMobileResponse(
    @JsonProperty("access_token") String accessToken,
    @JsonProperty("refresh_token") String refreshToken) { }
