package ru.cert.certificationserver.config.properties;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "spring.servlet.multipart")
@Validated
public record MultipartProperties(
    @NotNull DataSize maxFileSize,
    @NotNull DataSize maxRequestSize
) {}
