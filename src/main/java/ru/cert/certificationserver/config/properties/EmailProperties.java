package ru.cert.certificationserver.config.properties;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "email")
@Validated
public record EmailProperties(@NotBlank String from) {};
