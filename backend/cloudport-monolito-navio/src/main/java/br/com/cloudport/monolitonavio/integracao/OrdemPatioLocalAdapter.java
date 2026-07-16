package br.com.cloudport.monolitonavio.integracao;

import br.com.cloudport.serviconaviosiderurgico.cliente.OrdemPatioYardCliente;
import br.com.cloudport.serviconaviosiderurgico.dominio.ItemOperacaoNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.ReservaPosicaoPatioNavio;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.AtualizacaoPrioridadeOrdemTrabalhoDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.FilaOrdemTrabalhoPatioDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.OrdemTrabalhoPatioRequisicaoDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.OrdemTrabalhoPatioRespostaDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.WorkQueuePatioRespostaDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.servico.OrdemTrabalhoPatioServico;
import br.com.cloudport.servicoyard.patio.listatrabalho.servico.WorkQueuePatioServico;
import br.com.cloudport.servicoyard.patio.modelo.StatusConteiner;
import br.com.cloudport.servicoyard.patio.modelo.TipoMovimentoPatio;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "cloudport.modulo.yard.integracao", havingValue = "local")
public class OrdemPatioLocalAdapter implements OrdemPatioYardCliente {

    private final OrdemTrabalhoPatioServico ordemServico;
    private final WorkQueuePatioServico workQueueServico;

    public OrdemPatioLocalAdapter(OrdemTrabalhoPatioServico ordemServico,
                                  WorkQueuePatioServico workQueueServico) {
        this.ordemServico = ordemServico;
        this.workQueueServico = workQueueServico;
    }

    @Override
    public OrdemPatioYardRespostaDTO criarOuReutilizarOrdem(ItemOperacaoNavio item,
                                                              ReservaPosicaoPatioNavio reserva) {
        OrdemPatioYardRequisicaoDTO contrato = OrdemPatioYardRequisicaoDTO.de(item, reserva);
        OrdemTrabalhoPatioRequisicaoDto requisicao = new OrdemTrabalhoPatioRequisicaoDto();
        requisicao.setCodigoConteiner(contrato.getCodigoConteiner());
        requisicao.setTipoCarga(contrato.getTipoCarga());
        requisicao.setDestino(contrato.getDestino());
        requisicao.setLinhaDestino(contrato.getLinhaDestino());
        requisicao.setColunaDestino(contrato.getColunaDestino());
        requisicao.setCamadaDestino(contrato.getCamadaDestino());
        requisicao.setTipoMovimento(TipoMovimentoPatio.valueOf(contrato.getTipoMovimento()));
        requisicao.setStatusConteinerDestino(StatusConteiner.valueOf(contrato.getStatusConteinerDestino()));
        requisicao.setVisitaNavioId(contrato.getVisitaNavioId());
        requisicao.setItemOperacaoNavioId(contrato.getItemOperacaoNavioId());
        requisicao.setPlanoEstivaNavioId(contrato.getPlanoEstivaNavioId());
        requisicao.setTipoOrigem(contrato.getTipoOrigem());
        requisicao.setTipoDestino(contrato.getTipoDestino());
        requisicao.setSequenciaNavio(contrato.getSequenciaNavio());
        requisicao.setPrioridadeOperacional(contrato.getPrioridadeOperacional());
        return converter(ordemServico.registrarOuReutilizarOrdemNavio(requisicao));
    }

    @Override
    public List<OrdemPatioYardRespostaDTO> listarOrdensDaVisita(Long visitaNavioId) {
        return ordemServico.listarOrdensPorVisitaNavio(visitaNavioId).stream()
                .map(this::converter)
                .toList();
    }

    @Override
    public List<FilaOrdemPatioYardDTO> listarFilasDaVisita(Long visitaNavioId) {
        return ordemServico.listarFilasPorVisitaNavio(visitaNavioId).stream()
                .map(this::converter)
                .toList();
    }

    @Override
    public List<WorkQueuePatioYardDTO> listarWorkQueuesDaVisita(Long visitaNavioId) {
        return workQueueServico.listar(visitaNavioId).stream()
                .map(this::converter)
                .toList();
    }

    @Override
    public List<OrdemPatioYardRespostaDTO> listarOrdensSemCobertura(Long visitaNavioId) {
        return ordemServico.listarOrdensSemCobertura(visitaNavioId).stream()
                .map(this::converter)
                .toList();
    }

    @Override
    public OrdemPatioYardRespostaDTO atualizarPrioridade(Long ordemId,
                                                          Integer prioridadeOperacional,
                                                          Boolean prioridadeBusca) {
        AtualizacaoPrioridadeOrdemTrabalhoDto dto = new AtualizacaoPrioridadeOrdemTrabalhoDto();
        dto.setPrioridadeOperacional(prioridadeOperacional);
        dto.setPrioridadeBusca(prioridadeBusca);
        return converter(ordemServico.atualizarPrioridade(ordemId, dto));
    }

    @Override
    public OrdemPatioYardRespostaDTO suspender(Long ordemId) {
        return converter(ordemServico.suspender(ordemId));
    }

    @Override
    public OrdemPatioYardRespostaDTO retomar(Long ordemId) {
        return converter(ordemServico.retomar(ordemId));
    }

    private OrdemPatioYardRespostaDTO converter(OrdemTrabalhoPatioRespostaDto origem) {
        OrdemPatioYardRespostaDTO destino = new OrdemPatioYardRespostaDTO();
        destino.setId(origem.getId());
        destino.setCodigoConteiner(origem.getCodigoConteiner());
        destino.setDestino(origem.getDestino());
        destino.setLinhaDestino(origem.getLinhaDestino());
        destino.setColunaDestino(origem.getColunaDestino());
        destino.setCamadaDestino(origem.getCamadaDestino());
        destino.setTipoMovimento(origem.getTipoMovimento() == null ? null : origem.getTipoMovimento().name());
        destino.setStatusOrdem(origem.getStatusOrdem() == null ? null : origem.getStatusOrdem().name());
        destino.setVisitaNavioId(origem.getVisitaNavioId());
        destino.setItemOperacaoNavioId(origem.getItemOperacaoNavioId());
        destino.setSequenciaNavio(origem.getSequenciaNavio());
        destino.setPrioridadeOperacional(origem.getPrioridadeOperacional());
        return destino;
    }

    private FilaOrdemPatioYardDTO converter(FilaOrdemTrabalhoPatioDto origem) {
        FilaOrdemPatioYardDTO destino = new FilaOrdemPatioYardDTO();
        destino.setIdentificador(origem.getIdentificador());
        destino.setAgrupamento(origem.getAgrupamento());
        destino.setVisitaNavioId(origem.getVisitaNavioId());
        destino.setBerco(origem.getBerco());
        destino.setBlocoZona(origem.getBlocoZona());
        destino.setSequenciaInicial(origem.getSequenciaInicial());
        destino.setStatus(origem.getStatus() == null ? null : origem.getStatus().name());
        destino.setTotalOrdens(origem.getTotalOrdens());
        destino.setOrdens(origem.getOrdens() == null
                ? List.of()
                : origem.getOrdens().stream().map(this::converter).toList());
        return destino;
    }

    private WorkQueuePatioYardDTO converter(WorkQueuePatioRespostaDto origem) {
        WorkQueuePatioYardDTO destino = new WorkQueuePatioYardDTO();
        destino.setId(origem.getId());
        destino.setIdentificador(origem.getIdentificador());
        destino.setAgrupamento(origem.getAgrupamento());
        destino.setVisitaNavioId(origem.getVisitaNavioId());
        destino.setBerco(origem.getBerco());
        destino.setPorao(origem.getPorao());
        destino.setBlocoZona(origem.getBlocoZona());
        destino.setSequenciaInicial(origem.getSequenciaInicial());
        destino.setPow(origem.getPow());
        destino.setPoolOperacional(origem.getPoolOperacional());
        destino.setEquipamento(origem.getEquipamento());
        destino.setStatus(origem.getStatus());
        destino.setPrioridadeOperacional(origem.getPrioridadeOperacional());
        destino.setTotalOrdens(origem.getTotalOrdens());
        destino.setJobList(origem.getJobList() == null
                ? List.of()
                : origem.getJobList().stream().map(this::converter).toList());
        destino.setCriadoEm(origem.getCriadoEm());
        destino.setAtualizadoEm(origem.getAtualizadoEm());
        return destino;
    }
}
