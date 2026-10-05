package ru.cert.certificationserver.kafka.email.listener;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import ru.cert.certificationserver.dto.auth.event.MailEvent;
import ru.cert.certificationserver.dto.auth.event.ResetPasswordEmailEvent;
import ru.cert.certificationserver.kafka.email.MailProducer;
import ru.cert.certificationserver.model.enums.MailEventType;

import java.util.Map;

@Component
public class ResetPasswordEmailListener {
  private final MailProducer mailProducer;

  public ResetPasswordEmailListener(MailProducer mailProducer) {
    this.mailProducer = mailProducer;
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onResetPasswordEmail(ResetPasswordEmailEvent event) {
    MailEvent mail = new MailEvent();
    mail.setTo(event.email());
    mail.setType(MailEventType.PASSWORD_RESET);
    mail.setPayload(Map.of("code", event.code()));
    mailProducer.send(mail);
  }
}
