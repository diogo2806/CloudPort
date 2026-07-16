package br.com.cloudport.runtime.configuracao;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguracaoRuntime {

    @Bean
    public OpenAPI cloudPortOpenApi() {
        return new OpenAPI()
                .components(new Components())
                .info(new Info()
                        .title("CloudPort - API consolidada")
                        .description("Contratos dos módulos Autenticação, Gate, Rail, Visibilidade, Yard, Navio e Navio Siderúrgico")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Equipe CloudPort")
                                .email("suporte@cloudport.com.br"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://cloudport.com.br")));
    }
}
