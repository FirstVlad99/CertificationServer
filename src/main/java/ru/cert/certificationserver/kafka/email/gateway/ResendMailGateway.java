package ru.cert.certificationserver.kafka.email.gateway;

import com.resend.Resend;
import com.resend.services.emails.model.CreateEmailOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import ru.cert.certificationserver.config.properties.AppProperties;
import ru.cert.certificationserver.config.properties.EmailProperties;
import ru.cert.certificationserver.config.properties.TtlProperties;
import ru.cert.certificationserver.kafka.email.MailGateway;
import ru.cert.certificationserver.repository.redis.RedisSessionStore;

@Service
public class ResendMailGateway implements MailGateway {
  private final Resend resend;
  private final TemplateEngine templateEngine;
  private final EmailProperties emailProperties;
  private final AppProperties appProperties;
  private final TtlProperties ttlProperties;
  private static final Logger log = LoggerFactory.getLogger(ResendMailGateway.class);

  public ResendMailGateway(Resend resend, TemplateEngine templateEngine, EmailProperties emailProperties, AppProperties appProperties, TtlProperties ttlProperties) {
    this.resend = resend;
    this.templateEngine = templateEngine;
    this.emailProperties = emailProperties;
    this.appProperties = appProperties;
    this.ttlProperties = ttlProperties;
  }

  @Override
  public void sendPasswordReset(String to, String code) {
    try {
      Context context = new Context();
      context.setVariable("appName", appProperties.name());
      context.setVariable("resetCode", code);
      context.setVariable("expirationMinutes", ttlProperties.passwordReset().toMinutes());

      String htmlContent = templateEngine.process("password_reset", context);

      CreateEmailOptions options = CreateEmailOptions.builder()
          .from(emailProperties.from())
          .to(to)
          .subject(appProperties.name() + ": Сброс пароля")
          .html(htmlContent)
          .build();

      resend.emails().send(options);
    } catch (Exception | LinkageError e) {
      log.error("Failed to send password reset email to {}", to, e);
    }
  }

  @Override
  public void sendAccountActivation(String to, String code) {
    try {
      Context context = new Context();
      context.setVariable("appName", appProperties.name());
      context.setVariable("confirmationCode", code);
      context.setVariable("expirationMinutes", ttlProperties.activation().toMinutes());

      String htmlContent = templateEngine.process("account_activation", context);

      CreateEmailOptions options = CreateEmailOptions.builder()
          .from(emailProperties.from())
          .to(to)
          .subject(appProperties.name() + ": Подтверждение email ящика")
          .html(htmlContent)
          .build();

      resend.emails().send(options);
    } catch (Exception | LinkageError e) {
      log.error("Failed to send account activation email to {}", to, e);
    }
  }
}
