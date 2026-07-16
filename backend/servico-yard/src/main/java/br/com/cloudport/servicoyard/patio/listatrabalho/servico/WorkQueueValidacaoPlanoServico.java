package br.com.cloudport.servicoyard.patio.listatrabalho.servico;

import br.com.cloudport.servicoyard.patio.listatrabalho.dto.WorkQueueValidacaoPlanoDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.OrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.StatusWorkQueuePatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.WorkQueuePatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.repositorio.OrdemTrabalhoPatioRepositorio;
import br.com.cloudport.servicoyard.patio.listatrabalho.repositorio.WorkQueuePatioRepositorio;
import br.com.cloudport.servicoyard.patio.modelo.EquipamentoPatio;
import br.com.cloudport.servicoyard.patio.modelo.StatusEquipamento;
import br.com.cloudport.servicoyard.patio.repositorio.EquipamentoPatioRepositorio;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WorkQueueValidacaoPlanoServico {

    private final WorkQueuePatioRepositorio workQueueRepositorio;
    private final OrdemTrabalhoPatioRepositorio ordemRepositorio;
    private final EquipamentoPatioRepositorio equipamentoRepositorio;
    private final WorkQueueOperacaoServico operacaoServico;

    public WorkQueueValidacaoPlanoServico(
            WorkQueuePatioRepositorio workQueueRepositorio,
            OrdemTrabalhoPatioRepositorio ordemRepositorio,
            EquipamentoPatioRepositorio equipamentoRepositorio,
            WorkQueueOperacaoServico operacaoServico
    ) {
        this.workQueueRepositorio = workQueueRepositorio;
        this.ordemRepositorio = ordemRepositorio;
        this.equipamentoRepositorio = equipamentoRepositorio;
        this.operacaoServico = operacaoServico;
    }

    @Transactional(readOnly = true)
    public List<WorkQueueValidacaoPlanoDto> consultar(Long visitaNavioId) {
        if (visitaNavioId == null || visitaNavioId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A visita de navio e obrigatoria.");
        }
        Map<String, List<String>> matrizEstados = operacaoServico.matrizOficialEstados();
        return workQueueRepositorio.findByVisitaNavioIdOrderBySequenciaInicialAscCriadoEmAsc(visitaNavioId)
                .stream()
                .map(fila -> montar(fila, matrizEstados))
                .toList();
    }

    private WorkQueueValidacaoPlanoDto montar(
            WorkQueuePatio fila,
            Map<String, List<String>> matrizEstados
    ) {
        EquipamentoPatio equipamento = fila.getEquipamentoPatioId() == null
                ? null
                : equipamentoRepositorio.findById(fila.getEquipamentoPatioId()).orElse(null);
        List<OrdemTrabalhoPatio> ordens = ordemRepositorio
                .findByWorkQueueIdOrderByPrioridadeBuscaDescPrioridadeOperacionalAscSequenciaNavioAscCriadoEmAsc(
                        fila.getId());
        int dispatchaveis = (int) ordens.stream()
                .filter(ordem -> matrizEstados
                        .getOrDefault(ordem.getStatusOrdem().name(), List.of())
                        .contains("EM_EXECUCAO"))
                .count();

        WorkQueueValidacaoPlanoDto dto = new WorkQueueValidacaoPlanoDto();
        dto.setId(fila.getId());
        dto.setVisitaNavioId(fila.getVisitaNavioId());
        dto.setIdentificador(fila.getIdentificador());
        dto.setBerco(fila.getBerco());
        dto.setPorao(fila.getPorao());
        dto.setStatus(fila.getStatus() == null ? null : fila.getStatus().name());
        dto.setPow(fila.getPow());
        dto.setPoolOperacional(fila.getPoolOperacional());
        dto.setEquipamentoPatioId(fila.getEquipamentoPatioId());
        dto.setEquipamentoIdentificador(equipamento == null ? null : equipamento.getIdentificador());
        dto.setEquipamentoTipo(equipamento == null ? null : equipamento.getTipoEquipamento().name());
        dto.setEquipamentoStatus(equipamento == null ? null : equipamento.getStatusOperacional().name());
        dto.setPlanoGuindasteId(fila.getPlanoGuindasteId());
        dto.setRecursoCaisId(fila.getRecursoCaisId());
        dto.setTotalOrdens(ordens.size());
        dto.setTotalOrdensDispatchaveis(dispatchaveis);
        dto.setCoberturaValida(
                fila.getStatus() == StatusWorkQueuePatio.ATIVA
                        && fila.getPorao() != null
                        && StringUtils.hasText(fila.getPow())
                        && StringUtils.hasText(fila.getPoolOperacional())
                        && fila.getRecursoCaisId() != null
                        && equipamento != null
                        && equipamento.getStatusOperacional() == StatusEquipamento.OPERACIONAL
                        && dispatchaveis > 0);
        return dto;
    }
}
