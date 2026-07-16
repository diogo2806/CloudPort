package br.com.cloudport.monolitonavio.configuracao;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiMonolitoConfiguracao {

    @Bean
    public OpenAPI cloudportOpenApi() {
        return new OpenAPI().info(new Info()
                .title("CloudPort API")
                .description("Contrato consolidado do monólito modular CloudPort")
                .version("1.0"));
    }
}
