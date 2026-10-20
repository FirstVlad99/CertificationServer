package ru.cert.certificationserver.kafka.email;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
    property = "type"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = MailPayload.AccountActivation.class, name = "ACCOUNT_ACTIVATION"),
    @JsonSubTypes.Type(value = MailPayload.PasswordReset.class, name = "PASSWORD_RESET")
})
public sealed interface MailPayload {
  record PasswordReset(String code) implements MailPayload {}
  record AccountActivation(String code) implements MailPayload {}
}
