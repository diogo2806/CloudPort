package br.com.cloudport.servicoyard.patio.listatrabalho.servico;

import br.com.cloudport.servicoyard.patio.listatrabalho.dto.AtualizacaoPrioridadesWorkInstructionDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.AtualizacaoWorkQueueEquipamentoDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.AtualizacaoWorkQueuePowDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.AtualizacaoWorkQueueRecursosDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.ComandoWorkInstructionDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.DispatchWorkQueueDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.HistoricoOperacaoPatioRespostaDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.JobListEquipamentoDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.OrdemTrabalhoPatioRespostaDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.ResultadoDispatchWorkQueueDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.WorkInstructionDrillDownDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.WorkQueuePatioRespostaDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.HistoricoOperacaoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.OrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.StatusOrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.StatusWorkQueuePatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.WorkQueuePatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.repositorio.HistoricoWorkInstructionRepositorio;
import br.com.cloudport.servicoyard.patio.listatrabalho.repositorio.OrdemTrabalhoPatioRepositorio;
import br.com.cloudport.servicoyard.patio.listatrabalho.repositorio.WorkQueuePatioRepositorio;
import br.com.cloudport.servicoyard.patio.modelo.EquipamentoPatio;
import br.com.cloudport.servicoyard.patio.modelo.StatusEquipamento;
import br.com.cloudport.servicoyard.patio.repositorio.EquipamentoPatioRepositorio;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WorkQueueOperacaoServico {

    private static final Map<StatusOrdemTrabalhoPatio, Set<StatusOrdemTrabalhoPatio>> MATRIZ_ESTADOS = criarMatrizEstados();

    private final WorkQueuePatioRepositorio workQueueRepositorio;
    private final OrdemTrabalhoPatioRepositorio ordemRepositorio;
    private final HistoricoWorkInstructionRepositorio historicoRepositorio;
    private final EquipamentoPatioRepositorio equipamentoRepositorio;

    public WorkQueueOperacaoServico(WorkQueuePatioRepositorio workQueueRepositorio,
                                     OrdemTrabalhoPatioRepositorio ordemRepositorio,
                                     HistoricoWorkInstructionRepositorio historicoRepositorio,
                                     EquipamentoPatioRepositorio equipamentoRepositorio) {
        this.workQueueRepositorio = workQueueRepositorio;
        this.ordemRepositorio = ordemRepositorio;
        this.historicoRepositorio = historicoRepositorio;
        this.equipamentoRepositorio = equipamentoRepositorio;
    }

    @Transactional
    public WorkQueuePatioRespostaDto associarRecursos(Long workQueueId, AtualizacaoWorkQueueRecursosDto dto) {
        if (dto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Os recursos operacionais devem ser informados.");
        }
        WorkQueuePatio fila = buscarFila(workQueueId);
        EquipamentoPatio equipamento = dto.getEquipamentoPatioId() == null
                ? null
                : buscarEquipamento(dto.getEquipamentoPatioId());

        fila.setPorao(dto.getPorao());
        fila.setPlanoGuindasteId(dto.getPlanoGuindasteId());
        fila.setRecursoCaisId(dto.getRecursoCaisId());
        if (dto.getPow() != null) {
            fila.setPow(normalizarOpcional(dto.getPow()));
        }
        if (dto.getPoolOperacional() != null) {
            fila.setPoolOperacional(normalizarOpcional(dto.getPoolOperacional()));
        }
        fila.setEquipamentoPatioId(equipamento == null ? null : equipamento.getId());
        fila.setEquipamento(equipamento == null ? null : equipamento.getIdentificador());
        fila.setAtualizadoEm(LocalDateTime.now());
        WorkQueuePatio salva = workQueueRepositorio.save(fila);

        registrarHistorico(fila.getId(), null, "WORK_QUEUE_RECURSOS_ASSOCIADOS", dto.getMotivo(),
                "porao=" + dto.getPorao()
                        + "; planoGuindasteId=" + dto.getPlanoGuindasteId()
                        + "; recursoCaisId=" + dto.getRecursoCaisId()
                        + "; pow=" + valor(fila.getPow(), "SEM_POW")
                        + "; poolOperacional=" + valor(fila.getPoolOperacional(), "SEM_POOL")
                        + "; equipamentoPatioId=" + dto.getEquipamentoPatioId()
                        + metadados(dto.getOrigemAcao(), dto.getCorrelationId()),
                usuarioEfetivo(dto.getUsuario()));
        return WorkQueuePatioRespostaDto.deEntidade(salva, listarOrdens(salva.getId()));
    }

    @Transactional
    public WorkQueuePatioRespostaDto atualizarPow(Long workQueueId, AtualizacaoWorkQueuePowDto dto) {
        if (dto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "POW e pool operacional devem ser informados.");
        }
        WorkQueuePatio fila = buscarFila(workQueueId);
        fila.setPow(normalizarOpcional(dto.getPow()));
        fila.setPoolOperacional(normalizarOpcional(dto.getPoolOperacional()));
        fila.setAtualizadoEm(LocalDateTime.now());
        WorkQueuePatio salva = workQueueRepositorio.save(fila);
        registrarHistorico(fila.getId(), null, "WORK_QUEUE_COBERTURA_ATUALIZADA", dto.getMotivo(),
                "pow=" + valor(fila.getPow(), "SEM_POW")
                        + "; poolOperacional=" + valor(fila.getPoolOperacional(), "SEM_POOL")
                        + metadados(dto.getOrigemAcao(), dto.getCorrelationId()),
                usuarioEfetivo(dto.getUsuario()));
        return WorkQueuePatioRespostaDto.deEntidade(salva, listarOrdens(salva.getId()));
    }

    @Transactional
    public WorkQueuePatioRespostaDto atualizarEquipamento(Long workQueueId, AtualizacaoWorkQueueEquipamentoDto dto) {
        if (dto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O equipamento deve ser informado.");
        }
        WorkQueuePatio fila = buscarFila(workQueueId);
        EquipamentoPatio equipamento = StringUtils.hasText(dto.getEquipamento())
                ? buscarEquipamentoPorIdentificador(dto.getEquipamento())
                : null;
        fila.setEquipamentoPatioId(equipamento == null ? null : equipamento.getId());
        fila.setEquipamento(equipamento == null ? null : equipamento.getIdentificador());
        fila.setAtualizadoEm(LocalDateTime.now());
        WorkQueuePatio salva = workQueueRepositorio.save(fila);
        registrarHistorico(fila.getId(), null, "WORK_QUEUE_EQUIPAMENTO_REAL_ATUALIZADO", dto.getMotivo(),
                "equipamentoPatioId=" + fila.getEquipamentoPatioId()
                        + "; equipamento=" + valor(fila.getEquipamento(), "SEM_EQUIPAMENTO")
                        + metadados(dto.getOrigemAcao(), dto.getCorrelationId()),
                usuarioEfetivo(dto.getUsuario()));
        return WorkQueuePatioRespostaDto.deEntidade(salva, listarOrdens(salva.getId()));
    }

    @Transactional
    public ResultadoDispatchWorkQueueDto despachar(Long workQueueId, DispatchWorkQueueDto dto) {
        DispatchWorkQueueDto comando = dto == null ? new DispatchWorkQueueDto() : dto;
        String motivo = comando.motivoEfetivo();
        if (!StringUtils.hasText(motivo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O motivo do dispatch deve ser informado.");
        }

        WorkQueuePatio fila = buscarFila(workQueueId);
        EquipamentoPatio equipamento = validarCoberturaParaDispatch(fila);
        Set<Long> idsSelecionados = CollectionUtils.isEmpty(comando.getOrdemIds())
                ? Set.of()
                : comando.getOrdemIds().stream().filter(Objects::nonNull).collect(Collectors.toSet());
        int limite = comando.limiteOrdensEfetivo();
        int despachadas = 0;
        int ignoradas = 0;

        for (OrdemTrabalhoPatio ordem : listarOrdens(fila.getId())) {
            if (despachadas >= limite) {
                ignoradas++;
                continue;
            }
            if (!idsSelecionados.isEmpty() && !idsSelecionados.contains(ordem.getId())) {
                continue;
            }
            if (comando.somentePendentesEfetivo()
                    && ordem.getStatusOrdem() != StatusOrdemTrabalhoPatio.PENDENTE) {
                ignoradas++;
                continue;
            }
            if (!MATRIZ_ESTADOS.getOrDefault(ordem.getStatusOrdem(), Set.of())
                    .contains(StatusOrdemTrabalhoPatio.EM_EXECUCAO)) {
                ignoradas++;
                continue;
            }
            transicionar(ordem,
                    StatusOrdemTrabalhoPatio.EM_EXECUCAO,
                    "WORK_INSTRUCTION_DESPACHADA",
                    motivo,
                    comando.usuarioEfetivo(),
                    comando.getOrigemAcao(),
                    comando.getCorrelationId(),
                    "workQueueId=" + fila.getId()
                            + "; equipamentoPatioId=" + equipamento.getId()
                            + "; pow=" + fila.getPow()
                            + "; poolOperacional=" + fila.getPoolOperacional()
                            + "; planoGuindasteId=" + fila.getPlanoGuindasteId()
                            + "; recursoCaisId=" + fila.getRecursoCaisId());
            despachadas++;
        }

        registrarHistorico(fila.getId(), null, "WORK_QUEUE_DESPACHADA", motivo,
                "despachadas=" + despachadas
                        + "; ignoradas=" + ignoradas
                        + "; equipamentoPatioId=" + equipamento.getId()
                        + "; pow=" + fila.getPow()
                        + "; poolOperacional=" + fila.getPoolOperacional()
                        + "; planoGuindasteId=" + fila.getPlanoGuindasteId()
                        + "; recursoCaisId=" + fila.getRecursoCaisId()
                        + metadados(comando.getOrigemAcao(), comando.getCorrelationId()),
                comando.usuarioEfetivo());

        return new ResultadoDispatchWorkQueueDto(
                fila.getId(),
                despachadas,
                ignoradas,
                "Dispatch executado para a work queue " + fila.getIdentificador() + ".",
                listarOrdens(fila.getId()).stream().map(OrdemTrabalhoPatioRespostaDto::deEntidade).toList()
        );
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
    public OrdemTrabalhoPatioRespostaDto resetar(Long ordemId, ComandoWorkInstructionDto dto) {
        return transicionar(ordemId, StatusOrdemTrabalhoPatio.PENDENTE, "WORK_INSTRUCTION_RESETADA", dto);
    }

    @Transactional
    public OrdemTrabalhoPatioRespostaDto cancelar(Long ordemId, ComandoWorkInstructionDto dto) {
        return transicionar(ordemId, StatusOrdemTrabalhoPatio.CANCELADA, "WORK_INSTRUCTION_CANCELADA", dto);
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
        return OrdemTrabalhoPatioRespostaDto.deEntidade(ordemRepositorio.save(ordem));
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
        if (dto == null || !StringUtils.hasText(dto.getMotivo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O motivo da transicao deve ser informado.");
        }
        OrdemTrabalhoPatio ordem = buscarOrdem(ordemId);
        return transicionar(ordem,
                destino,
                acao,
                dto.getMotivo(),
                usuarioEfetivo(dto.getUsuario()),
                dto.getOrigemAcao(),
                dto.getCorrelationId(),
                null);
    }

    private OrdemTrabalhoPatioRespostaDto transicionar(OrdemTrabalhoPatio ordem,
                                                        StatusOrdemTrabalhoPatio destino,
                                                        String acao,
                                                        String motivo,
                                                        String usuario,
                                                        String origemAcao,
                                                        String correlationId,
                                                        String detalhesAdicionais) {
        StatusOrdemTrabalhoPatio origem = ordem.getStatusOrdem();
        if (!MATRIZ_ESTADOS.getOrDefault(origem, Set.of()).contains(destino)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Transicao de " + origem + " para " + destino + " nao permitida pela matriz oficial.");
        }
        ordem.setStatusOrdem(destino);
        ordem.setAtualizadoEm(LocalDateTime.now());
        ordem.setConcluidoEm(destino == StatusOrdemTrabalhoPatio.CONCLUIDA ? LocalDateTime.now() : null);
        OrdemTrabalhoPatio salva = ordemRepositorio.save(ordem);
        String detalhes = "estadoAnterior=" + origem + "; estadoAtual=" + destino;
        if (StringUtils.hasText(detalhesAdicionais)) {
            detalhes += "; " + detalhesAdicionais;
        }
        registrarHistorico(ordem.getWorkQueueId(), ordem.getId(), acao, motivo,
                detalhes + metadados(origemAcao, correlationId), usuario);
        return OrdemTrabalhoPatioRespostaDto.deEntidade(salva);
    }

    private EquipamentoPatio validarCoberturaParaDispatch(WorkQueuePatio fila) {
        if (fila.getStatus() != StatusWorkQueuePatio.ATIVA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A work queue precisa estar ativa para dispatch.");
        }
        List<String> recursosAusentes = new ArrayList<>();
        if (!StringUtils.hasText(fila.getPow())) {
            recursosAusentes.add("POW");
        }
        if (!StringUtils.hasText(fila.getPoolOperacional())) {
            recursosAusentes.add("pool operacional");
        }
        if (fila.getPlanoGuindasteId() == null) {
            recursosAusentes.add("plano de guindaste");
        }
        if (fila.getRecursoCaisId() == null) {
            recursosAusentes.add("recurso de cais");
        }
        if (fila.getEquipamentoPatioId() == null) {
            recursosAusentes.add("CHE real");
        }
        if (!recursosAusentes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Dispatch bloqueado por cobertura operacional incompleta: "
                            + String.join(", ", recursosAusentes) + ".");
        }

        EquipamentoPatio equipamento = buscarEquipamento(fila.getEquipamentoPatioId());
        if (equipamento.getStatusOperacional() != StatusEquipamento.OPERACIONAL) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O CHE " + equipamento.getIdentificador() + " nao esta operacional.");
        }
        if (!StringUtils.hasText(fila.getEquipamento())
                || !equipamento.getIdentificador().equalsIgnoreCase(fila.getEquipamento().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O vinculo textual do equipamento diverge do CHE real associado. Reassocie os recursos operacionais.");
        }
        return equipamento;
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

    private EquipamentoPatio buscarEquipamentoPorIdentificador(String identificador) {
        String normalizado = normalizarOpcional(identificador);
        return equipamentoRepositorio.findByIdentificador(normalizado)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "CHE de patio nao encontrado para o identificador informado."));
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
        historico.setUsuario(StringUtils.hasText(usuario) ? usuario.trim() : "sistema");
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

    private String normalizarOpcional(String valor) {
        return StringUtils.hasText(valor) ? valor.trim().toUpperCase(Locale.ROOT) : null;
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
                StatusOrdemTrabalhoPatio.PENDENTE,
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
