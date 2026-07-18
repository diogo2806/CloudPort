package br.com.cloudport.monolitonavio.integracao;

import br.com.cloudport.contracts.api.ComandoMotivado;
import br.com.cloudport.serviconaviosiderurgico.cliente.OrdemPatioYardCliente.OrdemPatioYardRespostaDTO;
import br.com.cloudport.serviconaviosiderurgico.cliente.OrdemPatioYardComandoCliente;
import br.com.cloudport.serviconaviosiderurgico.dto.ComandoPrioridadeOrdemPatioDTO;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.AtualizacaoPrioridadeOrdemTrabalhoDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.AtualizacaoStatusOrdemTrabalhoDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.ComandoMotivadoDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.StatusOrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.servico.AuditoriaComandoPatioServico;
import br.com.cloudport.servicoyard.patio.listatrabalho.servico.OrdemTrabalhoPatioServico;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
@ConditionalOnProperty(name = "cloudport.modulo.yard.integracao", havingValue = "local")
public class OrdemPatioComandoLocalAdapter extends OrdemPatioYardComandoCliente {

    private final OrdemTrabalhoPatioServico ordemServico;
    private final AuditoriaComandoPatioServico auditoriaServico;
    private final ObjectMapper objectMapper;

    public OrdemPatioComandoLocalAdapter(
            OrdemTrabalhoPatioServico ordemServico,
            AuditoriaComandoPatioServico auditoriaServico,
            ObjectMapper objectMapper) {
        super(new RestTemplateBuilder(), "http://yard-local.invalid");
        this.ordemServico = ordemServico;
        this.auditoriaServico = auditoriaServico;
        this.objectMapper = objectMapper;
    }

    @Override
    public OrdemPatioYardRespostaDTO atualizarPrioridade(
            Long ordemId,
            ComandoPrioridadeOrdemPatioDTO comando) {
        AtualizacaoPrioridadeOrdemTrabalhoDto dto = new AtualizacaoPrioridadeOrdemTrabalhoDto();
        dto.setPrioridadeOperacional(comando.prioridadeOperacional());
        dto.setPrioridadeBusca(comando.prioridadeBuscaEfetiva());
        preencher(dto, comando.motivo(), comando.usuario(), comando.origemAcao(), comando.correlationId());
        Object resposta = ordemServico.atualizarPrioridade(ordemId, dto);
        auditoriaServico.registrar(null, ordemId, "PRIORIDADE_ORDEM_ALTERADA_COM_MOTIVO", dto,
                "Prioridade=" + comando.prioridadeOperacional() + ".");
        return converter(resposta);
    }

    @Override
    public OrdemPatioYardRespostaDTO suspender(Long ordemId, ComandoMotivado comando) {
        ComandoMotivadoDto dto = converterComando(comando);
        Object resposta = ordemServico.suspender(ordemId);
        auditoriaServico.registrar(null, ordemId, "ORDEM_SUSPENSA_COM_MOTIVO", dto, "Ordem suspensa.");
        return converter(resposta);
    }

    @Override
    public OrdemPatioYardRespostaDTO retomar(Long ordemId, ComandoMotivado comando) {
        ComandoMotivadoDto dto = converterComando(comando);
        Object resposta = ordemServico.retomar(ordemId);
        auditoriaServico.registrar(null, ordemId, "ORDEM_RETOMADA_COM_MOTIVO", dto, "Ordem retomada.");
        return converter(resposta);
    }

    @Override
    public OrdemPatioYardRespostaDTO cancelar(Long ordemId, ComandoMotivado comando) {
        AtualizacaoStatusOrdemTrabalhoDto dto = new AtualizacaoStatusOrdemTrabalhoDto();
        dto.setStatusOrdem(StatusOrdemTrabalhoPatio.CANCELADA);
        preencher(dto, comando.motivo(), comando.usuario(), comando.origemAcao(), comando.correlationId());
        Object resposta = ordemServico.atualizarStatus(ordemId, dto);
        auditoriaServico.registrar(null, ordemId, "ORDEM_CANCELADA_COM_MOTIVO", dto,
                "Ordem cancelada administrativamente pelo modulo Navio.");
        return converter(resposta);
    }

    private ComandoMotivadoDto converterComando(ComandoMotivado comando) {
        ComandoMotivadoDto dto = new ComandoMotivadoDto();
        preencher(dto, comando.motivo(), comando.usuario(), comando.origemAcao(), comando.correlationId());
        return dto;
    }

    private void preencher(
            ComandoMotivadoDto dto,
            String motivo,
            String usuario,
            String origemAcao,
            String correlationId) {
        dto.setMotivo(motivo);
        dto.setUsuario(usuario);
        dto.setOrigemAcao(origemAcao);
        dto.setCorrelationId(correlationId);
    }

    private OrdemPatioYardRespostaDTO converter(Object origem) {
        return objectMapper.convertValue(origem, OrdemPatioYardRespostaDTO.class);
    }
}
