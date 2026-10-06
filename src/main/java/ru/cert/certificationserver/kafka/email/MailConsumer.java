package ru.cert.certificationserver.kafka.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.cert.certificationserver.config.properties.MailKafkaProperties;
import ru.cert.certificationserver.dto.auth.event.MailEvent;
import ru.cert.certificationserver.dto.auth.event.ResetPasswordEmailEvent;
import ru.cert.certificationserver.model.enums.MailEventType;

@Component
public class MailConsumer {
  private final MailGateway mailGateway;

  private static final Logger log = LoggerFactory.getLogger(MailConsumer.class);

  public MailConsumer(MailGateway mailGateway) {
    this.mailGateway = mailGateway;
  }

  @KafkaListener(
      topics = "${spring.kafka.mail.topic}",
      groupId = "${spring.kafka.mail.group-id}"
  )
  public void consume(MailEvent event) {
    log.info("Mail event received: {}",
        event
    );
    switch (event.payload()) {
      case MailPayload.PasswordReset p -> mailGateway.sendPasswordReset(event.to(), p.code());
      case MailPayload.AccountActivation p -> mailGateway.sendAccountActivation(event.to(), p.code());
    }
  }
}
