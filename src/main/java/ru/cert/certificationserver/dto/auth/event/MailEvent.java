package ru.cert.certificationserver.dto.auth.event;

import ru.cert.certificationserver.kafka.email.MailPayload;
import ru.cert.certificationserver.model.enums.MailEventType;

import java.util.Map;

public record MailEvent(String to, MailEventType type, MailPayload payload) {}
