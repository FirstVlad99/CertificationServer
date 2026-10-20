package ru.cert.certificationserver.config.properties;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "spring.kafka.mail")
@Validated
public record MailKafkaProperties(
    @NotBlank String topic,
    @NotBlank String groupId
) {}