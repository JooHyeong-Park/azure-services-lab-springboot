/***********************************************************
 * created by     :  JooHyeong.Park
 * creation date  :  2026-09-28
 *
 ***********************************************************/
package azure.services.lab;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * PROJECT     azure-services-lab-springboot
 *
 * @name       AzureServicesLabApplication.java
 * @desc
 * @author     JooHyeong.Park
 * @since      2026-09-28
 */
@SpringBootApplication
public class AzureServicesLabApplication {

  public static void main(String[] args) {
    SpringApplication.run(AzureServicesLabApplication.class, args);
  }

} // end of AzureServicesLabApplication
