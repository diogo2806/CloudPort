package br.com.cloudport.runtime.integracao;

import br.com.cloudport.serviconaviosiderurgico.cliente.OrdemPatioYardCliente;
import br.com.cloudport.serviconaviosiderurgico.dominio.ItemOperacaoNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.ReservaPosicaoPatioNavio;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.AtualizacaoPrioridadeOrdemTrabalhoDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.OrdemTrabalhoPatioRequisicaoDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.servico.OrdemTrabalhoPatioServico;
import br.com.cloudport.servicoyard.patio.listatrabalho.servico.WorkQueuePatioServico;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "cloudport.modulo.yard.integracao", havingValue = "local")
public class OrdemPatioYardLocalAdapter extends OrdemPatioYardCliente {

    private final OrdemTrabalhoPatioServico ordemServico;
    private final WorkQueuePatioServico workQueueServico;
    private final ObjectMapper objectMapper;

    public OrdemPatioYardLocalAdapter(
            OrdemTrabalhoPatioServico ordemServico,
            WorkQueuePatioServico workQueueServico,
            ObjectMapper objectMapper) {
        super(new RestTemplateBuilder(), "http://yard-local.invalid");
        this.ordemServico = ordemServico;
        this.workQueueServico = workQueueServico;
        this.objectMapper = objectMapper;
    }

    @Override
    public OrdemPatioYardRespostaDTO criarOuReutilizarOrdem(
            ItemOperacaoNavio item,
            ReservaPosicaoPatioNavio reserva) {
        OrdemPatioYardRequisicaoDTO origem = OrdemPatioYardRequisicaoDTO.de(item, reserva);
        OrdemTrabalhoPatioRequisicaoDto requisicao = objectMapper.convertValue(
                origem,
                OrdemTrabalhoPatioRequisicaoDto.class);
        return converter(ordemServico.registrarOuReutilizarOrdemNavio(requisicao), OrdemPatioYardRespostaDTO.class);
    }

    @Override
    public List<OrdemPatioYardRespostaDTO> listarOrdensDaVisita(Long visitaNavioId) {
        return converterLista(
                ordemServico.listarOrdensPorVisitaNavio(visitaNavioId),
                OrdemPatioYardRespostaDTO.class);
    }

    @Override
    public List<FilaOrdemPatioYardDTO> listarFilasDaVisita(Long visitaNavioId) {
        return converterLista(
                ordemServico.listarFilasPorVisitaNavio(visitaNavioId),
                FilaOrdemPatioYardDTO.class);
    }

    @Override
    public List<WorkQueuePatioYardDTO> listarWorkQueuesDaVisita(Long visitaNavioId) {
        return converterLista(
                workQueueServico.listar(visitaNavioId),
                WorkQueuePatioYardDTO.class);
    }

    @Override
    public List<OrdemPatioYardRespostaDTO> listarOrdensSemCobertura(Long visitaNavioId) {
        return converterLista(
                ordemServico.listarOrdensSemCobertura(visitaNavioId),
                OrdemPatioYardRespostaDTO.class);
    }

    @Override
    public OrdemPatioYardRespostaDTO atualizarPrioridade(
            Long ordemId,
            Integer prioridadeOperacional,
            Boolean prioridadeBusca) {
        AtualizacaoPrioridadeOrdemTrabalhoDto comando = new AtualizacaoPrioridadeOrdemTrabalhoDto();
        comando.setPrioridadeOperacional(prioridadeOperacional);
        comando.setPrioridadeBusca(prioridadeBusca);
        return converter(ordemServico.atualizarPrioridade(ordemId, comando), OrdemPatioYardRespostaDTO.class);
    }

    @Override
    public OrdemPatioYardRespostaDTO suspender(Long ordemId) {
        return converter(ordemServico.suspender(ordemId), OrdemPatioYardRespostaDTO.class);
    }

    @Override
    public OrdemPatioYardRespostaDTO retomar(Long ordemId) {
        return converter(ordemServico.retomar(ordemId), OrdemPatioYardRespostaDTO.class);
    }

    private <T> T converter(Object origem, Class<T> destino) {
        return objectMapper.convertValue(origem, destino);
    }

    private <T> List<T> converterLista(Object origem, Class<T> destino) {
        JavaType tipo = objectMapper.getTypeFactory().constructCollectionType(List.class, destino);
        return objectMapper.convertValue(origem, tipo);
    }
}
