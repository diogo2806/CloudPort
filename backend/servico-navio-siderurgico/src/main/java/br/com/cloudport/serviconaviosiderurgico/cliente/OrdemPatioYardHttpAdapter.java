package br.com.cloudport.serviconaviosiderurgico.cliente;

import br.com.cloudport.serviconaviosiderurgico.dominio.ItemOperacaoNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.ReservaPosicaoPatioNavio;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Component
@ConditionalOnProperty(
        name = "cloudport.modulo.yard.integracao",
        havingValue = "http",
        matchIfMissing = true)
public class OrdemPatioYardHttpAdapter implements OrdemPatioYardCliente {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public OrdemPatioYardHttpAdapter(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${cloudport.integracao.yard.base-url:http://localhost:8081}") String baseUrl) {
        this.restTemplate = restTemplateBuilder
                .requestFactory(HttpComponentsClientHttpRequestFactory.class)
                .build();
        this.baseUrl = removerBarraFinal(baseUrl);
    }

    @Override
    public OrdemPatioYardRespostaDTO criarOuReutilizarOrdem(ItemOperacaoNavio item,
                                                              ReservaPosicaoPatioNavio reserva) {
        OrdemPatioYardRequisicaoDTO requisicao = OrdemPatioYardRequisicaoDTO.de(item, reserva);
        return restTemplate.postForObject(
                baseUrl + "/yard/patio/ordens/navio",
                requisicao,
                OrdemPatioYardRespostaDTO.class);
    }

    @Override
    public List<OrdemPatioYardRespostaDTO> listarOrdensDaVisita(Long visitaNavioId) {
        ResponseEntity<OrdemPatioYardRespostaDTO[]> resposta = restTemplate.getForEntity(
                baseUrl + "/yard/patio/ordens/visita-navio/{visitaNavioId}",
                OrdemPatioYardRespostaDTO[].class,
                visitaNavioId);
        return lista(resposta.getBody());
    }

    @Override
    public List<FilaOrdemPatioYardDTO> listarFilasDaVisita(Long visitaNavioId) {
        ResponseEntity<FilaOrdemPatioYardDTO[]> resposta = restTemplate.getForEntity(
                baseUrl + "/yard/patio/ordens/visita-navio/{visitaNavioId}/filas",
                FilaOrdemPatioYardDTO[].class,
                visitaNavioId);
        return lista(resposta.getBody());
    }

    @Override
    public List<WorkQueuePatioYardDTO> listarWorkQueuesDaVisita(Long visitaNavioId) {
        ResponseEntity<WorkQueuePatioYardDTO[]> resposta = restTemplate.getForEntity(
                baseUrl + "/yard/patio/work-queues?visitaNavioId={visitaNavioId}",
                WorkQueuePatioYardDTO[].class,
                visitaNavioId);
        return lista(resposta.getBody());
    }

    @Override
    public List<OrdemPatioYardRespostaDTO> listarOrdensSemCobertura(Long visitaNavioId) {
        ResponseEntity<OrdemPatioYardRespostaDTO[]> resposta = restTemplate.getForEntity(
                baseUrl + "/yard/patio/ordens/visita-navio/{visitaNavioId}/sem-cobertura",
                OrdemPatioYardRespostaDTO[].class,
                visitaNavioId);
        return lista(resposta.getBody());
    }

    @Override
    public OrdemPatioYardRespostaDTO atualizarPrioridade(Long ordemId,
                                                          Integer prioridadeOperacional,
                                                          Boolean prioridadeBusca) {
        AtualizacaoPrioridadeOrdemPatioYardDTO requisicao =
                new AtualizacaoPrioridadeOrdemPatioYardDTO(prioridadeOperacional, prioridadeBusca);
        return restTemplate.patchForObject(
                baseUrl + "/yard/patio/ordens/{ordemId}/prioridade",
                requisicao,
                OrdemPatioYardRespostaDTO.class,
                ordemId);
    }

    @Override
    public OrdemPatioYardRespostaDTO suspender(Long ordemId) {
        return restTemplate.patchForObject(
                baseUrl + "/yard/patio/ordens/{ordemId}/suspender",
                null,
                OrdemPatioYardRespostaDTO.class,
                ordemId);
    }

    @Override
    public OrdemPatioYardRespostaDTO retomar(Long ordemId) {
        return restTemplate.patchForObject(
                baseUrl + "/yard/patio/ordens/{ordemId}/retomar",
                null,
                OrdemPatioYardRespostaDTO.class,
                ordemId);
    }

    private <T> List<T> lista(T[] corpo) {
        if (corpo == null) {
            return List.of();
        }
        return Arrays.stream(corpo).filter(Objects::nonNull).toList();
    }

    private String removerBarraFinal(String valor) {
        if (!StringUtils.hasText(valor)) {
            return "http://localhost:8081";
        }
        return valor.endsWith("/") ? valor.substring(0, valor.length() - 1) : valor;
    }
}
