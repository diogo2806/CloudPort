package br.com.cloudport.servicocargageral.integracao.yard;

import br.com.cloudport.servicocargageral.repositorio.AlocacaoCargoLotRepositorio;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Component
@ConditionalOnProperty(
        name = "cloudport.modulo.capacidade-carga-geral.integracao",
        havingValue = "http",
        matchIfMissing = true)
public class CapacidadeCargoLotCliente {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final AlocacaoCargoLotRepositorio alocacaoRepositorio;

    public CapacidadeCargoLotCliente(
            RestTemplateBuilder builder,
            @Value("${cloudport.integracao.yard.base-url:http://localhost:8081}") String baseUrl,
            AlocacaoCargoLotRepositorio alocacaoRepositorio) {
        this.restTemplate = builder.build();
        this.baseUrl = removerBarraFinal(baseUrl);
        this.alocacaoRepositorio = alocacaoRepositorio;
    }

    public ReservaCapacidadeResposta reservar(
            String posicao,
            UUID commandId,
            UUID loteId,
            BigDecimal quantidade,
            BigDecimal volumeM3,
            BigDecimal pesoKg,
            String usuario) {
        return restTemplate.postForObject(
                baseUrl + "/yard/capacidades-cargo-lot/{posicao}/reservas",
                new ReservarCapacidadeRequest(commandId, loteId, quantidade, volumeM3, pesoKg, usuario),
                ReservaCapacidadeResposta.class,
                posicao);
    }

    public ReservaCapacidadeResposta confirmar(UUID reservaId, String usuario, String motivo) {
        return restTemplate.postForObject(
                baseUrl + "/yard/capacidades-cargo-lot/reservas/{reservaId}/confirmar",
                new ComandoCapacidadeRequest(usuario, motivo, buscarPosicaoOrigem(reservaId)),
                ReservaCapacidadeResposta.class,
                reservaId);
    }

    public ReservaCapacidadeResposta cancelar(UUID reservaId, String usuario, String motivo) {
        return restTemplate.postForObject(
                baseUrl + "/yard/capacidades-cargo-lot/reservas/{reservaId}/cancelar",
                new ComandoCapacidadeRequest(usuario, motivo, null),
                ReservaCapacidadeResposta.class,
                reservaId);
    }

    protected String buscarPosicaoOrigem(UUID reservaId) {
        return alocacaoRepositorio.findByReservaCapacidadeId(reservaId)
                .map(alocacao -> alocacao.getOrigem())
                .filter(StringUtils::hasText)
                .orElse(null);
    }

    private String removerBarraFinal(String valor) {
        if (!StringUtils.hasText(valor)) return "http://localhost:8081";
        return valor.endsWith("/") ? valor.substring(0, valor.length() - 1) : valor;
    }

    public record ReservarCapacidadeRequest(
            UUID commandId,
            UUID loteId,
            BigDecimal quantidade,
            BigDecimal volumeM3,
            BigDecimal pesoKg,
            String usuario) {
    }

    public record ComandoCapacidadeRequest(String usuario, String motivo, String posicaoOrigem) {
    }

    public record ReservaCapacidadeResposta(
            UUID id,
            UUID commandId,
            UUID loteId,
            String posicao,
            BigDecimal quantidade,
            BigDecimal volumeM3,
            BigDecimal pesoKg,
            String status,
            String restricoes) {
    }
}
