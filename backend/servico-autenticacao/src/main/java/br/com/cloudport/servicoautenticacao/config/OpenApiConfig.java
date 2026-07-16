package br.com.cloudport.servicoautenticacao.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    @ConditionalOnExpression("'${spring.application.name:servico-autenticacao}' != 'cloudport-runtime'")
    public OpenAPI autenticacaoOpenApi() {
        return new OpenAPI()
                .components(new Components())
                .info(new Info()
                        .title("CloudPort - Servico de Autenticacao")
                        .description("API para autenticacao, autorizacao e configuracoes de seguranca do CloudPort")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Equipe CloudPort")
                                .email("suporte@cloudport.com.br"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://cloudport.com.br")));
    }
}
