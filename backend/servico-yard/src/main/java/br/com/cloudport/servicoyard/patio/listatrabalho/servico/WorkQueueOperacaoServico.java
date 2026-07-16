package br.com.cloudport.servicoyard.patio.listatrabalho.servico;

import br.com.cloudport.contracts.evento.EventoOperacaoPatioV1;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.AtualizacaoPrioridadesWorkInstructionDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.AtualizacaoWorkQueueRecursosDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.ComandoWorkInstructionDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.HistoricoOperacaoPatioRespostaDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.JobListEquipamentoDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.OrdemTrabalhoPatioRespostaDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.WorkInstructionDrillDownDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.WorkQueuePatioRespostaDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.HistoricoOperacaoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.OrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.StatusOrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.WorkQueuePatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.repositorio.HistoricoWorkInstructionRepositorio;
import br.com.cloudport.servicoyard.patio.listatrabalho.repositorio.OrdemTrabalhoPatioRepositorio;
import br.com.cloudport.servicoyard.patio.listatrabalho.repositorio.WorkQueuePatioRepositorio;
import br.com.cloudport.servicoyard.patio.modelo.EquipamentoPatio;
import br.com.cloudport.servicoyard.patio.repositorio.EquipamentoPatioRepositorio;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WorkQueueOperacaoServico {

    private static final Map<StatusOrdemTrabalhoPatio, Set<StatusOrdemTrabalhoPatio>> MATRIZ_ESTADOS = criarMatrizEstados();

    private final WorkQueuePatioRepositorio workQueueRepositorio;
    private final OrdemTrabalhoPatioRepositorio ordemRepositorio;
    private final HistoricoWorkInstructionRepositorio historicoRepositorio;
    private final EquipamentoPatioRepositorio equipamentoRepositorio;
    private final ApplicationEventPublisher eventPublisher;

    public WorkQueueOperacaoServico(WorkQueuePatioRepositorio workQueueRepositorio,
                                     OrdemTrabalhoPatioRepositorio ordemRepositorio,
                                     HistoricoWorkInstructionRepositorio historicoRepositorio,
                                     EquipamentoPatioRepositorio equipamentoRepositorio,
                                     ApplicationEventPublisher eventPublisher) {
        this.workQueueRepositorio = workQueueRepositorio;
        this.ordemRepositorio = ordemRepositorio;
        this.historicoRepositorio = historicoRepositorio;
        this.equipamentoRepositorio = equipamentoRepositorio;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public WorkQueuePatioRespostaDto associarRecursos(Long workQueueId, AtualizacaoWorkQueueRecursosDto dto) {
        WorkQueuePatio fila = buscarFila(workQueueId);
        EquipamentoPatio equipamento = dto.getEquipamentoPatioId() == null
                ? null
                : buscarEquipamento(dto.getEquipamentoPatioId());

        fila.setPorao(dto.getPorao());
        fila.setPlanoGuindasteId(dto.getPlanoGuindasteId());
        fila.setRecursoCaisId(dto.getRecursoCaisId());
        fila.setEquipamentoPatioId(equipamento == null ? null : equipamento.getId());
        fila.setEquipamento(equipamento == null ? null : equipamento.getIdentificador());
        fila.setAtualizadoEm(LocalDateTime.now());
        WorkQueuePatio salva = workQueueRepositorio.save(fila);

        registrarHistorico(fila.getId(), null, "WORK_QUEUE_RECURSOS_ASSOCIADOS", dto.getMotivo(),
                "porao=" + dto.getPorao()
                        + "; planoGuindasteId=" + dto.getPlanoGuindasteId()
                        + "; recursoCaisId=" + dto.getRecursoCaisId()
                        + "; equipamentoPatioId=" + dto.getEquipamentoPatioId()
                        + metadados(dto.getOrigemAcao(), dto.getCorrelationId()),
                usuarioEfetivo(dto.getUsuario()));
        publicarEvento(salva, null, "WORK_QUEUE_RECURSOS_ASSOCIADOS", null, null, dto.getCorrelationId());
        return WorkQueuePatioRespostaDto.deEntidade(salva, listarOrdens(salva.getId()));
    }

    @Transactional
    public OrdemTrabalhoPatioRespostaDto suspender(Long ordemId, ComandoWorkInstructionDto dto) {
        return transicionar(ordemId, StatusOrdemTrabalhoPatio.SUSPENSA, "WORK_INSTRUCTION_SUSPENSA", dto);
    }

    @Transactional
    public OrdemTrabalhoPatioRespostaDto retomar(Long ordemId, ComandoWorkInstructionDto dto) {
        return transicionar(ordemId, StatusOrdemTrabalhoPatio.PENDENTE, "WORK_INSTRUCTION_RETOMADA", dto);
    }

    @Transactional
    public OrdemTrabalhoPatioRespostaDto bloquear(Long ordemId, ComandoWorkInstructionDto dto) {
        return transicionar(ordemId, StatusOrdemTrabalhoPatio.BLOQUEADA, "WORK_INSTRUCTION_BLOQUEADA", dto);
    }

    @Transactional
    public OrdemTrabalhoPatioRespostaDto concluir(Long ordemId, ComandoWorkInstructionDto dto) {
        return transicionar(ordemId, StatusOrdemTrabalhoPatio.CONCLUIDA, "WORK_INSTRUCTION_CONCLUIDA", dto);
    }

    @Transactional
    public OrdemTrabalhoPatioRespostaDto atualizarPrioridades(Long ordemId,
                                                               AtualizacaoPrioridadesWorkInstructionDto dto) {
        OrdemTrabalhoPatio ordem = buscarOrdem(ordemId);
        if (dto.getPrioridadeOperacional() == null && dto.getPrioridadeBusca() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe prioridadeOperacional e/ou prioridadeBusca.");
        }
        if (dto.getPrioridadeOperacional() != null && dto.getPrioridadeOperacional() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A prioridade operacional nao pode ser negativa.");
        }

        if (dto.getPrioridadeOperacional() != null
                && !Objects.equals(ordem.getPrioridadeOperacional(), dto.getPrioridadeOperacional())) {
            Integer anterior = ordem.getPrioridadeOperacional();
            ordem.setPrioridadeOperacional(dto.getPrioridadeOperacional());
            registrarHistorico(ordem.getWorkQueueId(), ordem.getId(), "PRIORIDADE_OPERACIONAL_ALTERADA",
                    dto.getMotivo(), "anterior=" + anterior + "; atual=" + dto.getPrioridadeOperacional()
                            + metadados(dto.getOrigemAcao(), dto.getCorrelationId()),
                    usuarioEfetivo(dto.getUsuario()));
        }
        if (dto.getPrioridadeBusca() != null && ordem.isPrioridadeBusca() != dto.getPrioridadeBusca()) {
            boolean anterior = ordem.isPrioridadeBusca();
            ordem.setPrioridadeBusca(dto.getPrioridadeBusca());
            registrarHistorico(ordem.getWorkQueueId(), ordem.getId(), "PRIORIDADE_FETCH_ALTERADA",
                    dto.getMotivo(), "anterior=" + anterior + "; atual=" + dto.getPrioridadeBusca()
                            + metadados(dto.getOrigemAcao(), dto.getCorrelationId()),
                    usuarioEfetivo(dto.getUsuario()));
        }
        ordem.setAtualizadoEm(LocalDateTime.now());
        OrdemTrabalhoPatio salva = ordemRepositorio.save(ordem);
        if (salva.getWorkQueueId() != null) {
            publicarEvento(buscarFila(salva.getWorkQueueId()), salva.getId(),
                    "WORK_INSTRUCTION_PRIORIDADE_ALTERADA",
                    salva.getStatusOrdem().name(), salva.getStatusOrdem().name(), dto.getCorrelationId());
        }
        return OrdemTrabalhoPatioRespostaDto.deEntidade(salva);
    }

    @Transactional(readOnly = true)
    public Map<String, List<String>> matrizOficialEstados() {
        Map<String, List<String>> matriz = new LinkedHashMap<>();
        MATRIZ_ESTADOS.forEach((origem, destinos) -> matriz.put(origem.name(),
                destinos.stream().map(Enum::name).toList()));
        return matriz;
    }

    @Transactional(readOnly = true)
    public WorkInstructionDrillDownDto drillDown(Long ordemId) {
        OrdemTrabalhoPatio ordem = buscarOrdem(ordemId);
        WorkQueuePatio fila = ordem.getWorkQueueId() == null ? null : buscarFila(ordem.getWorkQueueId());
        List<OrdemTrabalhoPatio> ordensDaFila = fila == null ? List.of() : listarOrdens(fila.getId());
        EquipamentoPatio equipamento = fila == null || fila.getEquipamentoPatioId() == null
                ? null
                : equipamentoRepositorio.findById(fila.getEquipamentoPatioId()).orElse(null);

        WorkInstructionDrillDownDto dto = new WorkInstructionDrillDownDto();
        dto.setWorkInstruction(OrdemTrabalhoPatioRespostaDto.deEntidade(ordem));
        dto.setWorkQueue(fila == null ? null : WorkQueuePatioRespostaDto.deEntidade(fila, ordensDaFila));
        if (equipamento != null) {
            dto.setEquipamentoPatioId(equipamento.getId());
            dto.setEquipamentoIdentificador(equipamento.getIdentificador());
            dto.setEquipamentoTipo(equipamento.getTipoEquipamento().name());
            dto.setEquipamentoStatus(equipamento.getStatusOperacional().name());
        }
        dto.setProximosEstadosPermitidos(MATRIZ_ESTADOS.getOrDefault(ordem.getStatusOrdem(), Set.of())
                .stream().map(Enum::name).toList());
        dto.setMatrizOficialEstados(matrizOficialEstados());
        dto.setAuditoria(historicoRepositorio.findTop100ByOrdemTrabalhoPatioIdOrderByCriadoEmDesc(ordemId)
                .stream().map(HistoricoOperacaoPatioRespostaDto::deEntidade).toList());
        return dto;
    }

    @Transactional(readOnly = true)
    public List<JobListEquipamentoDto> listarJobListsPorEquipamento(Long visitaNavioId) {
        List<WorkQueuePatio> filas = visitaNavioId == null
                ? workQueueRepositorio.findAll()
                : workQueueRepositorio.findByVisitaNavioIdOrderBySequenciaInicialAscCriadoEmAsc(visitaNavioId);
        Map<Long, List<WorkQueuePatio>> filasPorEquipamento = new LinkedHashMap<>();
        for (WorkQueuePatio fila : filas) {
            if (fila.getEquipamentoPatioId() != null) {
                filasPorEquipamento.computeIfAbsent(fila.getEquipamentoPatioId(), ignored -> new ArrayList<>()).add(fila);
            }
        }
        return filasPorEquipamento.entrySet().stream()
                .map(entry -> montarPainelEquipamento(buscarEquipamento(entry.getKey()), entry.getValue()))
                .toList();
    }

    @Transactional(readOnly = true)
    public JobListEquipamentoDto obterJobListEquipamento(Long equipamentoId, Long visitaNavioId) {
        EquipamentoPatio equipamento = buscarEquipamento(equipamentoId);
        List<WorkQueuePatio> filas = (visitaNavioId == null ? workQueueRepositorio.findAll()
                : workQueueRepositorio.findByVisitaNavioIdOrderBySequenciaInicialAscCriadoEmAsc(visitaNavioId))
                .stream()
                .filter(fila -> Objects.equals(equipamentoId, fila.getEquipamentoPatioId()))
                .toList();
        return montarPainelEquipamento(equipamento, filas);
    }

    private OrdemTrabalhoPatioRespostaDto transicionar(Long ordemId,
                                                         StatusOrdemTrabalhoPatio destino,
                                                         String acao,
                                                         ComandoWorkInstructionDto dto) {
        OrdemTrabalhoPatio ordem = buscarOrdem(ordemId);
        StatusOrdemTrabalhoPatio origem = ordem.getStatusOrdem();
        if (!MATRIZ_ESTADOS.getOrDefault(origem, Set.of()).contains(destino)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Transicao de " + origem + " para " + destino + " nao permitida pela matriz oficial.");
        }
        ordem.setStatusOrdem(destino);
        ordem.setAtualizadoEm(LocalDateTime.now());
        ordem.setConcluidoEm(destino == StatusOrdemTrabalhoPatio.CONCLUIDA ? LocalDateTime.now() : null);
        OrdemTrabalhoPatio salva = ordemRepositorio.save(ordem);
        registrarHistorico(ordem.getWorkQueueId(), ordem.getId(), acao, dto.getMotivo(),
                "estadoAnterior=" + origem + "; estadoAtual=" + destino
                        + metadados(dto.getOrigemAcao(), dto.getCorrelationId()),
                usuarioEfetivo(dto.getUsuario()));
        if (salva.getWorkQueueId() != null) {
            publicarEvento(buscarFila(salva.getWorkQueueId()), salva.getId(), acao,
                    origem.name(), destino.name(), dto.getCorrelationId());
        }
        return OrdemTrabalhoPatioRespostaDto.deEntidade(salva);
    }

    private void publicarEvento(WorkQueuePatio fila,
                                 Long ordemId,
                                 String tipoAlteracao,
                                 String statusAnterior,
                                 String statusAtual,
                                 String correlationId) {
        eventPublisher.publishEvent(EventoOperacaoPatioV1.criar(
                fila.getVisitaNavioId(),
                fila.getId(),
                ordemId,
                tipoAlteracao,
                statusAnterior,
                statusAtual,
                correlationId
        ));
    }

    private JobListEquipamentoDto montarPainelEquipamento(EquipamentoPatio equipamento,
                                                            List<WorkQueuePatio> filas) {
        List<WorkQueuePatioRespostaDto> filasDto = filas.stream()
                .map(fila -> WorkQueuePatioRespostaDto.deEntidade(fila, listarOrdens(fila.getId())))
                .toList();
        JobListEquipamentoDto dto = new JobListEquipamentoDto();
        dto.setEquipamentoPatioId(equipamento.getId());
        dto.setEquipamentoIdentificador(equipamento.getIdentificador());
        dto.setEquipamentoTipo(equipamento.getTipoEquipamento().name());
        dto.setEquipamentoStatus(equipamento.getStatusOperacional().name());
        dto.setTotalFilas(filasDto.size());
        dto.setTotalInstrucoes(filasDto.stream().mapToInt(WorkQueuePatioRespostaDto::getTotalOrdens).sum());
        dto.setWorkQueues(filasDto);
        return dto;
    }

    private List<OrdemTrabalhoPatio> listarOrdens(Long workQueueId) {
        return ordemRepositorio
                .findByWorkQueueIdOrderByPrioridadeBuscaDescPrioridadeOperacionalAscSequenciaNavioAscCriadoEmAsc(workQueueId);
    }

    private WorkQueuePatio buscarFila(Long id) {
        return workQueueRepositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Work queue de patio nao encontrada."));
    }

    private OrdemTrabalhoPatio buscarOrdem(Long id) {
        return ordemRepositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Work instruction de patio nao encontrada."));
    }

    private EquipamentoPatio buscarEquipamento(Long id) {
        return equipamentoRepositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "CHE de patio nao encontrado."));
    }

    private void registrarHistorico(Long workQueueId,
                                     Long ordemId,
                                     String acao,
                                     String motivo,
                                     String detalhes,
                                     String usuario) {
        HistoricoOperacaoPatio historico = new HistoricoOperacaoPatio();
        historico.setWorkQueueId(workQueueId);
        historico.setOrdemTrabalhoPatioId(ordemId);
        historico.setAcao(acao);
        historico.setUsuario(usuario);
        historico.setMotivo(limitar(motivo, 500));
        historico.setDetalhes(limitar(detalhes, 2000));
        historico.setCriadoEm(LocalDateTime.now());
        historicoRepositorio.save(historico);
    }

    private String usuarioEfetivo(String usuarioInformado) {
        if (StringUtils.hasText(usuarioInformado)) {
            return usuarioInformado.trim();
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && StringUtils.hasText(authentication.getName())
                ? authentication.getName()
                : "sistema";
    }

    private String metadados(String origemAcao, String correlationId) {
        return "; origemAcao=" + valor(origemAcao, "NAO_INFORMADA")
                + "; correlationId=" + valor(correlationId, "NAO_INFORMADO");
    }

    private String valor(String valor, String padrao) {
        return StringUtils.hasText(valor) ? valor.trim() : padrao;
    }

    private String limitar(String valor, int limite) {
        if (valor == null || valor.length() <= limite) {
            return valor;
        }
        return valor.substring(0, limite);
    }

    private static Map<StatusOrdemTrabalhoPatio, Set<StatusOrdemTrabalhoPatio>> criarMatrizEstados() {
        Map<StatusOrdemTrabalhoPatio, Set<StatusOrdemTrabalhoPatio>> matriz =
                new EnumMap<>(StatusOrdemTrabalhoPatio.class);
        matriz.put(StatusOrdemTrabalhoPatio.PENDENTE, estados(
                StatusOrdemTrabalhoPatio.EM_EXECUCAO,
                StatusOrdemTrabalhoPatio.BLOQUEADA,
                StatusOrdemTrabalhoPatio.SUSPENSA,
                StatusOrdemTrabalhoPatio.CANCELADA));
        matriz.put(StatusOrdemTrabalhoPatio.EM_EXECUCAO, estados(
                StatusOrdemTrabalhoPatio.CONCLUIDA,
                StatusOrdemTrabalhoPatio.BLOQUEADA,
                StatusOrdemTrabalhoPatio.SUSPENSA,
                StatusOrdemTrabalhoPatio.CANCELADA));
        matriz.put(StatusOrdemTrabalhoPatio.BLOQUEADA, estados(
                StatusOrdemTrabalhoPatio.PENDENTE,
                StatusOrdemTrabalhoPatio.SUSPENSA,
                StatusOrdemTrabalhoPatio.CANCELADA));
        matriz.put(StatusOrdemTrabalhoPatio.SUSPENSA, estados(
                StatusOrdemTrabalhoPatio.PENDENTE,
                StatusOrdemTrabalhoPatio.EM_EXECUCAO,
                StatusOrdemTrabalhoPatio.BLOQUEADA,
                StatusOrdemTrabalhoPatio.CANCELADA));
        matriz.put(StatusOrdemTrabalhoPatio.CONCLUIDA, Set.of());
        matriz.put(StatusOrdemTrabalhoPatio.CANCELADA, Set.of());
        return Map.copyOf(matriz);
    }

    private static Set<StatusOrdemTrabalhoPatio> estados(StatusOrdemTrabalhoPatio... estados) {
        return Set.copyOf(new LinkedHashSet<>(List.of(estados)));
    }
}
