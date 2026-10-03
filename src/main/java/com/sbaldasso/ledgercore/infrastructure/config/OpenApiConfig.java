package com.sbaldasso.ledgercore.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ledgerCoreOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Ledger Core API - Banking Double-Entry Engine")
                        .description("API REST para gerenciamento de contas bancárias e transferências financeiras " +
                                "com partidas dobradas (Double-Entry Bookkeeping), prevenção de deadlock, " +
                                "idempotência e conformidade com RFC 7807.")
                        .version("0.1.0")
                        .contact(new Contact()
                                .name("Samuel Baldasso"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}
