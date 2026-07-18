package br.com.cloudport.servicoyard.vesselplanner.servico;

import br.com.cloudport.servicoyard.vesselplanner.modelo.ExecucaoSequenciaGuindaste;
import br.com.cloudport.servicoyard.vesselplanner.modelo.MomentoSequenciaTampaPorao;
import br.com.cloudport.servicoyard.vesselplanner.modelo.MovimentoExecucaoGuindaste;
import br.com.cloudport.servicoyard.vesselplanner.modelo.TarefaTampaPorao;
import br.com.cloudport.servicoyard.vesselplanner.modelo.TipoTarefaTampaPorao;
import br.com.cloudport.servicoyard.vesselplanner.repositorio.ExecucaoSequenciaGuindasteRepositorio;
import br.com.cloudport.servicoyard.vesselplanner.repositorio.TarefaTampaPoraoRepositorio;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MarcoSequenciaTampaPoraoValidador {

    private final TarefaTampaPoraoRepositorio tarefaRepositorio;
    private final ExecucaoSequenciaGuindasteRepositorio execucaoRepositorio;

    public MarcoSequenciaTampaPoraoValidador(
            TarefaTampaPoraoRepositorio tarefaRepositorio,
            ExecucaoSequenciaGuindasteRepositorio execucaoRepositorio) {
        this.tarefaRepositorio = tarefaRepositorio;
        this.execucaoRepositorio = execucaoRepositorio;
    }

    @Transactional(readOnly = true)
    public void validarInicio(Long planId, Long tarefaId) {
        TarefaTampaPorao tarefa = tarefaRepositorio.findById(tarefaId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Tarefa de tampa de porão não encontrada: " + tarefaId));
        if (tarefa.getTampa() == null
                || tarefa.getTampa().getEstivagem() == null
                || !Objects.equals(planId, tarefa.getTampa().getEstivagem().getId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A tarefa não pertence ao plano informado.");
        }
        if (tarefa.getTipo() != TipoTarefaTampaPorao.POSICIONAR
                || tarefa.getMomentoSequencia() != MomentoSequenciaTampaPorao.APOS) {
            return;
        }

        ExecucaoSequenciaGuindaste execucao = execucaoRepositorio.findByEstivagemId(planId)
                .orElseThrow(() -> conflito(
                        "A tampa somente pode ser posicionada após a execução dos movimentos do porão."));
        Integer ordemReferencia = tarefa.getOrdemMovimentoReferencia();
        String codigoTampa = tarefa.getTampa().getCodigo();
        boolean movimentoPendente = execucao.getMovimentos().stream()
                .filter(movimento -> codigoTampa.equalsIgnoreCase(movimento.getCodigoHatchCover()))
                .filter(movimento -> !movimento.isSobreHatchCover())
                .filter(movimento -> ordemReferencia == null
                        || movimento.getOrdemPlanejada() <= ordemReferencia)
                .anyMatch(movimento -> !movimento.terminal());
        if (movimentoPendente) {
            throw conflito(
                    "A tampa " + codigoTampa
                            + " somente pode ser posicionada após concluir ou registrar falha em todos os movimentos do porão vinculados à sequência.");
        }
    }

    private ResponseStatusException conflito(String mensagem) {
        return new ResponseStatusException(HttpStatus.CONFLICT, mensagem);
    }
}
