package br.com.cloudport.servicoyard.patio.listatrabalho.servico;

import br.com.cloudport.servicoyard.patio.listatrabalho.dto.AtualizacaoPrioridadesWorkInstructionDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.AtualizacaoWorkQueueEquipamentoDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.AtualizacaoWorkQueuePowDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.AtualizacaoWorkQueueRecursosDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.ComandoMotivadoDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.ComandoWorkInstructionDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.DispatchWorkQueueDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.EquipamentoOperacionalDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.HistoricoOperacaoPatioRespostaDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.JobListEquipamentoDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.OrdemTrabalhoPatioRespostaDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.ResultadoDispatchWorkQueueDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.WorkInstructionDrillDownDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.WorkQueuePatioRespostaDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.WorkQueueValidacaoPlanoDto;
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

    private static final Map<StatusOrdemTrabalhoPatio, Set<StatusOrdemTrabalhoPatio>> MATRIZ_ESTADOS =
            criarMatrizEstados();

    private final WorkQueuePatioRepositorio workQueueRepositorio;
    private final OrdemTrabalhoPatioRepositorio ordemRepositorio;
    private final HistoricoWorkInstructionRepositorio historicoRepositorio;
    private final EquipamentoPatioRepositorio equipamentoRepositorio;

    public WorkQueueOperacaoServico(
            WorkQueuePatioRepositorio workQueueRepositorio,
            OrdemTrabalhoPatioRepositorio ordemRepositorio,
            HistoricoWorkInstructionRepositorio historicoRepositorio,
            EquipamentoPatioRepositorio equipamentoRepositorio
    ) {
        this.workQueueRepositorio = workQueueRepositorio;
        this.ordemRepositorio = ordemRepositorio;
        this.historicoRepositorio = historicoRepositorio;
        this.equipamentoRepositorio = equipamentoRepositorio;
    }

    @Transactional
    public WorkQueuePatioRespostaDto associarRecursos(Long workQueueId, AtualizacaoWorkQueueRecursosDto dto) {
        if (dto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O comando de recursos operacionais e obrigatorio.");
        }
        exigirMotivo(dto.getMotivo());
        WorkQueuePatio fila = buscarFila(workQueueId);

        if (dto.getPorao() != null) {
            if (dto.getPorao() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O porao deve ser maior que zero.");
            }
            fila.setPorao(dto.getPorao());
        }
        if (dto.getPlanoGuindasteId() != null) {
            exigirIdentificadorPositivo(dto.getPlanoGuindasteId(), "Plano de guindaste");
            fila.setPlanoGuindasteId(dto.getPlanoGuindasteId());
        }
        if (dto.getRecursoCaisId() != null) {
            exigirIdentificadorPositivo(dto.getRecursoCaisId(), "Recurso de cais");
            fila.setRecursoCaisId(dto.getRecursoCaisId());
        }
        if (dto.getPow() != null) fila.setPow(normalizarOpcional(dto.getPow()));
        if (dto.getPoolOperacional() != null) fila.setPoolOperacional(normalizarOpcional(dto.getPoolOperacional()));
        if (dto.getEquipamentoPatioId() != null) {
            EquipamentoPatio equipamento = buscarEquipamento(dto.getEquipamentoPatioId());
            validarEquipamentoOperacional(equipamento);
            fila.setEquipamentoPatioId(equipamento.getId());
            fila.setEquipamento(equipamento.getIdentificador());
        }

        fila.setAtualizadoEm(LocalDateTime.now());
        WorkQueuePatio salva = workQueueRepositorio.save(fila);
        registrarHistorico(
                fila.getId(),
                null,
                "WORK_QUEUE_RECURSOS_ASSOCIADOS",
                dto.getMotivo(),
                "porao=" + fila.getPorao()
                        + "; planoGuindasteId=" + fila.getPlanoGuindasteId()
                        + "; recursoCaisId=" + fila.getRecursoCaisId()
                        + "; pow=" + valor(fila.getPow(), "SEM_POW")
                        + "; pool=" + valor(fila.getPoolOperacional(), "SEM_POOL")
                        + "; equipamentoPatioId=" + fila.getEquipamentoPatioId()
                        + metadados(dto.getOrigemAcao(), dto.getCorrelationId()),
                usuarioEfetivo(dto.getUsuario()));
        return WorkQueuePatioRespostaDto.deEntidade(salva, listarOrdens(salva.getId()));
    }

    @Transactional
    public WorkQueuePatioRespostaDto atualizarCoberturaLegada(Long workQueueId, AtualizacaoWorkQueuePowDto dto) {
        if (dto == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O comando de cobertura e obrigatorio.");
        AtualizacaoWorkQueueRecursosDto comando = new AtualizacaoWorkQueueRecursosDto();
        comando.setPow(dto.getPow());
        comando.setPoolOperacional(dto.getPoolOperacional());
        comando.setMotivo(dto.getMotivo());
        comando.setUsuario(dto.getUsuario());
        comando.setOrigemAcao(dto.getOrigemAcao());
        comando.setCorrelationId(dto.getCorrelationId());
        return associarRecursos(workQueueId, comando);
    }

    @Transactional
    public WorkQueuePatioRespostaDto atualizarEquipamentoLegado(Long workQueueId, AtualizacaoWorkQueueEquipamentoDto dto) {
        if (dto == null || !StringUtils.hasText(dto.getEquipamento())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o identificador de um CHE cadastrado no Yard.");
        }
        EquipamentoPatio equipamento = equipamentoRepositorio.findByIdentificador(dto.getEquipamento().trim())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "CHE informado nao existe no cadastro operacional do Yard."));
        AtualizacaoWorkQueueRecursosDto comando = new AtualizacaoWorkQueueRecursosDto();
        comando.setEquipamentoPatioId(equipamento.getId());
        comando.setMotivo(dto.getMotivo());
        comando.setUsuario(dto.getUsuario());
        comando.setOrigemAcao(dto.getOrigemAcao());
        comando.setCorrelationId(dto.getCorrelationId());
        return associarRecursos(workQueueId, comando);
    }

    @Transactional
    public WorkQueuePatioRespostaDto alterarStatus(
            Long workQueueId,
            StatusWorkQueuePatio destino,
            ComandoMotivadoDto comando
    ) {
        if (comando == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O comando motivado e obrigatorio.");
        exigirMotivo(comando.getMotivo());
        WorkQueuePatio fila = buscarFila(workQueueId);
        StatusWorkQueuePatio origem = fila.getStatus();
        fila.setStatus(destino);
        fila.setAtualizadoEm(LocalDateTime.now());
        WorkQueuePatio salva = workQueueRepositorio.save(fila);
        registrarHistorico(
                fila.getId(),
                null,
                destino == StatusWorkQueuePatio.ATIVA ? "WORK_QUEUE_ATIVADA" : "WORK_QUEUE_DESATIVADA",
                comando.getMotivo(),
                "estadoAnterior=" + origem + "; estadoAtual=" + destino
                        + metadados(comando.getOrigemAcao(), comando.getCorrelationId()),
                usuarioEfetivo(comando.getUsuario()));
        return WorkQueuePatioRespostaDto.deEntidade(salva, listarOrdens(salva.getId()));
    }

    @Transactional
    public ResultadoDispatchWorkQueueDto despachar(Long workQueueId, DispatchWorkQueueDto comando) {
        DispatchWorkQueueDto dto = comando == null ? new DispatchWorkQueueDto() : comando;
        exigirMotivo(dto.motivoEfetivo());
        WorkQueuePatio fila = buscarFila(workQueueId);
        EquipamentoPatio equipamento = validarCoberturaOperacional(fila);
        List<OrdemTrabalhoPatio> ordens = listarOrdens(fila.getId());
        if (ordens.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A work queue nao possui job list para dispatch.");
        }

        Set<Long> selecionadas = CollectionUtils.isEmpty(dto.getOrdemIds())
                ? Set.of()
                : dto.getOrdemIds().stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (!selecionadas.isEmpty()) {
            Set<Long> idsDaFila = ordens.stream().map(OrdemTrabalhoPatio::getId).collect(Collectors.toSet());
            if (!idsDaFila.containsAll(selecionadas)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Uma ou mais work instructions selecionadas nao pertencem a work queue.");
            }
        }

        int limite = dto.limiteOrdensEfetivo();
        int despachadas = 0;
        int ignoradas = 0;
        for (OrdemTrabalhoPatio ordem : ordens) {
            if (!selecionadas.isEmpty() && !selecionadas.contains(ordem.getId())) continue;
            if (despachadas >= limite) {
                ignoradas++;
                continue;
            }
            if (dto.somentePendentesEfetivo() && ordem.getStatusOrdem() != StatusOrdemTrabalhoPatio.PENDENTE) {
                ignoradas++;
                continue;
            }
            if (!permitido(ordem.getStatusOrdem(), StatusOrdemTrabalhoPatio.EM_EXECUCAO)) {
                ignoradas++;
                continue;
            }
            transicionarEntidade(
                    ordem,
                    StatusOrdemTrabalhoPatio.EM_EXECUCAO,
                    "WORK_INSTRUCTION_DESPACHADA",
                    dto.motivoEfetivo(),
                    dto.getOrigemAcao(),
                    dto.getCorrelationId(),
                    dto.usuarioEfetivo());
            despachadas++;
        }

        if (despachadas == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Nenhuma work instruction elegivel foi despachada pela matriz oficial de estados.");
        }
        registrarHistorico(
                fila.getId(),
                null,
                "WORK_QUEUE_DESPACHADA",
                dto.motivoEfetivo(),
                "despachadas=" + despachadas
                        + "; ignoradas=" + ignoradas
                        + "; equipamentoPatioId=" + equipamento.getId()
                        + "; pow=" + fila.getPow()
                        + "; pool=" + fila.getPoolOperacional()
                        + metadados(dto.getOrigemAcao(), dto.getCorrelationId()),
                dto.usuarioEfetivo());
        return new ResultadoDispatchWorkQueueDto(
                fila.getId(),
                despachadas,
                ignoradas,
                "Dispatch executado com recursos operacionais validados.",
                listarOrdens(fila.getId()).stream().map(OrdemTrabalhoPatioRespostaDto::deEntidade).toList());
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
    public OrdemTrabalhoPatioRespostaDto atualizarPrioridades(
            Long ordemId,
            AtualizacaoPrioridadesWorkInstructionDto dto
    ) {
        OrdemTrabalhoPatio ordem = buscarOrdem(ordemId);
        if (dto.getPrioridadeOperacional() == null && dto.getPrioridadeBusca() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe prioridadeOperacional e/ou prioridadeBusca.");
        }
        if (dto.getPrioridadeOperacional() != null && dto.getPrioridadeOperacional() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A prioridade operacional nao pode ser negativa.");
        }
        exigirMotivo(dto.getMotivo());

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
        MATRIZ_ESTADOS.forEach((origem, destinos) -> matriz.put(
                origem.name(),
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

    @Transactional(readOnly = true)
    public List<EquipamentoOperacionalDto> listarEquipamentos() {
        return equipamentoRepositorio.findAllByOrderByTipoEquipamentoAscIdentificadorAsc()
                .stream().map(EquipamentoOperacionalDto::de).toList();
    }

    @Transactional(readOnly = true)
    public List<WorkQueueValidacaoPlanoDto> consultarValidacaoPlano(Long visitaNavioId) {
        if (visitaNavioId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A visita de navio e obrigatoria.");
        }
        return workQueueRepositorio.findByVisitaNavioIdOrderBySequenciaInicialAscCriadoEmAsc(visitaNavioId)
                .stream().map(this::montarValidacaoPlano).toList();
    }

    private WorkQueueValidacaoPlanoDto montarValidacaoPlano(WorkQueuePatio fila) {
        EquipamentoPatio equipamento = fila.getEquipamentoPatioId() == null
                ? null
                : equipamentoRepositorio.findById(fila.getEquipamentoPatioId()).orElse(null);
        List<OrdemTrabalhoPatio> ordens = listarOrdens(fila.getId());
        int dispatchaveis = (int) ordens.stream()
                .filter(ordem -> permitido(ordem.getStatusOrdem(), StatusOrdemTrabalhoPatio.EM_EXECUCAO))
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
                        && StringUtils.hasText(fila.getPow())
                        && StringUtils.hasText(fila.getPoolOperacional())
                        && fila.getPorao() != null
                        && fila.getRecursoCaisId() != null
                        && equipamento != null
                        && equipamento.getStatusOperacional() == StatusEquipamento.OPERACIONAL
                        && dispatchaveis > 0);
        return dto;
    }

    private OrdemTrabalhoPatioRespostaDto transicionar(
            Long ordemId,
            StatusOrdemTrabalhoPatio destino,
            String acao,
            ComandoWorkInstructionDto dto
    ) {
        if (dto == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O comando de transicao e obrigatorio.");
        exigirMotivo(dto.getMotivo());
        OrdemTrabalhoPatio ordem = buscarOrdem(ordemId);
        return OrdemTrabalhoPatioRespostaDto.deEntidade(transicionarEntidade(
                ordem,
                destino,
                acao,
                dto.getMotivo(),
                dto.getOrigemAcao(),
                dto.getCorrelationId(),
                usuarioEfetivo(dto.getUsuario())));
    }

    private OrdemTrabalhoPatio transicionarEntidade(
            OrdemTrabalhoPatio ordem,
            StatusOrdemTrabalhoPatio destino,
            String acao,
            String motivo,
            String origemAcao,
            String correlationId,
            String usuario
    ) {
        StatusOrdemTrabalhoPatio origem = ordem.getStatusOrdem();
        if (!permitido(origem, destino)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Transicao de " + origem + " para " + destino + " nao permitida pela matriz oficial.");
        }
        ordem.setStatusOrdem(destino);
        ordem.setAtualizadoEm(LocalDateTime.now());
        ordem.setConcluidoEm(destino == StatusOrdemTrabalhoPatio.CONCLUIDA ? LocalDateTime.now() : null);
        OrdemTrabalhoPatio salva = ordemRepositorio.save(ordem);
        registrarHistorico(
                ordem.getWorkQueueId(),
                ordem.getId(),
                acao,
                motivo,
                "estadoAnterior=" + origem + "; estadoAtual=" + destino
                        + metadados(origemAcao, correlationId),
                usuario);
        return salva;
    }

    private boolean permitido(StatusOrdemTrabalhoPatio origem, StatusOrdemTrabalhoPatio destino) {
        return MATRIZ_ESTADOS.getOrDefault(origem, Set.of()).contains(destino);
    }

    private EquipamentoPatio validarCoberturaOperacional(WorkQueuePatio fila) {
        if (fila.getStatus() != StatusWorkQueuePatio.ATIVA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A work queue precisa estar ativa.");
        }
        if (!StringUtils.hasText(fila.getPow())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A work queue nao possui cobertura de POW.");
        }
        if (!StringUtils.hasText(fila.getPoolOperacional())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A work queue nao possui pool operacional.");
        }
        if (fila.getEquipamentoPatioId() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A work queue nao possui CHE real associado pelo identificador do cadastro.");
        }
        EquipamentoPatio equipamento = buscarEquipamento(fila.getEquipamentoPatioId());
        validarEquipamentoOperacional(equipamento);
        if (!Objects.equals(fila.getEquipamento(), equipamento.getIdentificador())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O identificador textual da fila diverge do CHE real associado.");
        }
        return equipamento;
    }

    private void validarEquipamentoOperacional(EquipamentoPatio equipamento) {
        if (equipamento.getStatusOperacional() != StatusEquipamento.OPERACIONAL) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O CHE selecionado nao esta operacional.");
        }
    }

    private JobListEquipamentoDto montarPainelEquipamento(
            EquipamentoPatio equipamento,
            List<WorkQueuePatio> filas
    ) {
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

    private void registrarHistorico(
            Long workQueueId,
            Long ordemId,
            String acao,
            String motivo,
            String detalhes,
            String usuario
    ) {
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

    private void exigirMotivo(String motivo) {
        if (!StringUtils.hasText(motivo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O motivo da operacao e obrigatorio.");
        }
    }

    private void exigirIdentificadorPositivo(Long id, String nome) {
        if (id <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                nome + " deve possuir identificador valido.");
    }

    private String usuarioEfetivo(String usuarioInformado) {
        if (StringUtils.hasText(usuarioInformado)) return usuarioInformado.trim();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && StringUtils.hasText(authentication.getName())
                ? authentication.getName()
                : "sistema";
    }

    private String metadados(String origemAcao, String correlationId) {
        return "; origemAcao=" + valor(origemAcao, "NAO_INFORMADA")
                + "; correlationId=" + valor(correlationId, "NAO_INFORMADO");
    }

    private String normalizarOpcional(String entrada) {
        return StringUtils.hasText(entrada) ? entrada.trim().toUpperCase(Locale.ROOT) : null;
    }

    private String valor(String entrada, String padrao) {
        return StringUtils.hasText(entrada) ? entrada.trim() : padrao;
    }

    private String limitar(String entrada, int limite) {
        if (entrada == null || entrada.length() <= limite) return entrada;
        return entrada.substring(0, limite);
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
