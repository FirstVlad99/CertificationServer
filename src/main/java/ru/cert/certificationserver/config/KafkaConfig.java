package ru.cert.certificationserver.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.util.backoff.FixedBackOff;
import ru.cert.certificationserver.config.properties.KafkaProperties;
import ru.cert.certificationserver.dto.auth.event.MailEvent;
import ru.cert.certificationserver.kafka.deserializer.CustomDeserializer;
import ru.cert.certificationserver.kafka.serializer.CustomSerializer;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
public class KafkaConfig {
  private final KafkaProperties kafkaProperties;

  public KafkaConfig(KafkaProperties kafkaProperties) {
    this.kafkaProperties = kafkaProperties;
  }

  @Bean
  public ProducerFactory<String, MailEvent> producerFactory() {
    Map<String, Object> config = new HashMap<>();

    config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.bootstrapServers());
    config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    config.put(
        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
        CustomSerializer.class
    );
    config.put("security.protocol", kafkaProperties.properties().get("security.protocol"));
    config.put("sasl.mechanism", kafkaProperties.properties().get("sasl.mechanism"));
    config.put("sasl.jaas.config", kafkaProperties.properties().get("sasl.jaas.config"));

    return new DefaultKafkaProducerFactory<>(config);
  }

  @Bean
  public ConsumerFactory<String, MailEvent> consumerFactory() {
    Map<String, Object> props = new HashMap<>();

    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.bootstrapServers());
    props.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaProperties.consumer().groupId());
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    props.put("security.protocol", kafkaProperties.properties().get("security.protocol"));
    props.put("sasl.mechanism", kafkaProperties.properties().get("sasl.mechanism"));
    props.put("sasl.jaas.config", kafkaProperties.properties().get("sasl.jaas.config"));

    return new DefaultKafkaConsumerFactory<>(
        props,
        new StringDeserializer(),
        new ErrorHandlingDeserializer<>(new CustomDeserializer<>(MailEvent.class))
    );
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, MailEvent> kafkaListenerContainerFactory() {
    ConcurrentKafkaListenerContainerFactory<String, MailEvent> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory());
    factory.setCommonErrorHandler(new DefaultErrorHandler(new FixedBackOff(0L, 0L)));
    return factory;
  }

  @Bean
  public KafkaTemplate<String, MailEvent> kafkaTemplate() {
    return new KafkaTemplate<>(producerFactory());
  }

}