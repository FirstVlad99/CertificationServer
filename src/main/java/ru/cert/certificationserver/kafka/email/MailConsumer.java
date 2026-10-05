package ru.cert.certificationserver.kafka.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.cert.certificationserver.dto.auth.event.MailEvent;
import ru.cert.certificationserver.model.enums.MailEventType;

@Component
public class MailConsumer {
  private final MailGateway mailGateway;
  private static final Logger log = LoggerFactory.getLogger(MailConsumer.class);

  public MailConsumer(MailGateway mailGateway) {
    this.mailGateway = mailGateway;
  }

  @KafkaListener(
      topics = "mail.send",
      groupId = "mail-service"
  )
  public void consume(MailEvent event) {
    log.info("Mail event received: {}",
        event
    );
    switch (event.getType()) {
      case MailEventType.PASSWORD_RESET -> mailGateway.sendPasswordReset(
          event.getTo(),
          event.getPayload().get("token")
      );
      case MailEventType.ACCOUNT_ACTIVATION -> mailGateway.sendAccountActivation(
          event.getTo(),
          event.getPayload().get("token")
      );
      default -> log.warn("Unhandled mail event type: {}", event.getType());
    }
  }
}
