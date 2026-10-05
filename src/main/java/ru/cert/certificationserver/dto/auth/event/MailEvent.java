package ru.cert.certificationserver.dto.auth.event;

import ru.cert.certificationserver.model.enums.MailEventType;

import java.util.Map;

public class MailEvent {
  private String to;
  private MailEventType type;
  private Map<String, String> payload;

  public MailEvent(String to, MailEventType type, Map<String, String> payload) {
    this.to = to;
    this.type = type;
    this.payload = payload;
  }

  public MailEvent() {
  }

  public String getTo() {
    return to;
  }

  public void setTo(String to) {
    this.to = to;
  }

  public MailEventType getType() {
    return type;
  }

  public void setType(MailEventType type) {
    this.type = type;
  }

  public Map<String, String> getPayload() {
    return payload;
  }

  public void setPayload(Map<String, String> payload) {
    this.payload = payload;
  }
}
