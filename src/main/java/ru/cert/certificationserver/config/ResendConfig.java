package ru.cert.certificationserver.config;

import com.resend.Resend;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.cert.certificationserver.config.properties.ResendProperties;

@Configuration
public class ResendConfig {
  private final ResendProperties resendProperties;

  public ResendConfig(ResendProperties resendProperties) {
    this.resendProperties = resendProperties;
  }

  @Bean
  public Resend resend() {
    return new Resend(resendProperties.apiKey());
  }
}
