/***********************************************************
 * created by     :  JooHyeong.Park
 * creation date  :  2026-09-28
 *
 ***********************************************************/
package azure.services.lab.managedredis;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * PROJECT     azure-services-lab-springboot
 *
 * @name       AzureManagedRedisStringClient.java
 * @desc       default-duration, key-prefix 적용된 기본적인 StringRedisTemplate 기반 연동 예제를 구현함
 *             Spring Data Redis 예외는 애플리케이션 레벨의 AzureManagedRedisException 으로 변환됨
 * @author     JooHyeong.Park
 * @since      2026-09-28
 */
@Service
public class AzureManagedRedisStringClient {

  // String 기반 Redis template
  private final StringRedisTemplate stringRedisTemplate;

  // `save(key, value)` 와 같이 duration 미지정시 적용되는 기본 만료 시간
  @Value("${application.azure.managed-redis.default-duration}")
  private Duration defaultDuration;

  // Redis ACL 에 지정된 key pattern 과 일치하도록 모든 키 앞에 자동으로 붙여야 하는 key prefix
  @Value("${application.azure.managed-redis.key-prefix}")
  private String keyPrefix;

  /**
   * StringRedisTemplate 기반 Azure Managed Redis client 를 생성한다.
   *
   * @param stringRedisTemplate  Redis string 연산을 실행하는 데 사용되는 template
   */
  public AzureManagedRedisStringClient(StringRedisTemplate stringRedisTemplate) {
    this.stringRedisTemplate = stringRedisTemplate;
  }

  /**
   * 키로 문자열 값을 조회한다.
   *
   * @param key                조회할 Redis key -> prefixed 메서드 적용
   * @return Optional<String>  키와 연결된 값. 키가 존재하지 않으면 empty
   */
  public Optional<String> find(String key) {
    return executeRedisOperation(
        () -> Optional.ofNullable(stringRedisTemplate.opsForValue().get(prefixed(key))));
  }

  /**
   * defaultDuration 을 적용하여 문자열 값을 저장한다.
   *
   * @param key    저장할 Redis key -> prefixed 메서드 적용
   * @param value  저장할 문자열 값
   */
  public void save(String key, String value) {
    save(key, value, defaultDuration);
  }

  /**
   * 요청된 만료 시간으로 문자열 값을 저장한다. duration 이 0 이면 영구적으로 저장한다.
   *
   * @param key       저장할 Redis key -> prefixed 메서드 적용
   * @param value     저장할 문자열 값
   * @param duration  값에 적용할 만료 시간
   * @throws IllegalArgumentException  duration 이 음수인 경우
   */
  public void save(String key, String value, Duration duration) {
    if (duration.isNegative()) {
      throw new IllegalArgumentException("Redis value duration cannot be negative");
    }
    if (duration.isZero()) {
      savePermanently(key, value);
      return;
    }
    executeRedisOperation(() -> stringRedisTemplate.opsForValue().set(prefixed(key), value, duration));
  }

  /**
   * 만료 시간 없이 영구적으로 문자열 값을 저장한다.
   *
   * @param key    저장할 Redis key -> prefixed 메서드 적용
   * @param value  저장할 문자열 값
   */
  public void savePermanently(String key, String value) {
    executeRedisOperation(() -> stringRedisTemplate.opsForValue().set(prefixed(key), value));
  }

  /**
   * 키로 값을 삭제한다.
   *
   * @param key       삭제할 Redis key -> prefixed 메서드 적용
   * @return boolean  키가 존재하여 삭제되었는지 여부
   */
  public boolean delete(String key) {
    return executeRedisOperation(() -> Boolean.TRUE.equals(stringRedisTemplate.delete(prefixed(key))));
  }

  /**
   * Redis ACL 에 지정된 key pattern 과 일치하도록 논리 키 앞에 keyPrefix 를 추가한다.
   * 단, 이미 keyPrefix 로 시작하는 경우 원본 key 를 그대로 반환 (중복으로 keyPrefix 적용 방지)
   *
   * @param key  애플리케이션 논리 키
   * @return string  keyPrefix 가 적용된 Redis 키
   * @throws IllegalArgumentException  key 가 null 또는 공백(blank) 인 경우
   */
  private String prefixed(String key) {
    if (key == null || key.isBlank()) {
      throw new IllegalArgumentException("Redis key cannot be null or blank");
    }
    // 이미 keyPrefix 로 시작하는 경우 원본 key 를 그대로 반환
    return key.startsWith(keyPrefix) ? key : keyPrefix + key;
  }

  /**
   * 값을 반환하는 Redis 연산을 실행하고 실패시 AzureManagedRedisException 으로 변환한다.
   *
   * @param operation  실행할 Redis 연산
   * @return T         Redis 연산이 반환한 값
   */
  private <T> T executeRedisOperation(Supplier<T> operation) {
    try {
      return operation.get();
    } catch (DataAccessException exception) {
      throw AzureManagedRedisException.from(exception);
    }
  }

  /**
   * 반환 값이 없는 Redis 연산을 실행하고 실패시 AzureManagedRedisException 으로 변환한다.
   *
   * @param operation  실행할 Redis 연산
   */
  private void executeRedisOperation(Runnable operation) {
    try {
      operation.run();
    } catch (DataAccessException exception) {
      throw AzureManagedRedisException.from(exception);
    }
  }

} // end of AzureManagedRedisStringClient
