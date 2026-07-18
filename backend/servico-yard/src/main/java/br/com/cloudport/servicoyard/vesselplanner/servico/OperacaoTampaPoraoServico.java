package br.com.cloudport.servicoyard.vesselplanner.servico;

import br.com.cloudport.servicoyard.vesselplanner.dto.OperacaoTampaPoraoDTOs.ConfirmarTarefaRequest;
import br.com.cloudport.servicoyard.vesselplanner.dto.OperacaoTampaPoraoDTOs.IniciarTarefaRequest;
import br.com.cloudport.servicoyard.vesselplanner.dto.OperacaoTampaPoraoDTOs.TampaPoraoResposta;
import br.com.cloudport.servicoyard.vesselplanner.dto.OperacaoTampaPoraoDTOs.TarefaTampaPoraoResposta;
import br.com.cloudport.servicoyard.vesselplanner.modelo.EstivagemPlan;
import br.com.cloudport.servicoyard.vesselplanner.modelo.ExecucaoSequenciaGuindaste;
import br.com.cloudport.servicoyard.vesselplanner.modelo.MomentoSequenciaTampaPorao;
import br.com.cloudport.servicoyard.vesselplanner.modelo.MovimentoExecucaoGuindaste;
import br.com.cloudport.servicoyard.vesselplanner.modelo.PosicaoTampaPorao;
import br.com.cloudport.servicoyard.vesselplanner.modelo.SlotNavio;
import br.com.cloudport.servicoyard.vesselplanner.modelo.StatusMovimentoExecucaoGuindaste;
import br.com.cloudport.servicoyard.vesselplanner.modelo.StatusTarefaTampaPorao;
import br.com.cloudport.servicoyard.vesselplanner.modelo.TampaPorao;
import br.com.cloudport.servicoyard.vesselplanner.modelo.TarefaTampaPorao;
import br.com.cloudport.servicoyard.vesselplanner.modelo.TipoTarefaTampaPorao;
import br.com.cloudport.servicoyard.vesselplanner.repositorio.EstivagemPlanRepositorio;
import br.com.cloudport.servicoyard.vesselplanner.repositorio.ExecucaoSequenciaGuindasteRepositorio;
import br.com.cloudport.servicoyard.vesselplanner.repositorio.TampaPoraoRepositorio;
import br.com.cloudport.servicoyard.vesselplanner.repositorio.TarefaTampaPoraoRepositorio;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OperacaoTampaPoraoServico {

    private final EstivagemPlanRepositorio planRepositorio;
    private final TampaPoraoRepositorio tampaRepositorio;
    private final TarefaTampaPoraoRepositorio tarefaRepositorio;
    private final ExecucaoSequenciaGuindasteRepositorio execucaoRepositorio;

    public OperacaoTampaPoraoServico(
            EstivagemPlanRepositorio planRepositorio,
            TampaPoraoRepositorio tampaRepositorio,
            TarefaTampaPoraoRepositorio tarefaRepositorio,
            ExecucaoSequenciaGuindasteRepositorio execucaoRepositorio) {
        this.planRepositorio = planRepositorio;
        this.tampaRepositorio = tampaRepositorio;
        this.tarefaRepositorio = tarefaRepositorio;
        this.execucaoRepositorio = execucaoRepositorio;
    }

    @Transactional
    public List<TampaPoraoResposta> sincronizar(Long planId) {
        EstivagemPlan plan = buscarPlanoBloqueado(planId);
        Map<String, List<SlotNavio>> slotsPorTampa = plan.getSlots().stream()
                .filter(Objects::nonNull)
                .filter(slot -> !vazio(slot.getCodigoHatchCover()))
                .collect(Collectors.groupingBy(
                        slot -> normalizarCodigo(slot.getCodigoHatchCover()),
                        LinkedHashMap::new,
                        Collectors.toList()));

        Map<String, TampaPorao> existentes = tampaRepositorio
                .findByEstivagemIdOrderByBayInicialAscCodigoAsc(planId)
                .stream()
                .collect(Collectors.toMap(TampaPorao::getCodigo, Function.identity()));
        Map<Long, Integer> ordemMovimentos = calcularOrdemMovimentos(plan);

        slotsPorTampa.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> sincronizarTampa(
                        plan,
                        entry.getKey(),
                        entry.getValue(),
                        existentes.get(entry.getKey()),
                        ordemMovimentos));
        return listarInterno(planId);
    }

    @Transactional(readOnly = true)
    public List<TampaPoraoResposta> listar(Long planId) {
        buscarPlano(planId);
        return listarInterno(planId);
    }

    @Transactional
    public TampaPoraoResposta iniciarTarefa(
            Long planId,
            Long tarefaId,
            IniciarTarefaRequest request,
            String usuario) {
        buscarPlanoBloqueado(planId);
        TarefaTampaPorao tarefa = buscarTarefaDoPlano(planId, tarefaId);
        if (tarefa.getStatus() != StatusTarefaTampaPorao.LIBERADA) {
            throw new IllegalStateException("Somente tarefa liberada pode ser iniciada.");
        }
        if (tarefa.getDependencia() != null
                && tarefa.getDependencia().getStatus() != StatusTarefaTampaPorao.CONCLUIDA) {
            throw new IllegalStateException("A dependência operacional da tarefa ainda não foi concluída.");
        }
        if (existeMovimentoAtivo(planId, tarefa.getTampa().getCodigo())) {
            throw new IllegalStateException(
                    "A tarefa da tampa não pode iniciar enquanto houver movimento de contêiner em execução.");
        }

        tarefa.setStatus(StatusTarefaTampaPorao.EM_EXECUCAO);
        tarefa.setRecurso(normalizarObrigatorio(request.getRecurso(), "O recurso operacional é obrigatório."));
        tarefa.setIniciadoPor(normalizarUsuario(usuario));
        tarefa.setIniciadoEm(LocalDateTime.now());
        tarefa.setObservacao(normalizarOpcional(request.getObservacao()));
        tarefa.getTampa().setRecursoAtual(tarefa.getRecurso());
        tarefaRepositorio.save(tarefa);
        tampaRepositorio.save(tarefa.getTampa());
        return toDto(tarefa.getTampa());
    }

    @Transactional
    public TampaPoraoResposta confirmarTarefa(
            Long planId,
            Long tarefaId,
            ConfirmarTarefaRequest request,
            String usuario) {
        buscarPlanoBloqueado(planId);
        TarefaTampaPorao tarefa = buscarTarefaDoPlano(planId, tarefaId);
        if (tarefa.getStatus() != StatusTarefaTampaPorao.EM_EXECUCAO) {
            throw new IllegalStateException("Somente tarefa em execução pode ser confirmada.");
        }

        TampaPorao tampa = tarefa.getTampa();
        aplicarPosicaoConfirmada(tampa, tarefa.getTipo());
        tarefa.setStatus(StatusTarefaTampaPorao.CONCLUIDA);
        tarefa.setConfirmadoPor(normalizarUsuario(usuario));
        tarefa.setConfirmadoEm(LocalDateTime.now());
        if (!vazio(request.getObservacao())) {
            tarefa.setObservacao(normalizarOpcional(request.getObservacao()));
        }
        tampa.setRecursoAtual(null);
        tarefaRepositorio.save(tarefa);
        tampaRepositorio.save(tampa);

        tarefaRepositorio.findByDependenciaId(tarefa.getId()).stream()
                .filter(dependente -> dependente.getStatus() == StatusTarefaTampaPorao.PLANEJADA)
                .forEach(dependente -> {
                    dependente.setStatus(StatusTarefaTampaPorao.LIBERADA);
                    tarefaRepositorio.save(dependente);
                });
        return toDto(tampa);
    }

    @Transactional
    public TampaPoraoResposta cancelarTarefa(
            Long planId,
            Long tarefaId,
            String motivo,
            String usuario) {
        buscarPlanoBloqueado(planId);
        TarefaTampaPorao tarefa = buscarTarefaDoPlano(planId, tarefaId);
        if (tarefa.getStatus() == StatusTarefaTampaPorao.CONCLUIDA) {
            throw new IllegalStateException("Tarefa concluída não pode ser cancelada.");
        }
        if (tarefa.getStatus() == StatusTarefaTampaPorao.CANCELADA) {
            return toDto(tarefa.getTampa());
        }
        tarefa.setStatus(StatusTarefaTampaPorao.CANCELADA);
        tarefa.setCanceladoPor(normalizarUsuario(usuario));
        tarefa.setCanceladoEm(LocalDateTime.now());
        tarefa.setObservacao(normalizarObrigatorio(motivo, "O motivo do cancelamento é obrigatório."));
        tarefa.getTampa().setRecursoAtual(null);
        tarefaRepositorio.save(tarefa);
        tampaRepositorio.save(tarefa.getTampa());
        return toDto(tarefa.getTampa());
    }

    @Transactional(readOnly = true)
    public String motivoBloqueio(
            EstivagemPlan plan,
            String codigoHatchCover,
            boolean sobreHatchCover) {
        if (plan == null || plan.getId() == null || vazio(codigoHatchCover)) {
            return null;
        }
        String codigo = normalizarCodigo(codigoHatchCover);
        TampaPorao tampa = tampaRepositorio.findByEstivagemIdAndCodigo(plan.getId(), codigo)
                .orElse(null);
        if (tampa == null) {
            return "Movimento bloqueado: a tampa de porão " + codigo + " não possui planejamento persistido.";
        }
        boolean emOperacao = tarefaRepositorio.findByTampaIdOrderByOrdemOperacionalAscIdAsc(tampa.getId())
                .stream()
                .anyMatch(tarefa -> tarefa.getStatus() == StatusTarefaTampaPorao.EM_EXECUCAO);
        if (emOperacao) {
            return "Movimento bloqueado: a tampa de porão " + codigo + " está em operação.";
        }
        if (sobreHatchCover) {
            if (tampa.getPosicao() != PosicaoTampaPorao.FECHADA
                    && tampa.getPosicao() != PosicaoTampaPorao.POSICIONADA) {
                return "Movimento bloqueado: o slot sobre a tampa " + codigo
                        + " exige tampa posicionada ou fechada.";
            }
            return null;
        }
        if (tampa.getPosicao() != PosicaoTampaPorao.REMOVIDA) {
            return "Movimento bloqueado: o acesso ao porão exige a remoção da tampa " + codigo + ".";
        }
        return null;
    }

    @Transactional(readOnly = true)
    public String motivoBloqueio(
            ExecucaoSequenciaGuindaste execucao,
            MovimentoExecucaoGuindaste movimento) {
        return motivoBloqueio(
                execucao == null ? null : execucao.getEstivagem(),
                movimento == null ? null : movimento.getCodigoHatchCover(),
                movimento != null && movimento.isSobreHatchCover());
    }

    public void validarInicioMovimento(
            ExecucaoSequenciaGuindaste execucao,
            MovimentoExecucaoGuindaste movimento) {
        String motivo = motivoBloqueio(execucao, movimento);
        if (motivo != null) {
            throw new IllegalStateException(motivo);
        }
    }

    private void sincronizarTampa(
            EstivagemPlan plan,
            String codigo,
            List<SlotNavio> slots,
            TampaPorao existente,
            Map<Long, Integer> ordemMovimentos) {
        boolean nova = existente == null;
        TampaPorao tampa = nova ? new TampaPorao() : existente;
        if (nova) {
            tampa.setEstivagem(plan);
            tampa.setCodigo(codigo);
            tampa.setPosicao(PosicaoTampaPorao.FECHADA);
        }
        tampa.setBayInicial(slots.stream().mapToInt(SlotNavio::getBay).min().orElseThrow());
        tampa.setBayFinal(slots.stream().mapToInt(SlotNavio::getBay).max().orElseThrow());
        TampaPorao persistida = tampaRepositorio.save(tampa);
        if (nova || !tarefaRepositorio.existsByTampaId(persistida.getId())) {
            criarFluxoTarefas(persistida, slots, ordemMovimentos);
        }
    }

    private void criarFluxoTarefas(
            TampaPorao tampa,
            List<SlotNavio> slots,
            Map<Long, Integer> ordemMovimentos) {
        List<Integer> ordensNoPorao = slots.stream()
                .filter(slot -> !slot.isSobreHatchCover())
                .map(SlotNavio::getId)
                .filter(Objects::nonNull)
                .map(ordemMovimentos::get)
                .filter(Objects::nonNull)
                .sorted()
                .collect(Collectors.toList());
        Integer primeiraOrdem = ordensNoPorao.isEmpty() ? null : ordensNoPorao.get(0);
        Integer ultimaOrdem = ordensNoPorao.isEmpty() ? null : ordensNoPorao.get(ordensNoPorao.size() - 1);

        TarefaTampaPorao abrir = tarefaRepositorio.save(novaTarefa(
                tampa, TipoTarefaTampaPorao.ABRIR, 1, primeiraOrdem,
                MomentoSequenciaTampaPorao.ANTES, null, StatusTarefaTampaPorao.LIBERADA));
        TarefaTampaPorao remover = tarefaRepositorio.save(novaTarefa(
                tampa, TipoTarefaTampaPorao.REMOVER, 2, primeiraOrdem,
                MomentoSequenciaTampaPorao.ANTES, abrir, StatusTarefaTampaPorao.PLANEJADA));
        TarefaTampaPorao posicionar = tarefaRepositorio.save(novaTarefa(
                tampa, TipoTarefaTampaPorao.POSICIONAR, 3, ultimaOrdem,
                MomentoSequenciaTampaPorao.APOS, remover, StatusTarefaTampaPorao.PLANEJADA));
        tarefaRepositorio.save(novaTarefa(
                tampa, TipoTarefaTampaPorao.FECHAR, 4, ultimaOrdem,
                MomentoSequenciaTampaPorao.APOS, posicionar, StatusTarefaTampaPorao.PLANEJADA));
    }

    private TarefaTampaPorao novaTarefa(
            TampaPorao tampa,
            TipoTarefaTampaPorao tipo,
            int ordemOperacional,
            Integer ordemMovimentoReferencia,
            MomentoSequenciaTampaPorao momento,
            TarefaTampaPorao dependencia,
            StatusTarefaTampaPorao status) {
        TarefaTampaPorao tarefa = new TarefaTampaPorao();
        tarefa.setTampa(tampa);
        tarefa.setTipo(tipo);
        tarefa.setOrdemOperacional(ordemOperacional);
        tarefa.setOrdemMovimentoReferencia(ordemMovimentoReferencia);
        tarefa.setMomentoSequencia(momento);
        tarefa.setDependencia(dependencia);
        tarefa.setStatus(status);
        return tarefa;
    }

    private Map<Long, Integer> calcularOrdemMovimentos(EstivagemPlan plan) {
        List<SlotNavio> ocupados = plan.getSlots().stream()
                .filter(Objects::nonNull)
                .filter(slot -> slot.getId() != null)
                .filter(slot -> !vazio(slot.getCodigoContainer()))
                .sorted(Comparator
                        .comparingInt(SlotNavio::getBay)
                        .thenComparingInt(SlotNavio::getRowBay)
                        .thenComparing(Comparator.comparingInt(SlotNavio::getTier).reversed()))
                .collect(Collectors.toList());
        Map<Long, Integer> ordens = new LinkedHashMap<>();
        for (int indice = 0; indice < ocupados.size(); indice++) {
            ordens.put(ocupados.get(indice).getId(), indice + 1);
        }
        return ordens;
    }

    private boolean existeMovimentoAtivo(Long planId, String codigoHatchCover) {
        return execucaoRepositorio.findByEstivagemId(planId)
                .map(ExecucaoSequenciaGuindaste::getMovimentos)
                .stream()
                .flatMap(List::stream)
                .anyMatch(movimento -> movimento.getStatus() == StatusMovimentoExecucaoGuindaste.EM_EXECUCAO
                        && !vazio(movimento.getCodigoHatchCover())
                        && movimento.getCodigoHatchCover().equalsIgnoreCase(codigoHatchCover));
    }

    private void aplicarPosicaoConfirmada(TampaPorao tampa, TipoTarefaTampaPorao tipo) {
        if (tipo == TipoTarefaTampaPorao.ABRIR) {
            tampa.setPosicao(PosicaoTampaPorao.ABERTA);
        } else if (tipo == TipoTarefaTampaPorao.REMOVER) {
            tampa.setPosicao(PosicaoTampaPorao.REMOVIDA);
        } else if (tipo == TipoTarefaTampaPorao.POSICIONAR) {
            tampa.setPosicao(PosicaoTampaPorao.POSICIONADA);
        } else if (tipo == TipoTarefaTampaPorao.FECHAR) {
            tampa.setPosicao(PosicaoTampaPorao.FECHADA);
        }
    }

    private List<TampaPoraoResposta> listarInterno(Long planId) {
        return tampaRepositorio.findByEstivagemIdOrderByBayInicialAscCodigoAsc(planId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private EstivagemPlan buscarPlano(Long planId) {
        return planRepositorio.findById(planId)
                .orElseThrow(() -> new EntityNotFoundException("EstivagemPlan não encontrado: " + planId));
    }

    private EstivagemPlan buscarPlanoBloqueado(Long planId) {
        return planRepositorio.findLockedById(planId)
                .orElseThrow(() -> new EntityNotFoundException("EstivagemPlan não encontrado: " + planId));
    }

    private TarefaTampaPorao buscarTarefaDoPlano(Long planId, Long tarefaId) {
        TarefaTampaPorao tarefa = tarefaRepositorio.findById(tarefaId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Tarefa de tampa de porão não encontrada: " + tarefaId));
        if (tarefa.getTampa() == null
                || tarefa.getTampa().getEstivagem() == null
                || !planId.equals(tarefa.getTampa().getEstivagem().getId())) {
            throw new IllegalArgumentException("A tarefa não pertence ao plano informado.");
        }
        return tarefa;
    }

    private TampaPoraoResposta toDto(TampaPorao tampa) {
        TampaPoraoResposta dto = new TampaPoraoResposta();
        dto.setId(tampa.getId());
        dto.setCodigo(tampa.getCodigo());
        dto.setBayInicial(tampa.getBayInicial());
        dto.setBayFinal(tampa.getBayFinal());
        dto.setPosicao(tampa.getPosicao() == null ? null : tampa.getPosicao().name());
        dto.setRecursoAtual(tampa.getRecursoAtual());
        List<TarefaTampaPoraoResposta> tarefas = tarefaRepositorio
                .findByTampaIdOrderByOrdemOperacionalAscIdAsc(tampa.getId())
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        dto.setTarefas(tarefas);
        dto.setBloqueioAtivo(tarefas.stream()
                .anyMatch(tarefa -> StatusTarefaTampaPorao.EM_EXECUCAO.name().equals(tarefa.getStatus())));
        return dto;
    }

    private TarefaTampaPoraoResposta toDto(TarefaTampaPorao tarefa) {
        TarefaTampaPoraoResposta dto = new TarefaTampaPoraoResposta();
        dto.setId(tarefa.getId());
        dto.setTipo(tarefa.getTipo() == null ? null : tarefa.getTipo().name());
        dto.setStatus(tarefa.getStatus() == null ? null : tarefa.getStatus().name());
        dto.setOrdemOperacional(tarefa.getOrdemOperacional());
        dto.setOrdemMovimentoReferencia(tarefa.getOrdemMovimentoReferencia());
        dto.setMomentoSequencia(tarefa.getMomentoSequencia() == null
                ? null
                : tarefa.getMomentoSequencia().name());
        dto.setDependenciaId(tarefa.getDependencia() == null ? null : tarefa.getDependencia().getId());
        dto.setRecurso(tarefa.getRecurso());
        dto.setIniciadoPor(tarefa.getIniciadoPor());
        dto.setConfirmadoPor(tarefa.getConfirmadoPor());
        dto.setCanceladoPor(tarefa.getCanceladoPor());
        dto.setObservacao(tarefa.getObservacao());
        dto.setIniciadoEm(tarefa.getIniciadoEm());
        dto.setConfirmadoEm(tarefa.getConfirmadoEm());
        dto.setCanceladoEm(tarefa.getCanceladoEm());
        return dto;
    }

    private String normalizarCodigo(String valor) {
        return valor.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizarObrigatorio(String valor, String mensagem) {
        if (vazio(valor)) {
            throw new IllegalArgumentException(mensagem);
        }
        return valor.trim();
    }

    private String normalizarOpcional(String valor) {
        return vazio(valor) ? null : valor.trim();
    }

    private String normalizarUsuario(String usuario) {
        return vazio(usuario) ? "SISTEMA" : usuario.trim();
    }

    private boolean vazio(String valor) {
        return valor == null || valor.trim().isEmpty();
    }
}
