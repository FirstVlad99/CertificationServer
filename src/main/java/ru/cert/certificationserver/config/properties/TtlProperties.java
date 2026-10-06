package ru.cert.certificationserver.config.properties;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@ConfigurationProperties(prefix = "ttl")
@Validated
public record TtlProperties(@NotNull Duration activation, @NotNull Duration passwordReset, @NotNull Duration session) { }
