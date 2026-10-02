/***********************************************************
 * created by     :  JooHyeong.Park
 * creation date  :  2026-09-28
 *
 ***********************************************************/
package azure.services.lab.managedredis;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * PROJECT     azure-services-lab-springboot
 *
 * @name       RedisTestController.java
 * @desc       Azure Managed Redis TEST API 를 노출한다.
 * @author     JooHyeong.Park
 * @since      2026-09-28
 */
@RestController
@RequestMapping("/api/redis")
public class RedisTestController {

  private final AzureManagedRedisStringClient azureManagedRedisStringClient;

  public RedisTestController(AzureManagedRedisStringClient azureManagedRedisStringClient) {
    this.azureManagedRedisStringClient = azureManagedRedisStringClient;
  }

  @GetMapping("/{key}")
  public ResponseEntity<String> find(@PathVariable String key) {
    Optional<String> value = azureManagedRedisStringClient.find(key);
    if (value.isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(value.get());
  }

  @PostMapping("/{key}")
  public ResponseEntity<Void> save(
      @PathVariable String key, @RequestBody String value) {
    azureManagedRedisStringClient.save(key, value);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{key}/delete")
  public ResponseEntity<Void> delete(@PathVariable String key) {
    return azureManagedRedisStringClient.delete(key)
        ? ResponseEntity.noContent().build()
        : ResponseEntity.notFound().build();
  }

  // AzureManagedRedisException 발생시 예외 처리 Handler
  @ExceptionHandler(AzureManagedRedisException.class)
  public ResponseEntity<String> handleAzureManagedRedisException(
      AzureManagedRedisException exception) {
    HttpStatus status = switch (exception.getRedisErrorType()) {
      case NOPERM -> HttpStatus.FORBIDDEN;
      case NOAUTH, WRONGPASS -> HttpStatus.UNAUTHORIZED;
      case CONNECTION_INITIALIZATION_TIMEOUT -> HttpStatus.GATEWAY_TIMEOUT;
      case DNS_RESOLUTION_FAILED -> HttpStatus.SERVICE_UNAVAILABLE;
      case CONNECTION_FAILED -> HttpStatus.SERVICE_UNAVAILABLE;
      case COMMAND_FAILED, UNCLASSIFIED -> HttpStatus.INTERNAL_SERVER_ERROR;
    };
    return ResponseEntity.status(status).body(exception.getMessage());
  }

} // end of RedisTestController
