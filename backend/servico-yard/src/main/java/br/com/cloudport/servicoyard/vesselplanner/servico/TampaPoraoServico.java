package br.com.cloudport.servicoyard.vesselplanner.servico;

import br.com.cloudport.servicoyard.vesselplanner.dto.GuindasteOperacaoDto;
import br.com.cloudport.servicoyard.vesselplanner.dto.SequenciamentoGuindasteDto;
import br.com.cloudport.servicoyard.vesselplanner.dto.TampaPoraoDTOs.ComandoTarefaRequest;
import br.com.cloudport.servicoyard.vesselplanner.dto.TampaPoraoDTOs.CriarTarefaRequest;
import br.com.cloudport.servicoyard.vesselplanner.dto.TampaPoraoDTOs.PosicaoResposta;
import br.com.cloudport.servicoyard.vesselplanner.dto.TampaPoraoDTOs.TampaResposta;
import br.com.cloudport.servicoyard.vesselplanner.dto.TampaPoraoDTOs.TarefaResposta;
import br.com.cloudport.servicoyard.vesselplanner.modelo.DependenciaTarefaTampaPorao;
import br.com.cloudport.servicoyard.vesselplanner.modelo.EstivagemPlan;
import br.com.cloudport.servicoyard.vesselplanner.modelo.PosicaoTampaPorao;
import br.com.cloudport.servicoyard.vesselplanner.modelo.SlotNavio;
import br.com.cloudport.servicoyard.vesselplanner.modelo.TampaPorao;
import br.com.cloudport.servicoyard.vesselplanner.modelo.TarefaTampaPorao;
import br.com.cloudport.servicoyard.vesselplanner.modelo.TampaPoraoTipos.StatusTarefaTampaPorao;
import br.com.cloudport.servicoyard.vesselplanner.modelo.TampaPoraoTipos.TipoOperacaoTampaPorao;
import br.com.cloudport.servicoyard.vesselplanner.modelo.TampaPoraoTipos.TipoPosicaoTampaPorao;
import br.com.cloudport.servicoyard.vesselplanner.repositorio.DependenciaTarefaTampaPoraoRepositorio;
import br.com.cloudport.servicoyard.vesselplanner.repositorio.EstivagemPlanRepositorio;
import br.com.cloudport.servicoyard.vesselplanner.repositorio.PosicaoTampaPoraoRepositorio;
import br.com.cloudport.servicoyard.vesselplanner.repositorio.TampaPoraoRepositorio;
import br.com.cloudport.servicoyard.vesselplanner.repositorio.TarefaTampaPoraoRepositorio;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TampaPoraoServico {

    private final EstivagemPlanRepositorio planRepositorio;
    private final TampaPoraoRepositorio tampaRepositorio;
    private final PosicaoTampaPoraoRepositorio posicaoRepositorio;
    private final TarefaTampaPoraoRepositorio tarefaRepositorio;
    private final DependenciaTarefaTampaPoraoRepositorio dependenciaRepositorio;

    public TampaPoraoServico(EstivagemPlanRepositorio planRepositorio,
                             TampaPoraoRepositorio tampaRepositorio,
                             PosicaoTampaPoraoRepositorio posicaoRepositorio,
                             TarefaTampaPoraoRepositorio tarefaRepositorio,
                             DependenciaTarefaTampaPoraoRepositorio dependenciaRepositorio) {
        this.planRepositorio = planRepositorio;
        this.tampaRepositorio = tampaRepositorio;
        this.posicaoRepositorio = posicaoRepositorio;
        this.tarefaRepositorio = tarefaRepositorio;
        this.dependenciaRepositorio = dependenciaRepositorio;
    }

    @Transactional
    public void inicializarDoPlano(EstivagemPlan plan) {
        if (plan == null || plan.getId() == null) {
            throw new IllegalArgumentException("O plano persistido deve ser informado para inicializar as tampas.");
        }
        Set<String> codigos = plan.getSlots().stream()
                .map(SlotNavio::getCodigoHatchCover)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(codigo -> !codigo.isEmpty())
                .map(codigo -> codigo.toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());
        for (String codigo : codigos) {
            if (tampaRepositorio.findByEstivagemIdAndCodigo(plan.getId(), codigo).isPresent()) {
                continue;
            }
            TampaPorao tampa = new TampaPorao();
            tampa.setEstivagem(plan);
            tampa.setCodigo(codigo);
            tampa = tampaRepositorio.save(tampa);
            registrarPosicao(tampa, TipoPosicaoTampaPorao.SOBRE_PORAO, codigo);
        }
    }

    @Transactional
    public List<TampaResposta> listar(Long planId) {
        EstivagemPlan plan = buscarPlano(planId);
        inicializarDoPlano(plan);
        return tampaRepositorio.findByEstivagemIdOrderByCodigoAsc(planId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public TampaResposta criarTarefa(Long planId,
                                     Long tampaId,
                                     CriarTarefaRequest request,
                                     String usuario) {
        TampaPorao tampa = buscarTampa(planId, tampaId);
        Optional<TarefaTampaPorao> tarefaAnterior = tarefaRepositorio
                .findFirstByTampaIdAndStatusNotOrderByCriadoEmDesc(
                        tampaId,
                        StatusTarefaTampaPorao.CANCELADA);

        TarefaTampaPorao tarefa = new TarefaTampaPorao();
        tarefa.setTampa(tampa);
        tarefa.setTipo(request.getTipo());
        tarefa.setRecurso(request.getRecurso());
        tarefa.setOperador(normalizarUsuario(usuario));
        tarefa.setMotivo(request.getMotivo());
        tarefa.setPosicaoDestinoTipo(request.getPosicaoDestinoTipo());
        tarefa.setPosicaoDestinoReferencia(request.getPosicaoDestinoReferencia());
        tarefaRepositorio.save(tarefa);

        List<Long> dependenciasIds = Optional.ofNullable(request.getDependenciasIds())
                .orElseGet(ArrayList::new)
                .stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));
        if (dependenciasIds.isEmpty()) {
            tarefaAnterior.map(TarefaTampaPorao::getId).ifPresent(dependenciasIds::add);
        }
        for (Long dependenciaId : dependenciasIds) {
            TarefaTampaPorao dependencia = tarefaRepositorio.findById(dependenciaId)
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Tarefa de dependência não encontrada: " + dependenciaId));
            if (!dependencia.getTampa().getId().equals(tampaId)) {
                throw new IllegalArgumentException("A dependência deve pertencer à mesma tampa de porão.");
            }
            DependenciaTarefaTampaPorao vinculo = new DependenciaTarefaTampaPorao();
            vinculo.setTarefa(tarefa);
            vinculo.setDependencia(dependencia);
            dependenciaRepositorio.save(vinculo);
        }
        return toDto(tampa);
    }

    @Transactional
    public TampaResposta iniciarTarefa(Long planId,
                                       Long tarefaId,
                                       ComandoTarefaRequest request,
                                       String usuario) {
        TarefaTampaPorao tarefa = buscarTarefa(planId, tarefaId);
        Optional<TarefaTampaPorao> emExecucao = tarefaRepositorio
                .findFirstByTampaIdAndStatusOrderByCriadoEmDesc(
                        tarefa.getTampa().getId(),
                        StatusTarefaTampaPorao.EM_EXECUCAO);
        if (emExecucao.isPresent() && !emExecucao.get().getId().equals(tarefaId)) {
            throw new IllegalStateException("Já existe uma tarefa em execução para a tampa informada.");
        }
        List<DependenciaTarefaTampaPorao> dependencias = dependenciaRepositorio.findByTarefaId(tarefaId);
        boolean pendente = dependencias.stream()
                .map(DependenciaTarefaTampaPorao::getDependencia)
                .anyMatch(item -> item.getStatus() != StatusTarefaTampaPorao.CONCLUIDA);
        if (pendente) {
            throw new IllegalStateException("A tarefa possui dependências operacionais ainda não concluídas.");
        }
        tarefa.getTampa().validarInicio(tarefa.getTipo());
        tarefa.setOperador(normalizarUsuario(usuario));
        aplicarMotivo(tarefa, request);
        tarefa.iniciar();
        tarefaRepositorio.save(tarefa);
        return toDto(tarefa.getTampa());
    }

    @Transactional
    public TampaResposta confirmarTarefa(Long planId,
                                         Long tarefaId,
                                         ComandoTarefaRequest request,
                                         String usuario) {
        TarefaTampaPorao tarefa = buscarTarefa(planId, tarefaId);
        TampaPorao tampa = tarefa.getTampa();
        tampa.confirmar(tarefa.getTipo());
        tarefa.setOperador(normalizarUsuario(usuario));
        aplicarMotivo(tarefa, request);
        tarefa.concluir();
        tarefaRepositorio.save(tarefa);
        tampaRepositorio.save(tampa);
        atualizarPosicaoAposConclusao(tarefa);
        return toDto(tampa);
    }

    @Transactional
    public TampaResposta cancelarTarefa(Long planId,
                                        Long tarefaId,
                                        ComandoTarefaRequest request,
                                        String usuario) {
        TarefaTampaPorao tarefa = buscarTarefa(planId, tarefaId);
        tarefa.setOperador(normalizarUsuario(usuario));
        tarefa.cancelar(request == null ? null : request.getMotivo());
        tarefaRepositorio.save(tarefa);
        return toDto(tarefa.getTampa());
    }

    @Transactional
    public void validarInicioMovimento(EstivagemPlan plan,
                                       SlotNavio destino,
                                       String codigoContainer) {
        inicializarDoPlano(plan);
        SlotNavio origem = plan.getSlots().stream()
                .filter(slot -> codigoContainer != null
                        && codigoContainer.equalsIgnoreCase(slot.getCodigoContainer()))
                .findFirst()
                .orElse(null);
        validarSlotParaMovimento(plan.getId(), origem);
        validarSlotParaMovimento(plan.getId(), destino);
    }

    @Transactional(readOnly = true)
    public void enriquecerSequenciamento(EstivagemPlan plan, SequenciamentoGuindasteDto sequenciamento) {
        if (sequenciamento == null || sequenciamento.getSequencia() == null) {
            return;
        }
        Map<String, SlotNavio> slots = plan.getSlots().stream()
                .collect(Collectors.toMap(this::chavePosicao, Function.identity(), (a, b) -> a));
        Map<String, TampaPorao> tampas = tampaRepositorio.findByEstivagemIdOrderByCodigoAsc(plan.getId()).stream()
                .collect(Collectors.toMap(TampaPorao::getCodigo, Function.identity()));
        for (GuindasteOperacaoDto operacao : sequenciamento.getSequencia()) {
            SlotNavio slot = slots.get(chavePosicao(operacao.getBay(), operacao.getRowBay(), operacao.getTier()));
            if (slot == null || vazio(slot.getCodigoHatchCover())) {
                continue;
            }
            String codigo = normalizarCodigo(slot.getCodigoHatchCover());
            operacao.setCodigoHatchCover(codigo);
            TampaPorao tampa = tampas.get(codigo);
            String bloqueio = motivoBloqueio(tampa, slot);
            operacao.setBloqueadoPorTampa(bloqueio != null);
            operacao.setMotivoBloqueioTampa(bloqueio);
        }
    }

    private void validarSlotParaMovimento(Long planId, SlotNavio slot) {
        if (slot == null || vazio(slot.getCodigoHatchCover())) {
            return;
        }
        String codigo = normalizarCodigo(slot.getCodigoHatchCover());
        TampaPorao tampa = tampaRepositorio.findByEstivagemIdAndCodigo(planId, codigo)
                .orElseThrow(() -> new IllegalStateException(
                        "A tampa " + codigo + " não possui estado operacional persistido."));
        String bloqueio = motivoBloqueio(tampa, slot);
        if (bloqueio != null) {
            throw new IllegalStateException(bloqueio);
        }
    }

    private String motivoBloqueio(TampaPorao tampa, SlotNavio slot) {
        if (tampa == null) {
            return "A tampa " + normalizarCodigo(slot.getCodigoHatchCover())
                    + " não possui estado operacional persistido.";
        }
        boolean operacaoAtiva = tarefaRepositorio.findFirstByTampaIdAndStatusOrderByCriadoEmDesc(
                tampa.getId(), StatusTarefaTampaPorao.EM_EXECUCAO).isPresent();
        if (operacaoAtiva) {
            return "A tampa " + tampa.getCodigo() + " possui uma tarefa em execução.";
        }
        if (!tampa.permiteMovimento(slot.isSobreHatchCover())) {
            return tampa.motivoBloqueio(slot.isSobreHatchCover());
        }
        return null;
    }

    private void atualizarPosicaoAposConclusao(TarefaTampaPorao tarefa) {
        if (tarefa.getTipo() == TipoOperacaoTampaPorao.REMOVER) {
            TipoPosicaoTampaPorao tipo = tarefa.getPosicaoDestinoTipo() == null
                    ? TipoPosicaoTampaPorao.AREA_SEGURA
                    : tarefa.getPosicaoDestinoTipo();
            String referencia = vazio(tarefa.getPosicaoDestinoReferencia())
                    ? "Área operacional segura"
                    : tarefa.getPosicaoDestinoReferencia();
            substituirPosicao(tarefa.getTampa(), tipo, referencia);
        } else if (tarefa.getTipo() == TipoOperacaoTampaPorao.POSICIONAR) {
            substituirPosicao(
                    tarefa.getTampa(),
                    TipoPosicaoTampaPorao.SOBRE_PORAO,
                    tarefa.getTampa().getCodigo());
        }
    }

    private void substituirPosicao(TampaPorao tampa,
                                   TipoPosicaoTampaPorao tipo,
                                   String referencia) {
        posicaoRepositorio.findFirstByTampaIdAndAtivaTrueOrderByInicioEmDesc(tampa.getId())
                .ifPresent(posicao -> {
                    posicao.encerrar();
                    posicaoRepositorio.save(posicao);
                });
        registrarPosicao(tampa, tipo, referencia);
    }

    private void registrarPosicao(TampaPorao tampa,
                                  TipoPosicaoTampaPorao tipo,
                                  String referencia) {
        PosicaoTampaPorao posicao = new PosicaoTampaPorao();
        posicao.setTampa(tampa);
        posicao.setTipo(tipo);
        posicao.setReferencia(referencia);
        posicaoRepositorio.save(posicao);
    }

    private EstivagemPlan buscarPlano(Long planId) {
        return planRepositorio.findById(planId)
                .orElseThrow(() -> new EntityNotFoundException("EstivagemPlan não encontrado: " + planId));
    }

    private TampaPorao buscarTampa(Long planId, Long tampaId) {
        TampaPorao tampa = tampaRepositorio.findById(tampaId)
                .orElseThrow(() -> new EntityNotFoundException("Tampa de porão não encontrada: " + tampaId));
        if (!tampa.getEstivagem().getId().equals(planId)) {
            throw new IllegalArgumentException("A tampa não pertence ao plano informado.");
        }
        return tampa;
    }

    private TarefaTampaPorao buscarTarefa(Long planId, Long tarefaId) {
        TarefaTampaPorao tarefa = tarefaRepositorio.findById(tarefaId)
                .orElseThrow(() -> new EntityNotFoundException("Tarefa de tampa não encontrada: " + tarefaId));
        if (!tarefa.getTampa().getEstivagem().getId().equals(planId)) {
            throw new IllegalArgumentException("A tarefa não pertence ao plano informado.");
        }
        return tarefa;
    }

    private TampaResposta toDto(TampaPorao tampa) {
        List<PosicaoTampaPorao> posicoes = posicaoRepositorio.findByTampaIdOrderByInicioEmAsc(tampa.getId());
        List<TarefaTampaPorao> tarefas = tarefaRepositorio.findByTampaIdOrderByCriadoEmAsc(tampa.getId());
        TampaResposta dto = new TampaResposta();
        dto.setId(tampa.getId());
        dto.setCodigo(tampa.getCodigo());
        dto.setEstado(tampa.getEstado());
        dto.setVersao(tampa.getVersao());
        dto.setTarefaEmExecucao(tarefas.stream()
                .anyMatch(item -> item.getStatus() == StatusTarefaTampaPorao.EM_EXECUCAO));
        List<PosicaoResposta> posicoesDto = posicoes.stream()
                .map(this::toPosicaoDto)
                .collect(Collectors.toList());
        dto.setPosicoes(posicoesDto);
        dto.setPosicaoAtual(posicoesDto.stream().filter(PosicaoResposta::isAtiva).findFirst().orElse(null));
        dto.setTarefas(tarefas.stream().map(this::toTarefaDto).collect(Collectors.toList()));
        return dto;
    }

    private PosicaoResposta toPosicaoDto(PosicaoTampaPorao posicao) {
        PosicaoResposta dto = new PosicaoResposta();
        dto.setId(posicao.getId());
        dto.setTipo(posicao.getTipo());
        dto.setReferencia(posicao.getReferencia());
        dto.setAtiva(posicao.isAtiva());
        dto.setInicioEm(posicao.getInicioEm());
        dto.setFimEm(posicao.getFimEm());
        return dto;
    }

    private TarefaResposta toTarefaDto(TarefaTampaPorao tarefa) {
        TarefaResposta dto = new TarefaResposta();
        dto.setId(tarefa.getId());
        dto.setTipo(tarefa.getTipo());
        dto.setStatus(tarefa.getStatus());
        dto.setRecurso(tarefa.getRecurso());
        dto.setOperador(tarefa.getOperador());
        dto.setMotivo(tarefa.getMotivo());
        dto.setPosicaoDestinoTipo(tarefa.getPosicaoDestinoTipo());
        dto.setPosicaoDestinoReferencia(tarefa.getPosicaoDestinoReferencia());
        dto.setDependenciasIds(dependenciaRepositorio.findByTarefaId(tarefa.getId()).stream()
                .map(DependenciaTarefaTampaPorao::getDependencia)
                .map(TarefaTampaPorao::getId)
                .collect(Collectors.toList()));
        dto.setCriadoEm(tarefa.getCriadoEm());
        dto.setIniciadoEm(tarefa.getIniciadoEm());
        dto.setConcluidoEm(tarefa.getConcluidoEm());
        dto.setCanceladoEm(tarefa.getCanceladoEm());
        dto.setVersao(tarefa.getVersao());
        return dto;
    }

    private void aplicarMotivo(TarefaTampaPorao tarefa, ComandoTarefaRequest request) {
        if (request != null && !vazio(request.getMotivo())) {
            tarefa.setMotivo(request.getMotivo());
        }
    }

    private String chavePosicao(SlotNavio slot) {
        return chavePosicao(slot.getBay(), slot.getRowBay(), slot.getTier());
    }

    private String chavePosicao(int bay, int row, int tier) {
        return bay + ":" + row + ":" + tier;
    }

    private String normalizarCodigo(String codigo) {
        return codigo.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizarUsuario(String usuario) {
        return vazio(usuario) ? "SISTEMA" : usuario.trim();
    }

    private boolean vazio(String valor) {
        return valor == null || valor.trim().isEmpty();
    }
}
