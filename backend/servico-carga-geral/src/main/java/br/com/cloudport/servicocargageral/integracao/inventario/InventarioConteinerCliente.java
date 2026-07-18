package br.com.cloudport.servicocargageral.integracao.inventario;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Component
@ConditionalOnProperty(
        name = "cloudport.modulo.inventario.integracao",
        havingValue = "http",
        matchIfMissing = true)
public class InventarioConteinerCliente {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public InventarioConteinerCliente(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${cloudport.integracao.inventario.base-url:http://localhost:8081}") String baseUrl) {
        this.restTemplate = restTemplateBuilder.build();
        this.baseUrl = removerBarraFinal(baseUrl);
    }

    public List<ConteinerInventarioResposta> listarElegiveis() {
        ResponseEntity<ConteinerInventarioResposta[]> resposta = restTemplate.getForEntity(
                baseUrl + "/yard/inventario/canonico/reservas-carga-geral/elegiveis",
                ConteinerInventarioResposta[].class);
        ConteinerInventarioResposta[] corpo = resposta.getBody();
        if (corpo == null) {
            return List.of();
        }
        return Arrays.stream(corpo).filter(Objects::nonNull).toList();
    }

    public ConteinerInventarioResposta reservar(String identificacao, UUID operacaoId, String usuario) {
        return restTemplate.postForObject(
                baseUrl + "/yard/inventario/canonico/reservas-carga-geral/{identificacao}",
                new ReservarConteinerRequest(operacaoId, usuario),
                ConteinerInventarioResposta.class,
                identificacao);
    }

    public ConteinerInventarioResposta liberar(
            UUID operacaoId,
            String usuario,
            String motivo,
            String resultado) {
        return restTemplate.postForObject(
                baseUrl + "/yard/inventario/canonico/reservas-carga-geral/{operacaoId}/liberar",
                new LiberarConteinerRequest(usuario, motivo, resultado),
                ConteinerInventarioResposta.class,
                operacaoId);
    }

    private String removerBarraFinal(String valor) {
        if (!StringUtils.hasText(valor)) {
            return "http://localhost:8081";
        }
        return valor.endsWith("/") ? valor.substring(0, valor.length() - 1) : valor;
    }

    public record ReservarConteinerRequest(UUID operacaoId, String usuario) {
    }

    public record LiberarConteinerRequest(String usuario, String motivo, String resultado) {
    }

    public record ConteinerInventarioResposta(
            Long unidadeId,
            String identificacao,
            String estado,
            String condicao,
            String posicaoAtual,
            UUID operacaoId,
            String statusReserva) {
    }
}
