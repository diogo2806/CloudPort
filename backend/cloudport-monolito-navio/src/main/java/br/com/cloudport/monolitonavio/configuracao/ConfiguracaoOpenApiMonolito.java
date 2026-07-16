package br.com.cloudport.monolitonavio.configuracao;

import br.com.cloudport.serviconaviosiderurgico.configuracao.PublicApiClientAuthenticationFilter;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.springdoc.core.customizers.OpenApiCustomiser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfiguracaoOpenApiMonolito {

    @Bean
    public OpenAPI cloudPortOpenApi() {
        Components components = new Components()
                .addSecuritySchemes("bearerJwt", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"))
                .addSecuritySchemes("publicClientId", new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name(PublicApiClientAuthenticationFilter.HEADER_CLIENT_ID))
                .addSecuritySchemes("publicClientSecret", new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name(PublicApiClientAuthenticationFilter.HEADER_CLIENT_SECRET));
        return new OpenAPI()
                .info(new Info()
                        .title("CloudPort Navio API")
                        .description("Contrato consolidado dos modulos Navio e Navio Siderurgico.")
                        .version("1.0.0"))
                .components(components);
    }

    @Bean
    public OpenApiCustomiser operationIdUnicoCustomiser() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }
            Set<String> usados = new HashSet<>();
            openApi.getPaths().forEach((path, pathItem) -> pathItem.readOperationsMap().forEach((metodo, operacao) -> {
                String base = operacao.getOperationId();
                if (base == null || base.isBlank()) {
                    base = metodo.name().toLowerCase(Locale.ROOT) + normalizarPath(path);
                }
                String operationId = base;
                if (!usados.add(operationId)) {
                    operationId = base + "_" + metodo.name().toLowerCase(Locale.ROOT) + normalizarPath(path);
                    int sufixo = 2;
                    while (!usados.add(operationId)) {
                        operationId = base + "_" + sufixo++;
                    }
                }
                operacao.setOperationId(operationId);
            }));
        };
    }

    private String normalizarPath(String path) {
        return path.replaceAll("[^A-Za-z0-9]", "_").replaceAll("_+", "_");
    }
}
