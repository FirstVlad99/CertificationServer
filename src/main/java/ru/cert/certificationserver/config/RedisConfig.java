package ru.cert.certificationserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import ru.cert.certificationserver.config.properties.RedisProperties;

@Configuration
public class RedisConfig {

  @Bean
  public RedisConnectionFactory redisConnectionFactory(RedisProperties properties) {
    RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
    config.setHostName(properties.host());
    config.setPort(properties.port());

    if (properties.password() != null) {
      config.setPassword(RedisPassword.of(properties.password()));
    }

    return new JedisConnectionFactory(config);
  }

  @Bean
  public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory cf) {
    RedisSerializer<Object> serializer = RedisSerializer.json();

    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(cf);

    template.setKeySerializer(new StringRedisSerializer());
    template.setValueSerializer(serializer);

    template.setHashKeySerializer(new StringRedisSerializer());
    template.setHashValueSerializer(serializer);

    template.afterPropertiesSet();
    return template;
  }

  @Bean
  public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory cf) {
    return new StringRedisTemplate(cf);
  }
}
