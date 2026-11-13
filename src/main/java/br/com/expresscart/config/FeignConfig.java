package br.com.expresscart.config;

import br.com.expresscart.client.PlatziStoreClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

/**
 * Registra os clientes Feign. Fica fora da classe principal para que os testes de slice
 * ({@code @WebMvcTest}) não tentem criar os clientes HTTP.
 */
@Configuration
@EnableFeignClients(basePackageClasses = PlatziStoreClient.class)
public class FeignConfig {
}
