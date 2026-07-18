package br.com.cloudport.servicoyard.vesselplanner.servico;

import br.com.cloudport.servicoyard.vesselplanner.dto.TampaPoraoDTOs.ComandoTarefaRequest;
import br.com.cloudport.servicoyard.vesselplanner.dto.TampaPoraoDTOs.TampaResposta;
import br.com.cloudport.servicoyard.vesselplanner.modelo.EstivagemPlan;
import br.com.cloudport.servicoyard.vesselplanner.modelo.ExecucaoSequenciaGuindaste;
import br.com.cloudport.servicoyard.vesselplanner.modelo.MovimentoExecucaoGuindaste;
import br.com.cloudport.servicoyard.vesselplanner.modelo.SlotNavio;
import br.com.cloudport.servicoyard.vesselplanner.modelo.StatusMovimentoExecucaoGuindaste;
import br.com.cloudport.servicoyard.vesselplanner.modelo.TarefaTampaPorao;
import br.com.cloudport.servicoyard.vesselplanner.modelo.TampaPoraoTipos.TipoOperacaoTampaPorao;
import br.com.cloudport.servicoyard.vesselplanner.repositorio.EstivagemPlanRepositorio;
import br.com.cloudport.servicoyard.vesselplanner.repositorio.ExecucaoSequenciaGuindasteRepositorio;
import br.com.cloudport.servicoyard.vesselplanner.repositorio.TarefaTampaPoraoRepositorio;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import javax.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TampaPoraoComandoTransacionalServico {

    private final EstivagemPlanRepositorio planRepositorio;
    private final ExecucaoSequenciaGuindasteRepositorio execucaoRepositorio;
    private final TarefaTampaPoraoRepositorio tarefaRepositorio;
    private final TampaPoraoServico tampaPoraoServico;

    public TampaPoraoComandoTransacionalServico(
            EstivagemPlanRepositorio planRepositorio,
            ExecucaoSequenciaGuindasteRepositorio execucaoRepositorio,
            TarefaTampaPoraoRepositorio tarefaRepositorio,
            TampaPoraoServico tampaPoraoServico) {
        this.planRepositorio = planRepositorio;
        this.execucaoRepositorio = execucaoRepositorio;
        this.tarefaRepositorio = tarefaRepositorio;
        this.tampaPoraoServico = tampaPoraoServico;
    }

    @Transactional
    public TampaResposta iniciarTarefa(
            Long planId,
            Long tarefaId,
            ComandoTarefaRequest request,
            String usuario) {
        ExecucaoSequenciaGuindaste execucao = execucaoRepositorio
                .findLockedByEstivagemId(planId)
                .orElse(null);
        EstivagemPlan plan = planRepositorio.findLockedById(planId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "EstivagemPlan não encontrado: " + planId));
        TarefaTampaPorao tarefa = tarefaRepositorio.findById(tarefaId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Tarefa de tampa não encontrada: " + tarefaId));
        if (!Objects.equals(planId, tarefa.getTampa().getEstivagem().getId())) {
            throw new IllegalArgumentException("A tarefa não pertence ao plano informado.");
        }

        validarAusenciaDeMovimentoAtivo(plan, execucao, tarefa);
        validarMarcoDaSequencia(plan, execucao, tarefa);
        return tampaPoraoServico.iniciarTarefa(planId, tarefaId, request, usuario);
    }

    private void validarAusenciaDeMovimentoAtivo(
            EstivagemPlan plan,
            ExecucaoSequenciaGuindaste execucao,
            TarefaTampaPorao tarefa) {
        if (execucao == null) {
            return;
        }
        String codigoTampa = normalizar(tarefa.getTampa().getCodigo());
        boolean movimentoAtivo = execucao.getMovimentos().stream()
                .filter(movimento -> movimento.getStatus() == StatusMovimentoExecucaoGuindaste.EM_EXECUCAO)
                .map(movimento -> buscarSlot(plan, movimento))
                .filter(Objects::nonNull)
                .map(SlotNavio::getCodigoHatchCover)
                .filter(Objects::nonNull)
                .map(this::normalizar)
                .anyMatch(codigoTampa::equals);
        if (movimentoAtivo) {
            throw new IllegalStateException(
                    "A tarefa da tampa " + codigoTampa
                            + " não pode iniciar enquanto houver movimento de contêiner em execução.");
        }
    }

    private void validarMarcoDaSequencia(
            EstivagemPlan plan,
            ExecucaoSequenciaGuindaste execucao,
            TarefaTampaPorao tarefa) {
        if (tarefa.getTipo() != TipoOperacaoTampaPorao.POSICIONAR) {
            return;
        }
        String codigoTampa = normalizar(tarefa.getTampa().getCodigo());
        List<SlotNavio> slotsOcupadosNoPorao = plan.getSlots().stream()
                .filter(slot -> !slot.isSobreHatchCover())
                .filter(slot -> codigoTampa.equals(normalizar(slot.getCodigoHatchCover())))
                .filter(slot -> !vazio(slot.getCodigoContainer()))
                .toList();
        if (slotsOcupadosNoPorao.isEmpty()) {
            return;
        }
        if (execucao == null) {
            throw new IllegalStateException(
                    "A tampa " + codigoTampa
                            + " somente pode ser posicionada após executar os movimentos planejados do porão.");
        }
        boolean movimentoPendente = slotsOcupadosNoPorao.stream()
                .anyMatch(slot -> execucao.getMovimentos().stream()
                        .filter(movimento -> mesmaPosicao(slot, movimento))
                        .noneMatch(MovimentoExecucaoGuindaste::terminal));
        if (movimentoPendente) {
            throw new IllegalStateException(
                    "A tampa " + codigoTampa
                            + " somente pode ser posicionada após concluir ou registrar falha em todos os movimentos do porão.");
        }
    }

    private SlotNavio buscarSlot(
            EstivagemPlan plan,
            MovimentoExecucaoGuindaste movimento) {
        return plan.getSlots().stream()
                .filter(slot -> mesmaPosicao(slot, movimento))
                .findFirst()
                .orElse(null);
    }

    private boolean mesmaPosicao(
            SlotNavio slot,
            MovimentoExecucaoGuindaste movimento) {
        return slot.getBay() == movimento.getBay()
                && slot.getRowBay() == movimento.getRowBay()
                && slot.getTier() == movimento.getTier();
    }

    private String normalizar(String valor) {
        return valor == null ? "" : valor.trim().toUpperCase(Locale.ROOT);
    }

    private boolean vazio(String valor) {
        return valor == null || valor.trim().isEmpty();
    }
}
