/***********************************************************
 * created by     :  JooHyeong.Park
 * creation date  :  2026-09-28
 *
 ***********************************************************/
package azure.services.lab.managedredis;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import tools.jackson.databind.ObjectMapper;

/**
 * PROJECT     azure-services-lab-springboot
 *
 * @name       AzureManagedRedisConfiguration.java
 * @desc       Azure Managed Redis 에 대한 Redis 접근을 구성한다.
 *             - 일반 텍스트 String 값을 위한 StringRedisTemplate 을 제공한다.
 *             - 엔티티별로 하나의 타입 기반 RedisTemplate 을 생성하기 위해 EntityRedisTemplateFactory 를 제공한다.
 * @author     JooHyeong.Park
 * @since      2026-09-28
 */
@Configuration(proxyBeanMethods = false)
public class AzureManagedRedisConfiguration {

  // Spring Cloud Azure 가 구성한 RedisConnectionFactory.
  private final RedisConnectionFactory redisConnectionFactory;

  public AzureManagedRedisConfiguration(RedisConnectionFactory redisConnectionFactory) {
    this.redisConnectionFactory = redisConnectionFactory;
  }

  /*
   * 일반 텍스트 String 값을 위한 StringRedisTemplate.
   * `redisTemplate` alias 를 추가 지정하여 의도하지 않게 RedisAutoConfiguration 기반 template 의 자동 생성을 방지한다.
   */
  @Bean(name = {"stringRedisTemplate", "redisTemplate"})
  public StringRedisTemplate stringRedisTemplate() {
    StringRedisTemplate template = new StringRedisTemplate();

    // string 연산을 위해 Spring Cloud Azure 가 구성한 redisConnectionFactory 를 재사용한다.
    template.setConnectionFactory(redisConnectionFactory);

    // key :: Redis 최상위 키
    template.setKeySerializer(new StringRedisSerializer());

    // hash key :: Redis Hash 필드 이름
    template.setHashKeySerializer(new StringRedisSerializer());

    // value :: Redis value 와 Hash value. 둘 다 일반 텍스트 문자열로 저장된다.
    template.setValueSerializer(new StringRedisSerializer());
    template.setHashValueSerializer(new StringRedisSerializer());

    template.afterPropertiesSet();
    return template;
  }

  /**
   * 엔티티별 RedisTemplate 인스턴스 생성을 위한 팩토리를 제공한다.
   *
   * <p>엔티티별 RedisTemplate 생성 예제:
   *
   * <pre>{@code
   * import tools.jackson.databind.cfg.DateTimeFeature;
   * import tools.jackson.databind.json.JsonMapper;
   *
   * @Bean
   * RedisTemplate<String, ExampleEntity> exampleEntityRedisTemplate(
   *     EntityRedisTemplateFactory factory) {
   *   // Builds the ObjectMapper used by the JSON value serializer.
   *   ObjectMapper objectMapper = JsonMapper.builder()
   *       // Stores date values as ISO-8601 strings instead of epoch millisecond timestamps.
   *       .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
   *       .build();
   *
   *   return factory.createEntityRedisTemplate(objectMapper, ExampleEntity.class);
   * }
   * }</pre>
   */
  @Bean
  public EntityRedisTemplateFactory entityRedisTemplateFactory() {
    return new EntityRedisTemplateFactory(redisConnectionFactory);
  }

  /**
   * 엔티티별 RedisTemplate 인스턴스를 생성하는 Factory Class
   */
  public static final class EntityRedisTemplateFactory {

    private final RedisConnectionFactory redisConnectionFactory;

    private EntityRedisTemplateFactory(RedisConnectionFactory redisConnectionFactory) {
      this.redisConnectionFactory = redisConnectionFactory;
    }

    /**
     * 값이 JSON 으로 직렬화되는 단일 엔티티 타입용 RedisTemplate 을 생성한다.
     * 값을 일반 텍스트 문자열로 저장해야 하는 경우 StringRedisTemplate 을 사용한다.
     *
     * @param objectMapper  엔티티용 Jackson 3 object mapper
     * @param entityType    template 이 처리하는 엔티티 클래스
     * @return 엔티티 타입 전용 template
     */
    public <T> RedisTemplate<String, T> createEntityRedisTemplate(
        ObjectMapper objectMapper,
        Class<T> entityType) {
      RedisTemplate<String, T> template = new RedisTemplate<>();

      // 타입 기반 값 연산을 위해 Spring Cloud Azure 가 구성한 redisConnectionFactory 를 재사용한다.
      template.setConnectionFactory(redisConnectionFactory);

      // key :: Redis 최상위 키
      template.setKeySerializer(new StringRedisSerializer());

      // hash key :: Redis Hash 필드 이름
      template.setHashKeySerializer(new StringRedisSerializer());

      // value :: Redis value 와 Hash value. 둘 다 지정된 엔티티 타입에 바인딩된다.
      RedisSerializer<T> valueSerializer = new JacksonJsonRedisSerializer<>(objectMapper, entityType);
      template.setValueSerializer(valueSerializer);
      template.setHashValueSerializer(valueSerializer);

      template.afterPropertiesSet();
      return template;
    }
  }

} // end of AzureManagedRedisConfiguration
