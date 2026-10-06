package ru.cert.certificationserver.kafka.serializer;

import io.jsonwebtoken.io.SerializationException;
import org.apache.kafka.common.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;

public class CustomSerializer<T> implements Serializer<T> {
  private final ObjectMapper objectMapper = new ObjectMapper();
  private static final Logger log = LoggerFactory.getLogger(CustomSerializer.class);

  @Override
  public byte[] serialize(String topic, T data) {
    try {
      if (data == null) {
        log.error("Null received at serializing");
        return null;
      }
      return objectMapper.writeValueAsBytes(data);
    } catch (Exception e) {
      throw new SerializationException("Error when serializing MessageDto to byte[]");
    }
  }
}

