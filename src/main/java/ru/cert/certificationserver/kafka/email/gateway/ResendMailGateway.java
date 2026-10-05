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
import ru.cert.certificationserver.kafka.email.MailGateway;

@Service
public class ResendMailGateway implements MailGateway {
  private final Resend resend;
  private final TemplateEngine templateEngine;
  private final EmailProperties emailProperties;
  private final AppProperties appProperties;
  private static final Logger log = LoggerFactory.getLogger(ResendMailGateway.class);

  public ResendMailGateway(Resend resend, TemplateEngine templateEngine, EmailProperties emailProperties, AppProperties appProperties) {
    this.resend = resend;
    this.templateEngine = templateEngine;
    this.emailProperties = emailProperties;
    this.appProperties = appProperties;
  }


  @Override
  public void sendPasswordReset(String to, String token) {
    try {
      Context context = new Context();
      context.setVariable("appName", appProperties.name());
      context.setVariable("resetLink", appProperties.url() + "/reset-password?token=" + token);

      String htmlContent = templateEngine.process("password_reset", context);

      CreateEmailOptions options = CreateEmailOptions.builder()
          .from(emailProperties.from())
          .to(to)
          .subject(appProperties.name() + ": Password Reset Request")
          .html(htmlContent)
          .build();

      resend.emails().send(options);
    } catch (Exception | LinkageError e) {
      log.error("Failed to send password reset email to {}", to, e);
    }
  }

  @Override
  public void sendAccountActivation(String to, String token) {
    try {
      Context context = new Context();
      context.setVariable("appName", appProperties.name());
      context.setVariable("activationLink", appProperties.url() + "/activate?token=" + token);

      String htmlContent = templateEngine.process("account_activation", context);

      CreateEmailOptions options = CreateEmailOptions.builder()
          .from(emailProperties.from())
          .to(to)
          .subject(appProperties.name() + ": Activate Your Account")
          .html(htmlContent)
          .build();

      resend.emails().send(options);
    } catch (Exception | LinkageError e) {
      log.error("Failed to send account activation email to {}", to, e);
    }
  }
}
