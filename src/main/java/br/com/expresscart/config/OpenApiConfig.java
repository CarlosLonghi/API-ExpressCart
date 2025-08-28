package br.com.expresscart.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI expressCartOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Express Cart API")
                        .version("1.0")
                        .description("Um serviço de gerenciamento de carrinho de compras simples e eficiente, desenvolvido com Java, Redis, MongoDB e Docker.")
                        .contact(new Contact()
                                .name("Carlos Longhi")
                                .email("carloslonghi.cl@gmail.com")
                                .url("https://carloslonghi.com.br"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("http://springdoc.org")))
                        .externalDocs(new ExternalDocumentation()
                                .description("Repositório no GitHub")
                                .url("https://github.com/CarlosLonghi/API-ExpressCart")
                );
    }
}
