package br.com.cloudport.servicogate.integration.cargageral;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Component
@ConditionalOnProperty(
        name = "cloudport.modulo.gate-carga-geral.integracao",
        havingValue = "http",
        matchIfMissing = true)
public class CargaGeralGateCliente {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public CargaGeralGateCliente(
            RestTemplateBuilder builder,
            @Value("${cloudport.integracao.carga-geral.base-url:http://localhost:8086}") String baseUrl) {
        this.restTemplate = builder.build();
        this.baseUrl = removerBarraFinal(baseUrl);
    }

    public void confirmar(UUID reservaId, UUID commandId, String estagio, String usuario) {
        if (reservaId == null) {
            return;
        }
        restTemplate.postForObject(
                baseUrl + "/api/carga-geral/operacoes-intermodais/gate/reservas/{reservaId}/confirmar",
                new ConfirmarGateCargaRequest(commandId, estagio, usuario),
                Object.class,
                reservaId);
    }

    private String removerBarraFinal(String valor) {
        if (!StringUtils.hasText(valor)) {
            return "http://localhost:8086";
        }
        return valor.endsWith("/") ? valor.substring(0, valor.length() - 1) : valor;
    }

    public record ConfirmarGateCargaRequest(UUID commandId, String estagio, String usuario) {
    }
}
