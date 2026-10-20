package ru.cert.certificationserver.config.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "spring.data.redis")
@Validated
public record RedisProperties(@NotBlank String host, @NotNull Integer port, @NotBlank String password) { }

