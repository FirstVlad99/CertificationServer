package ru.cert.certificationserver.kafka.email;


public interface MailGateway {
  void sendPasswordReset(String to, String token);

  void sendAccountActivation(String to, String token);
}