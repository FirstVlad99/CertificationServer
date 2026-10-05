package ru.cert.certificationserver.config.properties;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "resend")
@Validated
public record ResendProperties(@NotBlank String apiKey) { }
