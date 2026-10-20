package ru.cert.certificationserver.config.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.Properties;

@ConfigurationProperties(prefix = "spring.kafka")
@Validated
public record KafkaProperties(
    @NotBlank String bootstrapServers,
    @NotNull Properties properties,
    @NotNull Consumer consumer
) {
  public record Consumer(@NotBlank String groupId) {}
}
