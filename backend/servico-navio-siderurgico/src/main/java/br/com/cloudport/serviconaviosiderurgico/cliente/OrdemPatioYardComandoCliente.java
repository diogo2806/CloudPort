package br.com.cloudport.serviconaviosiderurgico.cliente;

import br.com.cloudport.contracts.api.ComandoMotivado;
import br.com.cloudport.serviconaviosiderurgico.cliente.OrdemPatioYardCliente.OrdemPatioYardRespostaDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.ComandoPrioridadeOrdemPatioDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Component
public class OrdemPatioYardComandoCliente {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public OrdemPatioYardComandoCliente(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${cloudport.integracao.yard.base-url:http://localhost:8081}") String baseUrl) {
        this.restTemplate = restTemplateBuilder
                .requestFactory(HttpComponentsClientHttpRequestFactory.class)
                .build();
        this.baseUrl = removerBarraFinal(baseUrl);
    }

    public OrdemPatioYardRespostaDTO atualizarPrioridade(Long ordemId, ComandoPrioridadeOrdemPatioDTO comando) {
        AtualizacaoPrioridadeRequisicao requisicao = new AtualizacaoPrioridadeRequisicao(
                comando.prioridadeOperacional(),
                comando.prioridadeBuscaEfetiva(),
                comando.motivo(),
                comando.usuario(),
                comando.origemAcao(),
                comando.correlationId()
        );
        return restTemplate.patchForObject(
                baseUrl + "/yard/patio/ordens/{ordemId}/prioridade",
                requisicao,
                OrdemPatioYardRespostaDTO.class,
                ordemId
        );
    }

    public OrdemPatioYardRespostaDTO suspender(Long ordemId, ComandoMotivado comando) {
        return restTemplate.patchForObject(
                baseUrl + "/yard/patio/ordens/{ordemId}/suspender",
                comando,
                OrdemPatioYardRespostaDTO.class,
                ordemId
        );
    }

    public OrdemPatioYardRespostaDTO retomar(Long ordemId, ComandoMotivado comando) {
        return restTemplate.patchForObject(
                baseUrl + "/yard/patio/ordens/{ordemId}/retomar",
                comando,
                OrdemPatioYardRespostaDTO.class,
                ordemId
        );
    }

    public OrdemPatioYardRespostaDTO cancelar(Long ordemId, ComandoMotivado comando) {
        return restTemplate.patchForObject(
                baseUrl + "/yard/patio/ordens/{ordemId}/cancelar",
                comando,
                OrdemPatioYardRespostaDTO.class,
                ordemId
        );
    }

    private String removerBarraFinal(String valor) {
        if (!StringUtils.hasText(valor)) {
            return "http://localhost:8081";
        }
        return valor.endsWith("/") ? valor.substring(0, valor.length() - 1) : valor;
    }

    private record AtualizacaoPrioridadeRequisicao(
            Integer prioridadeOperacional,
            Boolean prioridadeBusca,
            String motivo,
            String usuario,
            String origemAcao,
            String correlationId
    ) {
    }
}
