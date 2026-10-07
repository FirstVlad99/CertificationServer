package ru.cert.certificationserver.kafka.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import ru.cert.certificationserver.config.properties.MailKafkaProperties;
import ru.cert.certificationserver.dto.auth.event.MailEvent;
import ru.cert.certificationserver.model.enums.MailEventType;

import java.util.Map;

@Service
public class MailProducer {
  private final KafkaTemplate<String, MailEvent> kafkaTemplate;
  private final MailKafkaProperties kafkaProperties;

  private static final Logger log = LoggerFactory.getLogger(MailProducer.class);

  public MailProducer(KafkaTemplate<String, MailEvent> kafkaTemplate, MailKafkaProperties kafkaProperties) {
    this.kafkaTemplate = kafkaTemplate;
    this.kafkaProperties = kafkaProperties;
  }

  public void send(MailEvent event) {
    kafkaTemplate.send(kafkaProperties.topic(), event)
        .whenComplete((result, ex) -> {
          if (ex != null) {
            log.error("Failed to send mail to {}: {}", event.to(), ex.getMessage(), ex);
          } else {
            log.info("Mail sent to {}: partition={}, offset={}",
                event.to(), result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
          }
        });
  }
}
