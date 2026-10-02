/***********************************************************
 * created by     :  JooHyeong.Park
 * creation date  :  2026-09-28
 *
 ***********************************************************/
package azure.services.lab.managedredis;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.data.redis.autoconfigure.LettuceClientConfigurationBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.lettuce.core.SslVerifyMode;

/**
 * PROJECT     azure-services-lab-springboot
 *
 * @name       AzureManagedRedisLettuceCustomizer.java
 * @desc       Azure Managed Redis Lettuce Client 의 연결 구성을 커스터마이징한다.
 * @author     JooHyeong.Park
 * @since      2026-09-28
 */
@Configuration(proxyBeanMethods = false)
public class AzureManagedRedisLettuceCustomizer {

  @Bean
  public LettuceClientConfigurationBuilderCustomizer azureManagedRedisLettuceCustomizer(
      @Value("${spring.data.redis.ssl.enabled:false}") boolean sslEnabled) {
    return builder -> {
      /*
       * [ SSL (TLS) 검증 모드 변경 ]
       * - Azure Managed Redis 의 HTTPS 인증서는 Azure Managed Redis 공용 FQDN 전용으로 발급되어,
       *   Custom Domain 기반으로 접속시 `SslVerifyMode.FULL` (기본값) 에 의해 HostName 검증에서 실패할 수 있음
       * - 이에 `spring.data.redis.ssl.enabled=true` 지정시 `SslVerifyMode.CA` (인증서 CA 검증만 수행, HostName 검증 생략)
       *   적용되도록 별도 설정함
       */
      if (sslEnabled) {
        builder.useSsl()
            .verifyPeer(SslVerifyMode.CA)
            .and();
      }
    };
  }

} // end of AzureManagedRedisLettuceCustomizer
