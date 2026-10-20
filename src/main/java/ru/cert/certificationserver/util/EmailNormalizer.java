package ru.cert.certificationserver.util;

import com.sanctionco.jmail.Email;
import com.sanctionco.jmail.JMail;
import com.sanctionco.jmail.normalization.NormalizationOptions;

import java.util.Optional;

public class EmailNormalizer {
  public static Optional<String> tryNormalize(String rawEmail) {
    if (rawEmail == null || rawEmail.isBlank()) {
      return Optional.empty();
    }
    return JMail.tryParse(rawEmail)
        .map(e -> e.normalized(
            NormalizationOptions.builder()
                .removeSubAddress()
                .build()));
  }
}
