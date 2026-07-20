package br.com.cloudport.servicoyard.patio.dispatch;

import br.com.cloudport.servicoyard.inventario.modelo.UnidadeInventario;
import br.com.cloudport.servicoyard.inventario.repositorio.UnidadeInventarioRepositorio;
import br.com.cloudport.servicoyard.patio.dispatch.DispatchDtos.AutoDispatchRequest;
import br.com.cloudport.servicoyard.patio.dispatch.DispatchDtos.Auxiliar;
import br.com.cloudport.servicoyard.patio.dispatch.DispatchDtos.Configuracao;
import br.com.cloudport.servicoyard.patio.dispatch.DispatchDtos.Decisao;
import br.com.cloudport.servicoyard.patio.dispatch.DispatchDtos.Ranking;
import br.com.cloudport.servicoyard.patio.dispatch.DispatchDtos.Resumo;
import br.com.cloudport.servicoyard.patio.dispatch.DispatchDtos.Rota;
import br.com.cloudport.servicoyard.patio.dispatch.DispatchEnums.ModoDispatch;
import br.com.cloudport.servicoyard.patio.dispatch.DispatchScheduler.Avaliacao;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.DispatchWorkQueueDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.OrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.StatusOrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.StatusWorkQueuePatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.WorkQueuePatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.repositorio.OrdemTrabalhoPatioRepositorio;
import br.com.cloudport.servicoyard.patio.listatrabalho.repositorio.WorkQueuePatioRepositorio;
import br.com.cloudport.servicoyard.patio.listatrabalho.servico.WorkQueueOperacaoServico;
import br.com.cloudport.servicoyard.patio.modelo.EquipamentoPatio;
import br.com.cloudport.servicoyard.patio.modelo.StatusEquipamento;
import br.com.cloudport.servicoyard.patio.repositorio.EquipamentoPatioRepositorio;
import br.com.cloudport.servicoyard.scheduler.servico.ValidacaoPlanejamentoDispatchServico;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DispatchDinamicoServico {

    private static final Set<String> FASES_ATIVAS = Set.of(
            "OPERACAO", "EM_OPERACAO", "LIBERADA", "ATRACADO", "DESCARGA", "EMBARQUE");
    private static final Set<StatusOrdemTrabalhoPatio> ESTADOS_ELEGIVEIS = Set.of(
            StatusOrdemTrabalhoPatio.PENDENTE,
            StatusOrdemTrabalhoPatio.SUSPENSA);

    private final WorkQueuePatioRepositorio filaRepositorio;
    private final OrdemTrabalhoPatioRepositorio ordemRepositorio;
    private final EquipamentoPatioRepositorio equipamentoRepositorio;
    private final UnidadeInventarioRepositorio unidadeRepositorio;
    private final ConfiguracaoDispatchServico configuracaoServico;
    private final RoteamentoEquipamentoServico roteamentoServico;
    private final DispatchSchedulerRegistry schedulerRegistry;
    private final ValidacaoPlanejamentoDispatchServico validacaoPlanejamentoServico;
    private final WorkQueueOperacaoServico workQueueOperacaoServico;
    private final EtapaWorkInstructionServico etapaServico;
    private final SelecaoEquipamentoAuxiliarServico auxiliarServico;
    private final NamedParameterJdbcTemplate jdbc;

    public DispatchDinamicoServico(
            WorkQueuePatioRepositorio filaRepositorio,
            OrdemTrabalhoPatioRepositorio ordemRepositorio,
            EquipamentoPatioRepositorio equipamentoRepositorio,
            UnidadeInventarioRepositorio unidadeRepositorio,
            ConfiguracaoDispatchServico configuracaoServico,
            RoteamentoEquipamentoServico roteamentoServico,
            DispatchSchedulerRegistry schedulerRegistry,
            ValidacaoPlanejamentoDispatchServico validacaoPlanejamentoServico,
            WorkQueueOperacaoServico workQueueOperacaoServico,
            EtapaWorkInstructionServico etapaServico,
            SelecaoEquipamentoAuxiliarServico auxiliarServico,
            NamedParameterJdbcTemplate jdbc) {
        this.filaRepositorio = filaRepositorio;
        this.ordemRepositorio = ordemRepositorio;
        this.equipamentoRepositorio = equipamentoRepositorio;
        this.unidadeRepositorio = unidadeRepositorio;
        this.configuracaoServico = configuracaoServico;
        this.roteamentoServico = roteamentoServico;
        this.schedulerRegistry = schedulerRegistry;
        this.validacaoPlanejamentoServico = validacaoPlanejamentoServico;
        this.workQueueOperacaoServico = workQueueOperacaoServico;
        this.etapaServico = etapaServico;
        this.auxiliarServico = auxiliarServico;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public List<Ranking> ranking(Long workQueueId, Long equipamentoId) {
        WorkQueuePatio fila = buscarFila(workQueueId);
        EquipamentoPatio equipamento = buscarEquipamento(
                equipamentoId == null ? fila.getEquipamentoPatioId() : equipamentoId);
        Configuracao configuracao = configuracaoServico.resolver(fila, equipamento.getTipoEquipamento());
        return montarRanking(fila, equipamento, configuracao);
    }

    @Transactional
    public Decisao autoDispatch(AutoDispatchRequest request) {
        validarRequest(request);
        Optional<Decisao> existente = buscarPorChave(request.getChaveIdempotencia());
        if (existente.isPresent()) {
            return existente.get();
        }
        WorkQueuePatio fila = buscarFila(request.getWorkQueueId());
        EquipamentoPatio equipamento = buscarEquipamento(request.getEquipamentoPatioId());
        validarContexto(fila, equipamento, request);
        Configuracao configuracao = configuracaoServico.resolver(fila, equipamento.getTipoEquipamento());
        OrdemTrabalhoPatio ordem = selecionarOrdem(fila, equipamento, configuracao, request);
        validarUnidadeSemHold(ordem);
        validarCapacidadeConcorrente(fila, equipamento, configuracao, ordem.getId());

        Rota rota = roteamentoServico.calcular(equipamento, ordem, configuracao);
        Avaliacao avaliacao = schedulerRegistry.obter(equipamento.getTipoEquipamento())
                .avaliar(ordem, equipamento, configuracao, rota, LocalDateTime.now());
        if (!avaliacao.elegivel()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Dispatch bloqueado: " + String.join(" ", avaliacao.motivosBloqueio()));
        }
        DispatchWorkQueueDto comando = comando(request, ordem.getId());
        validacaoPlanejamentoServico.revalidar(fila.getId(), comando);
        Long decisaoId = inserirDecisao(fila, ordem, equipamento, configuracao, rota, avaliacao, request);
        Auxiliar auxiliar = auxiliarServico.reservarSeNecessario(
                decisaoId, fila, ordem, equipamento, configuracao, request.getOperador());
        workQueueOperacaoServico.despachar(fila.getId(), comando);
        etapaServico.inicializar(ordem.getId(), decisaoId, request.getOperador());
        jdbc.update("""
                UPDATE decisao_dispatch
                SET status = 'ATRIBUIDA', atualizado_em = CURRENT_TIMESTAMP
                WHERE id = :id
                """, new MapSqlParameterSource("id", decisaoId));
        return buscarDecisao(decisaoId, auxiliar);
    }

    @Transactional
    public Optional<Decisao> despacharProximaAutomaticamente(Long workQueueId,
                                                              Long equipamentoId,
                                                              String gatilho) {
        WorkQueuePatio fila = buscarFila(workQueueId);
        EquipamentoPatio equipamento = buscarEquipamento(equipamentoId);
        Configuracao configuracao = configuracaoServico.resolver(fila, equipamento.getTipoEquipamento());
        if (configuracao.modo() != ModoDispatch.AUTOMATICO) {
            return Optional.empty();
        }
        List<Ranking> ranking = montarRanking(fila, equipamento, configuracao);
        Ranking primeira = ranking.stream().filter(Ranking::elegivel).findFirst().orElse(null);
        if (primeira == null) {
            return Optional.empty();
        }
        AutoDispatchRequest request = new AutoDispatchRequest();
        request.setWorkQueueId(workQueueId);
        request.setEquipamentoPatioId(equipamentoId);
        request.setOrdemTrabalhoPatioId(primeira.ordemTrabalhoPatioId());
        request.setCodigoUnidade(primeira.codigoUnidade());
        request.setOperador("scheduler-automatico");
        request.setFaseVisita("EM_OPERACAO");
        request.setPow(fila.getPow());
        request.setPool(fila.getPoolOperacional());
        request.setChaveIdempotencia("AUTO-" + workQueueId + "-" + primeira.ordemTrabalhoPatioId()
                + "-" + UUID.nameUUIDFromBytes(gatilho.getBytes()));
        request.setMotivo("Autodespacho apos " + gatilho + ".");
        request.setOrigemAcao("RECONCILIADOR_VMT");
        request.setCorrelationId(gatilho);
        return Optional.of(autoDispatch(request));
    }

    @Transactional(readOnly = true)
    public List<Decisao> listarDecisoes(int limite) {
        int limiteSeguro = Math.max(1, Math.min(limite, 200));
        return jdbc.query("""
                SELECT d.*, e.identificador AS equipamento, e.tipo_equipamento
                FROM decisao_dispatch d
                JOIN equipamento_patio e ON e.id = d.equipamento_patio_id
                ORDER BY d.criado_em DESC, d.id DESC
                LIMIT :limite
                """, new MapSqlParameterSource("limite", limiteSeguro), this::mapearDecisaoBasica)
                .stream().map(decisao -> buscarDecisao(decisao.id(), null)).toList();
    }

    @Transactional(readOnly = true)
    public Resumo resumo() {
        long configuracoes = numero("SELECT COUNT(*) FROM configuracao_dispatch WHERE status = 'ATIVA'");
        long decisoes = numero("""
                SELECT COUNT(*) FROM decisao_dispatch
                WHERE criado_em >= CURRENT_TIMESTAMP - INTERVAL '24 HOURS'
                """);
        long emExecucao = numero("SELECT COUNT(*) FROM ordem_trabalho_patio WHERE status_ordem = 'EM_EXECUCAO'");
        long telemetriasAtrasadas = numero("""
                SELECT COUNT(*)
                FROM equipamento_patio e
                LEFT JOIN telemetria_equipamento_patio t ON t.equipamento_id = e.id
                WHERE t.recebido_em IS NULL OR t.recebido_em < CURRENT_TIMESTAMP - INTERVAL '2 MINUTES'
                """);
        long auxiliares = numero("""
                SELECT COUNT(*) FROM reserva_equipamento_auxiliar_dispatch
                WHERE status IN ('RESERVADO', 'ASSOCIADO')
                """);
        return new Resumo(configuracoes, decisoes, emExecucao, telemetriasAtrasadas,
                auxiliares, listarDecisoes(20));
    }

    private List<Ranking> montarRanking(WorkQueuePatio fila,
                                        EquipamentoPatio equipamento,
                                        Configuracao configuracao) {
        List<Candidato> candidatos = ordemRepositorio
                .findByWorkQueueIdOrderByPrioridadeBuscaDescPrioridadeOperacionalAscSequenciaNavioAscCriadoEmAsc(
                        fila.getId())
                .stream()
                .filter(ordem -> ESTADOS_ELEGIVEIS.contains(ordem.getStatusOrdem()))
                .map(ordem -> avaliarCandidato(ordem, equipamento, configuracao))
                .sorted(Comparator.comparingDouble((Candidato candidato) -> candidato.avaliacao().score()).reversed()
                        .thenComparing(candidato -> candidato.ordem().getId()))
                .toList();
        return IntStream.range(0, candidatos.size())
                .mapToObj(indice -> {
                    Candidato candidato = candidatos.get(indice);
                    return new Ranking(
                            indice + 1,
                            candidato.ordem().getId(),
                            candidato.ordem().getCodigoConteiner(),
                            candidato.rota().origem(),
                            candidato.rota().destino(),
                            candidato.avaliacao().score(),
                            candidato.rota().etaSegundos(),
                            candidato.avaliacao().elegivel(),
                            candidato.avaliacao().motivosBloqueio(),
                            candidato.avaliacao().memoriaCalculo(),
                            candidato.rota());
                }).toList();
    }

    private Candidato avaliarCandidato(OrdemTrabalhoPatio ordem,
                                       EquipamentoPatio equipamento,
                                       Configuracao configuracao) {
        Rota rota = roteamentoServico.calcular(equipamento, ordem, configuracao);
        Avaliacao avaliacao = schedulerRegistry.obter(equipamento.getTipoEquipamento())
                .avaliar(ordem, equipamento, configuracao, rota, LocalDateTime.now());
        List<String> bloqueios = new ArrayList<>(avaliacao.motivosBloqueio());
        unidadeRepositorio.findByIdentificacaoIgnoreCase(ordem.getCodigoConteiner())
                .filter(unidade -> unidade.possuiHoldAtivo(LocalDateTime.now()))
                .ifPresent(unidade -> bloqueios.add("Unidade possui hold ativo."));
        if (!bloqueios.equals(avaliacao.motivosBloqueio())) {
            avaliacao = new Avaliacao(avaliacao.score(), false, List.copyOf(bloqueios),
                    avaliacao.memoriaCalculo() + "; hold=ATIVO");
        }
        return new Candidato(ordem, rota, avaliacao);
    }

    private OrdemTrabalhoPatio selecionarOrdem(WorkQueuePatio fila,
                                                EquipamentoPatio equipamento,
                                                Configuracao configuracao,
                                                AutoDispatchRequest request) {
        List<OrdemTrabalhoPatio> ordens = ordemRepositorio
                .findByWorkQueueIdOrderByPrioridadeBuscaDescPrioridadeOperacionalAscSequenciaNavioAscCriadoEmAsc(
                        fila.getId());
        if (request.getOrdemTrabalhoPatioId() != null) {
            return ordens.stream()
                    .filter(ordem -> Objects.equals(ordem.getId(), request.getOrdemTrabalhoPatioId()))
                    .filter(ordem -> ESTADOS_ELEGIVEIS.contains(ordem.getStatusOrdem()))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                            "A work instruction informada nao pertence a fila ou nao esta elegivel."));
        }
        if (StringUtils.hasText(request.getCodigoUnidade())) {
            return ordens.stream()
                    .filter(ordem -> request.getCodigoUnidade().trim().equalsIgnoreCase(ordem.getCodigoConteiner()))
                    .filter(ordem -> ESTADOS_ELEGIVEIS.contains(ordem.getStatusOrdem()))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                            "A unidade informada nao possui work instruction elegivel na fila."));
        }
        return montarRanking(fila, equipamento, configuracao).stream()
                .filter(Ranking::elegivel)
                .findFirst()
                .map(ranking -> ordemRepositorio.findById(ranking.ordemTrabalhoPatioId()).orElseThrow())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "Nao existe work instruction elegivel para autodespacho."));
    }

    private void validarRequest(AutoDispatchRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A requisicao de autodespacho deve ser informada.");
        }
        String fase = normalizar(request.getFaseVisita());
        if (!FASES_ATIVAS.contains(fase)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A fase da visita nao permite dispatch: " + fase + ".");
        }
    }

    private void validarContexto(WorkQueuePatio fila,
                                 EquipamentoPatio equipamento,
                                 AutoDispatchRequest request) {
        if (fila.getStatus() != StatusWorkQueuePatio.ATIVA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A work queue precisa estar ativa.");
        }
        if (equipamento.getStatusOperacional() != StatusEquipamento.OPERACIONAL) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O CHE informado nao esta operacional.");
        }
        if (!Objects.equals(fila.getEquipamentoPatioId(), equipamento.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O CHE informado nao corresponde ao equipamento associado a work queue.");
        }
        if (StringUtils.hasText(request.getPow())
                && !request.getPow().trim().equalsIgnoreCase(valor(fila.getPow(), ""))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O POW informado diverge do POW persistido na work queue.");
        }
        if (StringUtils.hasText(request.getPool())
                && !request.getPool().trim().equalsIgnoreCase(valor(fila.getPoolOperacional(), ""))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O pool informado diverge do pool persistido na work queue.");
        }
    }

    private void validarUnidadeSemHold(OrdemTrabalhoPatio ordem) {
        UnidadeInventario unidade = unidadeRepositorio
                .findByIdentificacaoIgnoreCase(ordem.getCodigoConteiner()).orElse(null);
        if (unidade != null && unidade.possuiHoldAtivo(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A unidade " + ordem.getCodigoConteiner() + " possui hold ativo.");
        }
    }

    private void validarCapacidadeConcorrente(WorkQueuePatio fila,
                                              EquipamentoPatio equipamento,
                                              Configuracao configuracao,
                                              Long ordemSelecionadaId) {
        Integer emExecucao = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM ordem_trabalho_patio o
                JOIN work_queue_patio w ON w.id = o.work_queue_id
                WHERE w.equipamento_patio_id = :equipamentoId
                  AND o.status_ordem = 'EM_EXECUCAO'
                  AND o.id <> :ordemId
                """, new MapSqlParameterSource()
                .addValue("equipamentoId", equipamento.getId())
                .addValue("ordemId", ordemSelecionadaId), Integer.class);
        if (emExecucao != null && emExecucao >= configuracao.capacidadeSimultanea()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O CHE atingiu a capacidade simultanea configurada para o dispatch.");
        }
    }

    private DispatchWorkQueueDto comando(AutoDispatchRequest request, Long ordemId) {
        DispatchWorkQueueDto comando = new DispatchWorkQueueDto();
        comando.setOrdemIds(List.of(ordemId));
        comando.setSomentePendentes(false);
        comando.setLimiteOrdens(1);
        comando.setOperador(request.getOperador());
        comando.setMotivo(request.getMotivo());
        comando.setOrigemAcao(request.getOrigemAcao());
        comando.setCorrelationId(request.getCorrelationId());
        return comando;
    }

    private Long inserirDecisao(WorkQueuePatio fila,
                                OrdemTrabalhoPatio ordem,
                                EquipamentoPatio equipamento,
                                Configuracao configuracao,
                                Rota rota,
                                Avaliacao avaliacao,
                                AutoDispatchRequest request) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update("""
                INSERT INTO decisao_dispatch (
                    chave_idempotencia, work_queue_id, ordem_trabalho_patio_id,
                    equipamento_patio_id, configuracao_dispatch_id, versao_configuracao,
                    modo_dispatch, score, memoria_calculo, origem_rota, destino_rota,
                    distancia_metros, congestionamento_percentual, eta_segundos,
                    telemetria_recebida_em, telemetria_atrasada, rota_bloqueada,
                    status, motivo, operador, correlation_id, criado_em, atualizado_em
                ) VALUES (
                    :chave, :filaId, :ordemId,
                    :equipamentoId, :configuracaoId, :versao,
                    :modo, :score, :memoria, :origem, :destino,
                    :distancia, :congestionamento, :eta,
                    :telemetriaRecebidaEm, :telemetriaAtrasada, :rotaBloqueada,
                    'RECOMENDADA', :motivo, :operador, :correlationId,
                    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                )
                """, new MapSqlParameterSource()
                .addValue("chave", request.getChaveIdempotencia().trim())
                .addValue("filaId", fila.getId())
                .addValue("ordemId", ordem.getId())
                .addValue("equipamentoId", equipamento.getId())
                .addValue("configuracaoId", configuracao.id())
                .addValue("versao", configuracao.versao())
                .addValue("modo", configuracao.modo().name())
                .addValue("score", avaliacao.score())
                .addValue("memoria", limitar(avaliacao.memoriaCalculo(), 4000))
                .addValue("origem", rota.origem())
                .addValue("destino", rota.destino())
                .addValue("distancia", rota.distanciaMetros())
                .addValue("congestionamento", rota.congestionamentoPercentual())
                .addValue("eta", rota.etaSegundos())
                .addValue("telemetriaRecebidaEm", rota.telemetriaRecebidaEm())
                .addValue("telemetriaAtrasada", rota.telemetriaAtrasada())
                .addValue("rotaBloqueada", rota.bloqueada())
                .addValue("motivo", request.getMotivo().trim())
                .addValue("operador", request.getOperador().trim())
                .addValue("correlationId", limpar(request.getCorrelationId())), keyHolder,
                new String[]{"id"});
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    private Optional<Decisao> buscarPorChave(String chave) {
        if (!StringUtils.hasText(chave)) {
            return Optional.empty();
        }
        return jdbc.query("""
                SELECT d.*, e.identificador AS equipamento, e.tipo_equipamento
                FROM decisao_dispatch d
                JOIN equipamento_patio e ON e.id = d.equipamento_patio_id
                WHERE d.chave_idempotencia = :chave
                """, new MapSqlParameterSource("chave", chave.trim()), this::mapearDecisaoBasica)
                .stream().findFirst().map(decisao -> buscarDecisao(decisao.id(), null));
    }

    private Decisao buscarDecisao(Long id, Auxiliar auxiliarInformado) {
        Decisao basica = jdbc.query("""
                SELECT d.*, e.identificador AS equipamento, e.tipo_equipamento
                FROM decisao_dispatch d
                JOIN equipamento_patio e ON e.id = d.equipamento_patio_id
                WHERE d.id = :id
                """, new MapSqlParameterSource("id", id), this::mapearDecisaoBasica)
                .stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Decisao de dispatch nao encontrada."));
        Auxiliar auxiliar = auxiliarInformado == null
                ? auxiliarServico.buscarPorDecisao(id).orElse(null)
                : auxiliarInformado;
        return new Decisao(
                basica.id(), basica.chaveIdempotencia(), basica.workQueueId(),
                basica.ordemTrabalhoPatioId(), basica.equipamentoPatioId(), basica.equipamento(),
                basica.tipoEquipamento(), basica.modo(), basica.score(), basica.etaSegundos(),
                basica.status(), basica.memoriaCalculo(), basica.rota(), auxiliar,
                etapaServico.listar(basica.ordemTrabalhoPatioId()), basica.criadoEm());
    }

    private Decisao mapearDecisaoBasica(ResultSet rs, int rowNum) throws SQLException {
        Rota rota = new Rota(
                rs.getString("origem_rota"),
                rs.getString("destino_rota"),
                rs.getDouble("distancia_metros"),
                rs.getDouble("congestionamento_percentual"),
                rs.getInt("eta_segundos"),
                rs.getBoolean("rota_bloqueada"),
                rs.getBoolean("telemetria_atrasada"),
                data(rs.getTimestamp("telemetria_recebida_em")),
                rs.getString("memoria_calculo"));
        return new Decisao(
                rs.getLong("id"),
                rs.getString("chave_idempotencia"),
                rs.getLong("work_queue_id"),
                rs.getLong("ordem_trabalho_patio_id"),
                rs.getLong("equipamento_patio_id"),
                rs.getString("equipamento"),
                rs.getString("tipo_equipamento"),
                ModoDispatch.valueOf(rs.getString("modo_dispatch")),
                rs.getDouble("score"),
                rs.getInt("eta_segundos"),
                rs.getString("status"),
                rs.getString("memoria_calculo"),
                rota,
                null,
                List.of(),
                data(rs.getTimestamp("criado_em")));
    }

    private long numero(String sql) {
        Long valor = jdbc.queryForObject(sql, new MapSqlParameterSource(), Long.class);
        return valor == null ? 0L : valor;
    }

    private WorkQueuePatio buscarFila(Long id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A work queue deve ser informada.");
        }
        return filaRepositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Work queue de patio nao encontrada."));
    }

    private EquipamentoPatio buscarEquipamento(Long id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A work queue nao possui CHE real associado.");
        }
        return equipamentoRepositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "CHE de patio nao encontrado."));
    }

    private LocalDateTime data(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private String normalizar(String valor) {
        return StringUtils.hasText(valor) ? valor.trim().toUpperCase(Locale.ROOT) : "";
    }

    private String valor(String valor, String padrao) {
        return StringUtils.hasText(valor) ? valor.trim() : padrao;
    }

    private String limpar(String valor) {
        return StringUtils.hasText(valor) ? valor.trim() : null;
    }

    private String limitar(String valor, int limite) {
        return valor == null || valor.length() <= limite ? valor : valor.substring(0, limite);
    }

    private record Candidato(OrdemTrabalhoPatio ordem, Rota rota, Avaliacao avaliacao) {
    }
}
