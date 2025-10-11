package br.com.cloudport.servicogate.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@Configuration
@EnableConfigurationProperties({
        AgendamentoRulesProperties.class,
        DocumentoStorageProperties.class,
        GateFlowProperties.class,
        HardwareIntegrationProperties.class
})
public class OpenApiConfig {

    @Bean
    public OpenAPI gateOpenApi() {
        return new OpenAPI()
                .components(new Components())
                .info(new Info()
                        .title("CloudPort - Serviço de Gate")
                        .description("API para gestão de agendamentos e janelas de atendimento do Gate CloudPort")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Equipe CloudPort")
                                .email("suporte@cloudport.com.br"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://cloudport.com.br")));
    }
}
