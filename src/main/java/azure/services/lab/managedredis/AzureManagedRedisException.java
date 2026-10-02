/***********************************************************
 * created by     :  JooHyeong.Park
 * creation date  :  2026-09-28
 *
 ***********************************************************/
package azure.services.lab.managedredis;

import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.Set;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;

import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.RedisCommandTimeoutException;

/**
 * PROJECT     azure-services-lab-springboot
 *
 * @name       AzureManagedRedisException.java
 * @desc       Azure Managed Redis 관련 에러 Case 들를 정의한다.
 *             Spring Data Redis 및 Lettuce 관련 Exception 을 웹 애플리케이션, 배치 작업, 스케줄러,
 *             메시지 리스너 등에서 인식할 수 있는 안정적인 error type 으로 변환한다.
 * @author     JooHyeong.Park
 * @since      2026-09-28
 */
public class AzureManagedRedisException extends RuntimeException {

  /*
   * RedisErrorType :: START
  */
  public enum RedisErrorType {

    // Azure Managed Redis 연동시 발생할 수 있는 Error Case 들을 저수준 Exception 타입과 에러 메시지 prefix 로 분류한다.

    /*
     * Redis error type declarations :: START
     */

    // io.lettuce.core.RedisCommandExecutionException: NOPERM No permissions to access a key
    NOPERM(RedisCommandExecutionException.class, "NOPERM "),

    // io.lettuce.core.RedisCommandExecutionException: NOAUTH HELLO must be called with the client already authenticated;
    // HELLO AUTH <user> <pass> can authenticate and select the protocol.
    NOAUTH(RedisCommandExecutionException.class, "NOAUTH "),

    // io.lettuce.core.RedisCommandExecutionException: WRONGPASS invalid username-password pair
    WRONGPASS(RedisCommandExecutionException.class, "WRONGPASS "),

    // RedisCommandExecutionException 이 발생했지만, 해당 서버 에러 코드가 위에서 명시적으로 분류되지 않은 경우
    COMMAND_FAILED(RedisCommandExecutionException.class),

    // Lettuce 연결 초기화 타임아웃; 다른 명령 타임아웃 메시지는 분류되지 않은 채로 남는다.
    CONNECTION_INITIALIZATION_TIMEOUT(
        RedisCommandTimeoutException.class,
        "Connection initialization timed out"),

    // Redis hostname 을 DNS 로 해석할 수 없음.
    DNS_RESOLUTION_FAILED(UnknownHostException.class),

    // java.net.ConnectException: Connection refused 를 감싸는 RedisConnectionFailureException
    CONNECTION_FAILED(RedisConnectionFailureException.class),

    // 알려진 Redis 또는 Spring Data Redis error type 에 해당하지 않는 Error.
    UNCLASSIFIED;

    /*
     * Redis error type declarations :: END
     */

    // 이 Redis error type 이 나타내는 저수준 Exception Class.
    private final Class<? extends Throwable> exceptionType;

    // 선택적인 Redis 서버 메시지 prefix.
    // null 값은 Exception Class 만으로 분류가 충분함을 의미한다.
    private final String redisMessage;

    // 등록된 Exception Class 타입이 일치하지 않을 때 사용되는 fallback 타입을 생성한다.
    RedisErrorType() {
      this(null, null);
    }

    /**
     * Exception Class 만으로 식별할 수 있는 error type 을 생성한다.
     *
     * @param exceptionType  이 error type 이 나타내는 저수준 Exception Class
     */
    RedisErrorType(Class<? extends Throwable> exceptionType) {
      this(exceptionType, null);
    }

    /**
     * Exception Class 와 Redis 메시지 prefix 가 필요한 error type 을 생성한다.
     *
     * @param exceptionType  이 error type 이 나타내는 저수준 Exception Class
     * @param redisMessage   분류에 필요한 Redis 서버 메시지 prefix
     */
    RedisErrorType(
        Class<? extends Throwable> exceptionType,
        String redisMessage) {
      this.exceptionType = exceptionType;
      this.redisMessage = redisMessage;
    }

    /**
     * 원본 Exception 부터 Spring wrapper 방향으로 cause chain 을 검색한다.
     *
     * @param exception        분류할 Spring Data Redis Exception
     * @return RedisErrorType  일치하는 error type. 일치하는 타입이 없으면 UNCLASSIFIED
     */
    private static RedisErrorType from(DataAccessException exception) {
      Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
      RedisErrorType errorType = findFromCause(exception, visited);
      return errorType != null ? errorType : UNCLASSIFIED;
    }

    /**
     * 명령 오류가 connection wrapper 에 가려지지 않도록 가장 깊은 cause 부터 cause chain 을 탐색한다.
     *
     * @param throwable        cause chain 의 현재 Exception
     * @param visited          identity 로 이미 검사된 Exception 들
     * @return RedisErrorType  일치하는 error type.
     *                         일치하는 cause 가 없으면 null; 호출자가 null 을 UNCLASSIFIED 로 변환한다
     */
    private static RedisErrorType findFromCause(
        Throwable throwable,
        Set<Throwable> visited) {
      if (!visited.add(throwable)) {
        return null;
      }

      Throwable cause = throwable.getCause();
      if (cause != null && cause != throwable) {
        RedisErrorType causeErrorType = findFromCause(cause, visited);
        if (causeErrorType != null) {
          return causeErrorType;
        }
      }

      return Arrays.stream(values())
          .filter(errorType -> errorType.matches(throwable))
          .findFirst()
          .orElse(null);
    }

    /**
     * 현재 cause 만 매칭한다. cause 탐색은 findFromCause 가 처리한다.
     *
     * @param throwable  검사할 현재 cause
     * @return boolean   Exception Class 와 선택적 메시지 prefix 가 일치하는지 여부
     */
    private boolean matches(Throwable throwable) {
      return exceptionType != null
          && exceptionType.isInstance(throwable)
          && matchesWithMessage(throwable.getMessage());
    }

    /**
     * Exception Class 가 일치한 후 선택적인 메시지 조건을 적용한다.
     *
     * @param exceptionMessage  현재 Exception 의 메시지
     * @return boolean          메시지가 필요 없거나, 메시지가 redisMessage 로 시작하는지 여부
     */
    private boolean matchesWithMessage(String exceptionMessage) {
      if (redisMessage == null) {
        return true;
      }
      return exceptionMessage != null && exceptionMessage.strip().startsWith(redisMessage);
    }

  } // end of RedisErrorType

  /*
   * RedisErrorType :: END
   */

  /*
   * AzureManagedRedisException :: START
   */

  // 호출 애플리케이션에 노출되는 안정적인 Redis error type.
  private final RedisErrorType redisErrorType;

  // 진단을 위해 보존된 가장 구체적인 원본 오류 메시지.
  private final String originalErrorMessage;

  /**
   * 원본 Exception 을 cause 로 유지하면서 AzureManagedRedisException 으로 변환 한다.
   *
   * @param redisErrorType  분류된 Redis 오류
   * @param cause           원본 Exception
   */
  public AzureManagedRedisException(RedisErrorType redisErrorType, Throwable cause) {
    this(redisErrorType, cause, findOriginalErrorMessage(cause));
  }

  /**
   * 원본 메시지가 추출된 후 AzureManagedRedisException 을 생성한다.
   *
   * @param redisErrorType        분류된 Redis 오류
   * @param cause                 원본 Exception
   * @param originalErrorMessage  cause chain 에서 가장 구체적인 메시지
   */
  private AzureManagedRedisException(
      RedisErrorType redisErrorType,
      Throwable cause,
      String originalErrorMessage) {
    super(
        createMessage(
            Objects.requireNonNull(redisErrorType, "redisErrorType"),
            originalErrorMessage),
        cause);
    this.redisErrorType = redisErrorType;
    this.originalErrorMessage = originalErrorMessage;
  }

  /**
   * 호출자가 재시도 또는 Exception 처리 결정에 사용할 수 있는 안정적인 error type 을 반환한다.
   *
   * @return RedisErrorType  Redis error type
   */
  public RedisErrorType getRedisErrorType() {
    return redisErrorType;
  }

  /**
   * 원본 Exception cause chain 에서 가장 구체적인 메시지를 반환한다.
   *
   * @return string  오류 메시지. cause 가 메시지를 포함하지 않으면 null
   */
  public String getOriginalErrorMessage() {
    return originalErrorMessage;
  }

  /**
   * HTTP 관련 동작을 노출하지 않고 Spring Data Redis Exception 를 변환한다.
   *
   * @param exception                    변환할 Spring Data Redis Exception
   * @return AzureManagedRedisException  애플리케이션 레벨의 Azure Managed Redis Exception
   */
  static AzureManagedRedisException from(DataAccessException exception) {
    return new AzureManagedRedisException(RedisErrorType.from(exception), exception);
  }

  /**
   * 안정적인 애플리케이션 메시지를 생성하고 진단을 위해 원본 오류 메시지를 추가한다.
   *
   * @param redisErrorType        분류된 Redis 오류
   * @param originalErrorMessage  가장 구체적인 원본 오류 메시지
   * @return string               예외 메시지를 담고 있는 문자열
   */
  private static String createMessage(
      RedisErrorType redisErrorType, String originalErrorMessage) {
    StringBuilder message = new StringBuilder("[ERROR] Azure Managed Redis operation failed :: ");
    message.append(redisErrorType);
    if (originalErrorMessage == null || originalErrorMessage.isBlank()) {
      return message.toString();
    }
    return message.append(" - ").append(originalErrorMessage).toString();
  }

  /**
   * Exception cause chain 에서 가장 깊은 비어 있지 않은 메시지를 찾는다.
   *
   * @param throwable  cause 가 검사되는 루트 Exception
   * @return string    사용 가능한 오류 메시지. 없으면 null
   */
  private static String findOriginalErrorMessage(Throwable throwable) {
    String originalErrorMessage = null;
    Throwable current = throwable;
    Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
    while (current != null && visited.add(current)) {
      if (current.getMessage() != null && !current.getMessage().isBlank()) {
        originalErrorMessage = current.getMessage();
      }
      current = current.getCause();
    }
    return originalErrorMessage;
  }

  /*
   * AzureManagedRedisException :: END
   */

} // end of AzureManagedRedisException
