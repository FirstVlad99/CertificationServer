package ru.cert.certificationserver.kafka.email.listener;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import ru.cert.certificationserver.dto.auth.event.ActivationEmailEvent;
import ru.cert.certificationserver.dto.auth.event.MailEvent;
import ru.cert.certificationserver.kafka.email.MailPayload;
import ru.cert.certificationserver.kafka.email.MailProducer;
import ru.cert.certificationserver.model.enums.MailEventType;

import java.util.Map;

@Component
public class ActivationEmailListener {
  private final MailProducer mailProducer;

  public ActivationEmailListener(MailProducer mailProducer) {
    this.mailProducer = mailProducer;
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onActivationEmail(ActivationEmailEvent event) {
    MailEvent mail = new MailEvent(event.email(), MailEventType.ACCOUNT_ACTIVATION,new MailPayload.AccountActivation(event.code()));
    mailProducer.send(mail);
  }
}
