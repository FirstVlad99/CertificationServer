package ru.cert.certificationserver.kafka.deserializer;

import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

public class CustomDeserializer<T> implements Deserializer<T> {
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final Class<T> clazz;
  private static final Logger log = LoggerFactory.getLogger(CustomDeserializer.class);

  public CustomDeserializer(Class<T> clazz) {
    this.clazz = clazz;
  }

  public T deserialize(String topic, byte[] data) {
    try {
      if (data == null) {
        log.error("Null received at deserializing");
        return null;
      }
      log.debug("Deserializing {} bytes from topic {}", data.length, topic);
      return objectMapper.readValue(data, clazz);
    } catch (Exception e) {
      log.error("Failed to deserialize from topic {}. Payload: {}",
          topic, new String(data, StandardCharsets.UTF_8), e);
      throw new SerializationException(
          "Error when deserializing byte[] to " + clazz.getSimpleName(), e);  // ← передайте cause!
    }
  }
}
