package br.com.cloudport.servicoyard.vesselplanner.servico;

import br.com.cloudport.servicoyard.vesselplanner.dto.ExecucaoSequenciaGuindasteDtos.ConcluirMovimentoRequest;
import br.com.cloudport.servicoyard.vesselplanner.dto.ExecucaoSequenciaGuindasteDtos.CriarExecucaoRequest;
import br.com.cloudport.servicoyard.vesselplanner.dto.ExecucaoSequenciaGuindasteDtos.ExecucaoResponse;
import br.com.cloudport.servicoyard.vesselplanner.dto.ExecucaoSequenciaGuindasteDtos.FalharMovimentoRequest;
import br.com.cloudport.servicoyard.vesselplanner.dto.ExecucaoSequenciaGuindasteDtos.IniciarMovimentoRequest;
import br.com.cloudport.servicoyard.vesselplanner.dto.ExecucaoSequenciaGuindasteDtos.MovimentoResponse;
import br.com.cloudport.servicoyard.vesselplanner.dto.ExecucaoSequenciaGuindasteDtos.ReconciliarExecucaoRequest;
import br.com.cloudport.servicoyard.vesselplanner.dto.ExecucaoSequenciaGuindasteDtos.ReplanejarMovimentoRequest;
import br.com.cloudport.servicoyard.vesselplanner.dto.GuindasteOperacaoDto;
import br.com.cloudport.servicoyard.vesselplanner.dto.SequenciamentoGuindasteDto;
import br.com.cloudport.servicoyard.vesselplanner.modelo.EstivagemPlan;
import br.com.cloudport.servicoyard.vesselplanner.modelo.ExecucaoSequenciaGuindaste;
import br.com.cloudport.servicoyard.vesselplanner.modelo.MovimentoExecucaoGuindaste;
import br.com.cloudport.servicoyard.vesselplanner.modelo.SlotNavio;
import br.com.cloudport.servicoyard.vesselplanner.modelo.StatusMovimentoExecucaoGuindaste;
import br.com.cloudport.servicoyard.vesselplanner.repositorio.EstivagemPlanRepositorio;
import br.com.cloudport.servicoyard.vesselplanner.repositorio.ExecucaoSequenciaGuindasteRepositorio;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ExecucaoSequenciaGuindasteServico {

    private static final int NUMERO_GUINDASTES_PADRAO = 2;
    private static final int DURACAO_MOVIMENTO_PADRAO_MINUTOS = 5;

    private final EstivagemPlanRepositorio planRepositorio;
    private final ExecucaoSequenciaGuindasteRepositorio execucaoRepositorio;
    private final SequenciamentoGuindasteServico sequenciamentoServico;
    private final TampaPoraoServico tampaPoraoServico;
    private final EventoOperacionalGuindasteServico eventoOperacionalServico;

    public ExecucaoSequenciaGuindasteServico(
            EstivagemPlanRepositorio planRepositorio,
            ExecucaoSequenciaGuindasteRepositorio execucaoRepositorio,
            SequenciamentoGuindasteServico sequenciamentoServico,
            TampaPoraoServico tampaPoraoServico,
            EventoOperacionalGuindasteServico eventoOperacionalServico) {
        this.planRepositorio = planRepositorio;
        this.execucaoRepositorio = execucaoRepositorio;
        this.sequenciamentoServico = sequenciamentoServico;
        this.tampaPoraoServico = tampaPoraoServico;
        this.eventoOperacionalServico = eventoOperacionalServico;
    }

    @Transactional
    public ExecucaoResponse criar(Long planId, CriarExecucaoRequest request) {
        execucaoRepositorio.findByEstivagemId(planId).ifPresent(execucao -> {
            throw conflito("O plano já possui execução de sequência de guindastes.");
        });

        EstivagemPlan plan = planRepositorio.findById(planId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Plano de estivagem não encontrado: " + planId));

        int numeroGuindastes = request != null && request.numGuindastes() != null
                ? request.numGuindastes()
                : NUMERO_GUINDASTES_PADRAO;
        int duracaoMinutos = request != null && request.duracaoMovimentoMinutos() != null
                ? request.duracaoMovimentoMinutos()
                : DURACAO_MOVIMENTO_PADRAO_MINUTOS;
        LocalDateTime janelaInicio = request != null && request.janelaInicio() != null
                ? request.janelaInicio()
                : LocalDateTime.now();

        SequenciamentoGuindasteDto sequenciamento = sequenciamentoServico.sequenciar(plan, numeroGuindastes);
        if (sequenciamento.getSequencia() == null || sequenciamento.getSequencia().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O plano não possui movimentos para iniciar a execução de guindastes.");
        }

        ExecucaoSequenciaGuindaste execucao = new ExecucaoSequenciaGuindaste();
        execucao.setEstivagem(plan);
        execucao.setNumeroGuindastes(numeroGuindastes);
        execucao.setJanelaBaseInicio(janelaInicio);
        execucao.setDuracaoMovimentoMinutos(duracaoMinutos);

        Map<Integer, Integer> indicePorGuindaste = new HashMap<>();
        for (GuindasteOperacaoDto operacao : sequenciamento.getSequencia()) {
            int indice = indicePorGuindaste.getOrDefault(operacao.getGuindasteId(), 0);
            LocalDateTime inicioMovimento = janelaInicio.plusMinutes((long) indice * duracaoMinutos);
            LocalDateTime fimMovimento = inicioMovimento.plusMinutes(duracaoMinutos);
            indicePorGuindaste.put(operacao.getGuindasteId(), indice + 1);

            MovimentoExecucaoGuindaste movimento = new MovimentoExecucaoGuindaste();
            movimento.setOrdemPlanejada(operacao.getOrdem());
            movimento.setGuindasteId(operacao.getGuindasteId());
            movimento.setCodigoContainer(operacao.getCodigoContainer());
            movimento.setBay(operacao.getBay());
            movimento.setRowBay(operacao.getRowBay());
            movimento.setTier(operacao.getTier());
            movimento.setTipoOperacao(operacao.getTipoOperacao());
            movimento.setJanelaInicioPlanejada(inicioMovimento);
            movimento.setJanelaFimPlanejada(fimMovimento);
            movimento.setQuantidadePlanejada(BigDecimal.ONE);
            movimento.setQuantidadeRealizada(BigDecimal.ZERO);
            execucao.adicionarMovimento(movimento);
        }

        return toResponse(execucaoRepositorio.saveAndFlush(execucao));
    }

    @Transactional(readOnly = true)
    public ExecucaoResponse buscarPorPlano(Long planId) {
        return toResponse(execucaoRepositorio.findByEstivagemId(planId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Execução de guindastes ainda não criada para o plano " + planId + ".")));
    }

    @Transactional
    public ExecucaoResponse iniciar(Long execucaoId,
                                     Long movimentoId,
                                     IniciarMovimentoRequest request,
                                     String usuario) {
        ExecucaoSequenciaGuindaste execucao = buscarExecucao(execucaoId);
        MovimentoExecucaoGuindaste movimento = buscarMovimento(execucao, movimentoId);
        validarVersao(movimento.getVersao(), request.versao(), "movimento");
        eventoOperacionalServico.validarGuindasteDisponivel(
                execucaoId,
                movimento.getGuindasteId(),
                request.ocorridoEm());
        executarTransicao(() -> {
            SlotNavio slot = buscarSlotDoMovimento(execucao.getEstivagem(), movimento);
            tampaPoraoServico.validarInicioMovimento(
                    execucao.getEstivagem(),
                    slot,
                    movimento.getCodigoContainer());
            movimento.iniciar(request.ocorridoEm(), usuario);
        });
        execucao.atualizarStatus();
        return toResponse(execucaoRepositorio.saveAndFlush(execucao));
    }

    @Transactional
    public ExecucaoResponse concluir(Long execucaoId,
                                      Long movimentoId,
                                      ConcluirMovimentoRequest request,
                                      String usuario) {
        ExecucaoSequenciaGuindaste execucao = buscarExecucao(execucaoId);
        MovimentoExecucaoGuindaste movimento = buscarMovimento(execucao, movimentoId);
        validarVersao(movimento.getVersao(), request.versao(), "movimento");
        executarTransicao(() -> movimento.concluir(
                request.quantidadeRealizada(),
                request.concluidoEm(),
                usuario));
        execucao.atualizarStatus();
        return toResponse(execucaoRepositorio.saveAndFlush(execucao));
    }

    @Transactional
    public ExecucaoResponse falhar(Long execucaoId,
                                    Long movimentoId,
                                    FalharMovimentoRequest request,
                                    String usuario) {
        ExecucaoSequenciaGuindaste execucao = buscarExecucao(execucaoId);
        MovimentoExecucaoGuindaste movimento = buscarMovimento(execucao, movimentoId);
        validarVersao(movimento.getVersao(), request.versao(), "movimento");
        executarTransicao(() -> movimento.falhar(
                request.excecao(),
                request.quantidadeRealizada(),
                request.ocorridoEm(),
                usuario));
        execucao.atualizarStatus();
        return toResponse(execucaoRepositorio.saveAndFlush(execucao));
    }

    @Transactional
    public ExecucaoResponse replanejar(Long execucaoId,
                                        Long movimentoId,
                                        ReplanejarMovimentoRequest request,
                                        String usuario) {
        ExecucaoSequenciaGuindaste execucao = buscarExecucao(execucaoId);
        MovimentoExecucaoGuindaste movimento = buscarMovimento(execucao, movimentoId);
        validarVersao(movimento.getVersao(), request.versao(), "movimento");
        validarOrdemDisponivel(execucao, movimento, request.ordemPlanejada());
        validarJanelaDisponivel(
                execucao,
                movimento,
                request.guindasteId(),
                request.janelaInicio(),
                request.janelaFim());
        executarTransicao(() -> movimento.replanejar(
                request.guindasteId(),
                request.ordemPlanejada(),
                request.janelaInicio(),
                request.janelaFim(),
                request.motivo(),
                usuario));
        execucao.atualizarStatus();
        return toResponse(execucaoRepositorio.saveAndFlush(execucao));
    }

    @Transactional
    public ExecucaoResponse reconciliar(Long execucaoId,
                                         ReconciliarExecucaoRequest request,
                                         String usuario) {
        ExecucaoSequenciaGuindaste execucao = buscarExecucao(execucaoId);
        validarVersao(execucao.getVersao(), request.versao(), "execução");
        executarTransicao(() -> execucao.reconciliar(request.observacao(), usuario));
        return toResponse(execucaoRepositorio.saveAndFlush(execucao));
    }

    private ExecucaoSequenciaGuindaste buscarExecucao(Long execucaoId) {
        return execucaoRepositorio.findById(execucaoId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Execução de guindastes não encontrada: " + execucaoId));
    }

    private MovimentoExecucaoGuindaste buscarMovimento(
            ExecucaoSequenciaGuindaste execucao,
            Long movimentoId) {
        return execucao.getMovimentos().stream()
                .filter(movimento -> Objects.equals(movimento.getId(), movimentoId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Movimento não pertence à execução informada: " + movimentoId));
    }

    private SlotNavio buscarSlotDoMovimento(
            EstivagemPlan plan,
            MovimentoExecucaoGuindaste movimento) {
        return plan.getSlots().stream()
                .filter(slot -> slot.getBay() == movimento.getBay())
                .filter(slot -> slot.getRowBay() == movimento.getRowBay())
                .filter(slot -> slot.getTier() == movimento.getTier())
                .findFirst()
                .orElseThrow(() -> conflito(
                        "O slot do movimento não foi encontrado no plano de estivagem."));
    }

    private void validarVersao(Long versaoAtual, Long versaoEsperada, String recurso) {
        if (!Objects.equals(versaoAtual, versaoEsperada)) {
            throw conflito("A " + recurso + " foi alterada por outro operador. Recarregue os dados antes de continuar.");
        }
    }

    private void validarOrdemDisponivel(
            ExecucaoSequenciaGuindaste execucao,
            MovimentoExecucaoGuindaste movimentoAtual,
            Integer novaOrdem) {
        boolean ocupada = execucao.getMovimentos().stream()
                .filter(movimento -> !Objects.equals(movimento.getId(), movimentoAtual.getId()))
                .anyMatch(movimento -> Objects.equals(movimento.getOrdemPlanejada(), novaOrdem));
        if (ocupada) {
            throw conflito("A ordem planejada " + novaOrdem + " já está atribuída a outro movimento.");
        }
    }

    private void validarJanelaDisponivel(
            ExecucaoSequenciaGuindaste execucao,
            MovimentoExecucaoGuindaste movimentoAtual,
            Integer guindasteId,
            LocalDateTime inicio,
            LocalDateTime fim) {
        boolean sobreposta = execucao.getMovimentos().stream()
                .filter(movimento -> !Objects.equals(movimento.getId(), movimentoAtual.getId()))
                .filter(movimento -> Objects.equals(movimento.getGuindasteId(), guindasteId))
                .anyMatch(movimento -> inicio.isBefore(movimento.getJanelaFimPlanejada())
                        && movimento.getJanelaInicioPlanejada().isBefore(fim));
        if (sobreposta) {
            throw conflito("A janela replanejada conflita com outro movimento do guindaste " + guindasteId + ".");
        }
    }

    private void executarTransicao(Runnable transicao) {
        try {
            transicao.run();
        } catch (IllegalStateException exception) {
            throw conflito(exception.getMessage());
        }
    }

    private ResponseStatusException conflito(String mensagem) {
        return new ResponseStatusException(HttpStatus.CONFLICT, mensagem);
    }

    private ExecucaoResponse toResponse(ExecucaoSequenciaGuindaste execucao) {
        List<MovimentoResponse> movimentos = execucao.getMovimentos().stream()
                .sorted(Comparator.comparing(MovimentoExecucaoGuindaste::getOrdemPlanejada))
                .map(this::toMovimentoResponse)
                .collect(Collectors.toList());

        BigDecimal quantidadePlanejada = execucao.getMovimentos().stream()
                .map(MovimentoExecucaoGuindaste::getQuantidadePlanejada)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal quantidadeRealizada = execucao.getMovimentos().stream()
                .map(MovimentoExecucaoGuindaste::getQuantidadeRealizada)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int concluidos = (int) execucao.getMovimentos().stream()
                .filter(movimento -> movimento.getStatus() == StatusMovimentoExecucaoGuindaste.CONCLUIDO)
                .count();
        int falhas = (int) execucao.getMovimentos().stream()
                .filter(movimento -> movimento.getStatus() == StatusMovimentoExecucaoGuindaste.FALHA)
                .count();
        int emExecucao = (int) execucao.getMovimentos().stream()
                .filter(movimento -> movimento.getStatus() == StatusMovimentoExecucaoGuindaste.EM_EXECUCAO)
                .count();
        int total = execucao.getMovimentos().size();
        BigDecimal percentual = total == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(concluidos + falhas)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);

        return new ExecucaoResponse(
                execucao.getId(),
                execucao.getEstivagem().getId(),
                execucao.getVersao(),
                execucao.getStatus().name(),
                execucao.getNumeroGuindastes(),
                execucao.getJanelaBaseInicio(),
                execucao.getDuracaoMovimentoMinutos(),
                quantidadePlanejada,
                quantidadeRealizada,
                quantidadePlanejada.subtract(quantidadeRealizada),
                total,
                concluidos,
                falhas,
                emExecucao,
                percentual,
                execucao.getReconciliadoEm(),
                execucao.getReconciliadoPor(),
                execucao.getObservacaoReconciliacao(),
                movimentos);
    }

    private MovimentoResponse toMovimentoResponse(MovimentoExecucaoGuindaste movimento) {
        boolean atrasado = !movimento.terminal()
                && movimento.getJanelaFimPlanejada() != null
                && LocalDateTime.now().isAfter(movimento.getJanelaFimPlanejada());
        return new MovimentoResponse(
                movimento.getId(),
                movimento.getVersao(),
                movimento.getOrdemPlanejada(),
                movimento.getGuindasteId(),
                movimento.getCodigoContainer(),
                movimento.getBay(),
                movimento.getRowBay(),
                movimento.getTier(),
                movimento.getTipoOperacao(),
                movimento.getJanelaInicioPlanejada(),
                movimento.getJanelaFimPlanejada(),
                movimento.getQuantidadePlanejada(),
                movimento.getQuantidadeRealizada(),
                movimento.getQuantidadePlanejada().subtract(movimento.getQuantidadeRealizada()),
                movimento.getStatus().name(),
                movimento.getIniciadoEm(),
                movimento.getIniciadoPor(),
                movimento.getConcluidoEm(),
                movimento.getConcluidoPor(),
                movimento.getExcecao(),
                movimento.getMotivoReplanejamento(),
                movimento.getReplanejadoEm(),
                movimento.getReplanejadoPor(),
                atrasado);
    }
}
