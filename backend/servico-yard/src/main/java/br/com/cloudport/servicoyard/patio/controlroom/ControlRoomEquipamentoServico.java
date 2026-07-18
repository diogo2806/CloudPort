package br.com.cloudport.servicoyard.patio.controlroom;

import br.com.cloudport.servicoyard.patio.dto.TelemetriaEquipamentoPatioDto;
import br.com.cloudport.servicoyard.patio.modelo.EquipamentoPatio;
import br.com.cloudport.servicoyard.patio.modelo.StatusEquipamento;
import br.com.cloudport.servicoyard.patio.repositorio.EquipamentoPatioRepositorio;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ControlRoomEquipamentoServico {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() { };

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final EquipamentoPatioRepositorio equipamentoRepositorio;
    private final ControlRoomEquipamentoStreamingServico streamingServico;

    public ControlRoomEquipamentoServico(
            NamedParameterJdbcTemplate jdbc,
            ObjectMapper objectMapper,
            EquipamentoPatioRepositorio equipamentoRepositorio,
            ControlRoomEquipamentoStreamingServico streamingServico
    ) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
        this.equipamentoRepositorio = equipamentoRepositorio;
        this.streamingServico = streamingServico;
    }

    @Transactional
    public ControlRoomEquipamentoDtos.Resumo resumo() {
        avaliarSaudeInterna();
        LocalDateTime agora = LocalDateTime.now();
        MapSqlParameterSource parametros = new MapSqlParameterSource()
                .addValue("limiteHeartbeat", agora.minus(ControlRoomEquipamentoRegras.LIMITE_HEARTBEAT))
                .addValue("limiteTelemetria", agora.minus(ControlRoomEquipamentoRegras.LIMITE_TELEMETRIA));
        return jdbc.queryForObject("""
                SELECT
                    COUNT(*) AS total_equipamentos,
                    COALESCE(SUM(CASE WHEN e.status_operacional = 'OPERACIONAL' THEN 1 ELSE 0 END), 0) AS operacionais,
                    COALESCE(SUM(CASE WHEN e.status_operacional = 'MANUTENCAO' THEN 1 ELSE 0 END), 0) AS manutencao,
                    COALESCE(SUM(CASE WHEN e.status_operacional = 'INDISPONIVEL' THEN 1 ELSE 0 END), 0) AS indisponiveis,
                    (SELECT COUNT(*) FROM control_room_dispositivo d
                        WHERE d.status_integracao = 'CONECTADO'
                          AND d.ultimo_heartbeat_em >= :limiteHeartbeat) AS conectados,
                    (SELECT COUNT(*) FROM equipamento_patio ep
                        LEFT JOIN telemetria_equipamento_patio t ON t.equipamento_id = ep.id
                        WHERE t.recebido_em IS NULL OR t.recebido_em < :limiteTelemetria) AS telemetria_atrasada,
                    (SELECT COUNT(*) FROM control_room_alarme a
                        WHERE a.status IN ('ATIVO', 'RECONHECIDO')) AS alarmes_ativos,
                    (SELECT COUNT(*) FROM control_room_comando c
                        WHERE c.status IN ('PENDENTE', 'ENVIADO')) AS comandos_pendentes,
                    (SELECT COUNT(*) FROM control_room_indisponibilidade i
                        WHERE i.fim_em IS NULL) AS indisponibilidades_abertas
                FROM equipamento_patio e
                """, parametros, (rs, rowNum) -> new ControlRoomEquipamentoDtos.Resumo(
                rs.getLong("total_equipamentos"),
                rs.getLong("operacionais"),
                rs.getLong("manutencao"),
                rs.getLong("indisponiveis"),
                rs.getLong("conectados"),
                rs.getLong("telemetria_atrasada"),
                rs.getLong("alarmes_ativos"),
                rs.getLong("comandos_pendentes"),
                rs.getLong("indisponibilidades_abertas"),
                agora
        ));
    }

    @Transactional
    public List<ControlRoomEquipamentoDtos.Equipamento> listarEquipamentos(
            String status,
            String tipo,
            String conectividade
    ) {
        avaliarSaudeInterna();
        LocalDateTime agora = LocalDateTime.now();
        MapSqlParameterSource parametros = new MapSqlParameterSource()
                .addValue("limiteHeartbeat", agora.minus(ControlRoomEquipamentoRegras.LIMITE_HEARTBEAT))
                .addValue("limiteTelemetria", agora.minus(ControlRoomEquipamentoRegras.LIMITE_TELEMETRIA));
        return jdbc.query("""
                SELECT
                    e.id,
                    e.identificador,
                    e.tipo_equipamento,
                    e.status_operacional,
                    e.linha,
                    e.coluna,
                    CASE
                        WHEN d.id IS NOT NULL
                          AND d.status_integracao = 'CONECTADO'
                          AND d.ultimo_heartbeat_em >= :limiteHeartbeat THEN 'CONECTADO'
                        WHEN t.recebido_em >= :limiteTelemetria THEN 'TELEMETRIA_ATIVA'
                        WHEN d.id IS NOT NULL THEN 'DESCONECTADO'
                        ELSE 'SEM_DISPOSITIVO'
                    END AS conectividade,
                    d.identificador AS dispositivo,
                    d.protocolo,
                    d.firmware,
                    d.ultimo_heartbeat_em,
                    t.latitude,
                    t.longitude,
                    t.coordenada_x,
                    t.coordenada_y,
                    t.heading,
                    t.posicao_mais_proxima,
                    t.dentro_da_posicao,
                    t.status_vmt,
                    t.work_instruction_atual_id,
                    t.sequencia AS sequencia_telemetria,
                    t.capturado_em,
                    t.recebido_em,
                    (SELECT COUNT(*) FROM control_room_alarme a
                        WHERE a.equipamento_id = e.id
                          AND a.status IN ('ATIVO', 'RECONHECIDO')) AS alarmes_ativos,
                    (SELECT MAX(i.id) FROM control_room_indisponibilidade i
                        WHERE i.equipamento_id = e.id AND i.fim_em IS NULL) AS indisponibilidade_aberta_id
                FROM equipamento_patio e
                LEFT JOIN telemetria_equipamento_patio t ON t.equipamento_id = e.id
                LEFT JOIN control_room_dispositivo d ON d.equipamento_id = e.id
                ORDER BY e.tipo_equipamento, e.identificador
                """, parametros, this::mapearEquipamento).stream()
                .filter(item -> semFiltroOuIgual(status, item.statusOperacional()))
                .filter(item -> semFiltroOuIgual(tipo, item.tipoEquipamento()))
                .filter(item -> semFiltroOuIgual(conectividade, item.conectividade()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ControlRoomEquipamentoDtos.HistoricoTelemetria> historico(String identificador, int limite) {
        EquipamentoPatio equipamento = equipamento(identificador);
        int limiteSeguro = Math.max(1, Math.min(limite, 500));
        return jdbc.query("""
                SELECT h.*, e.identificador AS equipamento
                FROM control_room_telemetria_historico h
                JOIN equipamento_patio e ON e.id = h.equipamento_id
                WHERE h.equipamento_id = :equipamentoId
                ORDER BY h.capturado_em DESC, h.id DESC
                LIMIT :limite
                """, new MapSqlParameterSource()
                .addValue("equipamentoId", equipamento.getId())
                .addValue("limite", limiteSeguro), this::mapearHistorico);
    }

    @Transactional(readOnly = true)
    public List<ControlRoomEquipamentoDtos.Alarme> listarAlarmes(String status, String severidade) {
        return jdbc.query("""
                SELECT a.*, e.identificador AS equipamento, e.tipo_equipamento
                FROM control_room_alarme a
                JOIN equipamento_patio e ON e.id = a.equipamento_id
                ORDER BY
                    CASE a.severidade WHEN 'CRITICA' THEN 1 WHEN 'ALTA' THEN 2 WHEN 'MEDIA' THEN 3 ELSE 4 END,
                    a.aberto_em DESC
                """, Collections.emptyMap(), this::mapearAlarme).stream()
                .filter(item -> semFiltroOuIgual(status, item.status()))
                .filter(item -> semFiltroOuIgual(severidade, item.severidade()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ControlRoomEquipamentoDtos.Comando> listarComandos(String identificador) {
        List<ControlRoomEquipamentoDtos.Comando> comandos = jdbc.query("""
                SELECT c.*, e.identificador AS equipamento, d.identificador AS dispositivo
                FROM control_room_comando c
                JOIN equipamento_patio e ON e.id = c.equipamento_id
                LEFT JOIN control_room_dispositivo d ON d.id = c.dispositivo_id
                ORDER BY c.criado_em DESC, c.id DESC
                LIMIT 250
                """, Collections.emptyMap(), this::mapearComando);
        if (!StringUtils.hasText(identificador)) {
            return comandos;
        }
        String normalizado = ControlRoomEquipamentoRegras.normalizarIdentificador(identificador);
        return comandos.stream().filter(item -> normalizado.equals(item.equipamento())).toList();
    }

    @Transactional(readOnly = true)
    public List<ControlRoomEquipamentoDtos.Indisponibilidade> listarIndisponibilidades(String identificador) {
        List<ControlRoomEquipamentoDtos.Indisponibilidade> periodos = jdbc.query("""
                SELECT i.*, e.identificador AS equipamento, e.tipo_equipamento
                FROM control_room_indisponibilidade i
                JOIN equipamento_patio e ON e.id = i.equipamento_id
                ORDER BY i.inicio_em DESC, i.id DESC
                LIMIT 250
                """, Collections.emptyMap(), this::mapearIndisponibilidade);
        if (!StringUtils.hasText(identificador)) {
            return periodos;
        }
        String normalizado = ControlRoomEquipamentoRegras.normalizarIdentificador(identificador);
        return periodos.stream().filter(item -> normalizado.equals(item.equipamento())).toList();
    }

    @Transactional(readOnly = true)
    public List<ControlRoomEquipamentoDtos.Dispositivo> listarDispositivos() {
        return jdbc.query("""
                SELECT d.*, e.identificador AS equipamento, e.tipo_equipamento
                FROM control_room_dispositivo d
                JOIN equipamento_patio e ON e.id = d.equipamento_id
                ORDER BY e.tipo_equipamento, e.identificador
                """, Collections.emptyMap(), this::mapearDispositivo);
    }

    @Transactional
    public ControlRoomEquipamentoDtos.Comando criarComando(
            String identificador,
            ControlRoomEquipamentoDtos.ComandoRequisicao requisicao,
            String usuario
    ) {
        EquipamentoPatio equipamento = equipamento(identificador);
        String tipo = ControlRoomEquipamentoRegras.normalizarTipoComando(requisicao.getTipo());
        Long dispositivoId = dispositivoId(equipamento.getId()).orElse(null);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        MapSqlParameterSource parametros = new MapSqlParameterSource()
                .addValue("equipamentoId", equipamento.getId())
                .addValue("dispositivoId", dispositivoId)
                .addValue("tipo", tipo)
                .addValue("parametros", escreverJson(requisicao.getParametros()))
                .addValue("mensagem", limpar(requisicao.getMensagem(), 500))
                .addValue("usuario", usuario(usuario))
                .addValue("correlationId", limpar(requisicao.getCorrelationId(), 100));
        jdbc.update("""
                INSERT INTO control_room_comando (
                    equipamento_id, dispositivo_id, tipo, status, parametros_json,
                    mensagem, solicitado_por, correlation_id, criado_em
                ) VALUES (
                    :equipamentoId, :dispositivoId, :tipo, 'PENDENTE', :parametros,
                    :mensagem, :usuario, :correlationId, CURRENT_TIMESTAMP
                )
                """, parametros, keyHolder, new String[]{"id"});
        Long id = Objects.requireNonNull(keyHolder.getKey()).longValue();
        ControlRoomEquipamentoDtos.Comando comando = obterComando(id);
        streamingServico.publicar("COMANDO_CRIADO", comando);
        return comando;
    }

    @Transactional
    public List<ControlRoomEquipamentoDtos.Comando> buscarComandosPendentes(String dispositivo) {
        ControlRoomEquipamentoDtos.Dispositivo registro = obterDispositivo(dispositivo);
        jdbc.update("""
                UPDATE control_room_comando
                SET status = 'ENVIADO', enviado_em = COALESCE(enviado_em, CURRENT_TIMESTAMP)
                WHERE dispositivo_id = :dispositivoId AND status = 'PENDENTE'
                """, Map.of("dispositivoId", registro.id()));
        return jdbc.query("""
                SELECT c.*, e.identificador AS equipamento, d.identificador AS dispositivo
                FROM control_room_comando c
                JOIN equipamento_patio e ON e.id = c.equipamento_id
                JOIN control_room_dispositivo d ON d.id = c.dispositivo_id
                WHERE c.dispositivo_id = :dispositivoId AND c.status = 'ENVIADO'
                ORDER BY c.criado_em, c.id
                LIMIT 20
                """, Map.of("dispositivoId", registro.id()), this::mapearComando);
    }

    @Transactional
    public ControlRoomEquipamentoDtos.Comando confirmarComando(
            String dispositivo,
            Long comandoId,
            ControlRoomEquipamentoDtos.ConfirmacaoComandoRequisicao requisicao
    ) {
        ControlRoomEquipamentoDtos.Dispositivo registro = obterDispositivo(dispositivo);
        ControlRoomEquipamentoDtos.Comando atual = obterComando(comandoId);
        if (!Objects.equals(registro.identificador(), atual.dispositivo())) {
            throw new IllegalArgumentException("O comando nao pertence ao dispositivo informado.");
        }
        String status = ControlRoomEquipamentoRegras.normalizarStatusConfirmacao(requisicao.getStatus());
        LocalDateTime confirmadoEm = requisicao.getConfirmadoEm() == null
                ? LocalDateTime.now() : requisicao.getConfirmadoEm();
        int alterados = jdbc.update("""
                UPDATE control_room_comando
                SET status = :status,
                    confirmado_em = :confirmadoEm,
                    retorno_dispositivo = :retorno,
                    sequencia_dispositivo = :sequencia
                WHERE id = :id AND status IN ('PENDENTE', 'ENVIADO')
                """, new MapSqlParameterSource()
                .addValue("status", status)
                .addValue("confirmadoEm", confirmadoEm)
                .addValue("retorno", limpar(requisicao.getRetorno(), 1000))
                .addValue("sequencia", requisicao.getSequenciaDispositivo())
                .addValue("id", comandoId));
        if (alterados == 0) {
            throw new IllegalArgumentException("O comando ja foi finalizado ou cancelado.");
        }
        if (requisicao.getSequenciaDispositivo() != null) {
            jdbc.update("""
                    UPDATE control_room_dispositivo
                    SET ultima_sequencia = CASE
                            WHEN ultima_sequencia IS NULL OR ultima_sequencia < :sequencia THEN :sequencia
                            ELSE ultima_sequencia END,
                        atualizado_em = CURRENT_TIMESTAMP
                    WHERE id = :id
                    """, Map.of("sequencia", requisicao.getSequenciaDispositivo(), "id", registro.id()));
        }
        if ("EXECUTADO".equals(status)) {
            aplicarEfeitoComando(atual, registro.identificador());
        }
        ControlRoomEquipamentoDtos.Comando confirmado = obterComando(comandoId);
        streamingServico.publicar("COMANDO_CONFIRMADO", confirmado);
        return confirmado;
    }

    @Transactional
    public ControlRoomEquipamentoDtos.Dispositivo heartbeat(
            String identificadorDispositivo,
            ControlRoomEquipamentoDtos.HeartbeatRequisicao requisicao
    ) {
        String dispositivo = ControlRoomEquipamentoRegras.normalizarIdentificador(identificadorDispositivo);
        EquipamentoPatio equipamento = equipamento(requisicao.getEquipamento());
        String protocolo = ControlRoomEquipamentoRegras.normalizarIdentificador(requisicao.getProtocolo());
        String status = ControlRoomEquipamentoRegras.normalizarStatusIntegracao(requisicao.getStatusIntegracao());
        Optional<ControlRoomEquipamentoDtos.Dispositivo> atual = dispositivoOpcional(dispositivo);
        if (atual.isPresent()) {
            ControlRoomEquipamentoDtos.Dispositivo existente = atual.get();
            if (!existente.equipamento().equals(equipamento.getIdentificador())) {
                throw new IllegalArgumentException("O dispositivo ja esta vinculado a outro equipamento.");
            }
            if (existente.ultimaSequencia() != null && requisicao.getSequencia() <= existente.ultimaSequencia()) {
                throw new IllegalArgumentException("A sequencia do heartbeat deve ser maior que a ultima processada.");
            }
            jdbc.update("""
                    UPDATE control_room_dispositivo
                    SET protocolo = :protocolo,
                        status_integracao = :status,
                        firmware = :firmware,
                        endereco_rede = :endereco,
                        ultima_sequencia = :sequencia,
                        ultimo_heartbeat_em = :capturadoEm,
                        atualizado_em = CURRENT_TIMESTAMP
                    WHERE id = :id
                    """, new MapSqlParameterSource()
                    .addValue("protocolo", protocolo)
                    .addValue("status", status)
                    .addValue("firmware", limpar(requisicao.getFirmware(), 80))
                    .addValue("endereco", limpar(requisicao.getEnderecoRede(), 120))
                    .addValue("sequencia", requisicao.getSequencia())
                    .addValue("capturadoEm", requisicao.getCapturadoEm())
                    .addValue("id", existente.id()));
        } else {
            try {
                jdbc.update("""
                        INSERT INTO control_room_dispositivo (
                            equipamento_id, identificador, protocolo, status_integracao,
                            firmware, endereco_rede, ultima_sequencia, ultimo_heartbeat_em, atualizado_em
                        ) VALUES (
                            :equipamentoId, :identificador, :protocolo, :status,
                            :firmware, :endereco, :sequencia, :capturadoEm, CURRENT_TIMESTAMP
                        )
                        """, new MapSqlParameterSource()
                        .addValue("equipamentoId", equipamento.getId())
                        .addValue("identificador", dispositivo)
                        .addValue("protocolo", protocolo)
                        .addValue("status", status)
                        .addValue("firmware", limpar(requisicao.getFirmware(), 80))
                        .addValue("endereco", limpar(requisicao.getEnderecoRede(), 120))
                        .addValue("sequencia", requisicao.getSequencia())
                        .addValue("capturadoEm", requisicao.getCapturadoEm()));
            } catch (DuplicateKeyException erro) {
                throw new IllegalArgumentException("O equipamento ja possui outro dispositivo integrado.", erro);
            }
        }
        if ("CONECTADO".equals(status)) {
            resolverAlarme(equipamento.getId(), "DISPOSITIVO_DESCONECTADO", dispositivo);
        } else if ("ERRO".equals(status)) {
            abrirAlarme(equipamento.getId(), "FALHA_DISPOSITIVO", "CRITICA",
                    "O dispositivo informou falha de integracao.", dispositivo, null);
        }
        ControlRoomEquipamentoDtos.Dispositivo resposta = obterDispositivo(dispositivo);
        streamingServico.publicar("HEARTBEAT_DISPOSITIVO", resposta);
        return resposta;
    }

    @Transactional
    public ControlRoomEquipamentoDtos.Indisponibilidade iniciarIndisponibilidade(
            String identificador,
            ControlRoomEquipamentoDtos.IndisponibilidadeRequisicao requisicao,
            String usuario
    ) {
        EquipamentoPatio equipamento = equipamento(identificador);
        Long id = abrirIndisponibilidade(
                equipamento,
                requisicao.getMotivo(),
                requisicao.getObservacao(),
                usuario(usuario),
                null
        );
        ControlRoomEquipamentoDtos.Indisponibilidade indisponibilidade = obterIndisponibilidade(id);
        streamingServico.publicar("INDISPONIBILIDADE_INICIADA", indisponibilidade);
        return indisponibilidade;
    }

    @Transactional
    public ControlRoomEquipamentoDtos.Indisponibilidade encerrarIndisponibilidade(
            Long id,
            ControlRoomEquipamentoDtos.EncerramentoIndisponibilidadeRequisicao requisicao,
            String usuario
    ) {
        ControlRoomEquipamentoDtos.Indisponibilidade atual = obterIndisponibilidade(id);
        if (atual.fimEm() != null) {
            throw new IllegalArgumentException("A indisponibilidade ja foi encerrada.");
        }
        jdbc.update("""
                UPDATE control_room_indisponibilidade
                SET fim_em = CURRENT_TIMESTAMP,
                    encerrado_por = :usuario,
                    observacao = CASE
                        WHEN :observacao IS NULL THEN observacao
                        WHEN observacao IS NULL OR observacao = '' THEN :observacao
                        ELSE observacao || ' | Encerramento: ' || :observacao END
                WHERE id = :id AND fim_em IS NULL
                """, new MapSqlParameterSource()
                .addValue("usuario", usuario(usuario))
                .addValue("observacao", limpar(requisicao == null ? null : requisicao.getObservacao(), 1000))
                .addValue("id", id));
        EquipamentoPatio equipamento = equipamento(atual.equipamento());
        equipamento.setStatusOperacional(StatusEquipamento.OPERACIONAL);
        equipamentoRepositorio.save(equipamento);
        resolverAlarme(equipamento.getId(), "EQUIPAMENTO_INDISPONIVEL", usuario(usuario));
        ControlRoomEquipamentoDtos.Indisponibilidade encerrada = obterIndisponibilidade(id);
        streamingServico.publicar("INDISPONIBILIDADE_ENCERRADA", encerrada);
        return encerrada;
    }

    @Transactional
    public ControlRoomEquipamentoDtos.Alarme reconhecerAlarme(Long id, String usuario) {
        int alterados = jdbc.update("""
                UPDATE control_room_alarme
                SET status = 'RECONHECIDO', reconhecido_em = CURRENT_TIMESTAMP, reconhecido_por = :usuario
                WHERE id = :id AND status = 'ATIVO'
                """, Map.of("usuario", usuario(usuario), "id", id));
        if (alterados == 0) {
            throw new IllegalArgumentException("O alarme nao esta ativo ou ja foi reconhecido.");
        }
        ControlRoomEquipamentoDtos.Alarme alarme = obterAlarme(id);
        streamingServico.publicar("ALARME_RECONHECIDO", alarme);
        return alarme;
    }

    @Transactional
    public ControlRoomEquipamentoDtos.Alarme resolverAlarme(Long id, String usuario) {
        int alterados = jdbc.update("""
                UPDATE control_room_alarme
                SET status = 'RESOLVIDO', resolvido_em = CURRENT_TIMESTAMP, resolvido_por = :usuario
                WHERE id = :id AND status IN ('ATIVO', 'RECONHECIDO')
                """, Map.of("usuario", usuario(usuario), "id", id));
        if (alterados == 0) {
            throw new IllegalArgumentException("O alarme ja foi resolvido ou nao existe.");
        }
        ControlRoomEquipamentoDtos.Alarme alarme = obterAlarme(id);
        streamingServico.publicar("ALARME_RESOLVIDO", alarme);
        return alarme;
    }

    @Transactional
    public void registrarTelemetria(TelemetriaEquipamentoPatioDto telemetria) {
        EquipamentoPatio equipamento = equipamento(telemetria.equipamento());
        try {
            jdbc.update("""
                    INSERT INTO control_room_telemetria_historico (
                        equipamento_id, latitude, longitude, coordenada_x, coordenada_y, heading,
                        posicao_mais_proxima, distancia_posicao_centimetros, dentro_da_posicao,
                        origem, operador_vmt, status_vmt, work_instruction_atual_id,
                        sequencia, capturado_em, recebido_em
                    ) VALUES (
                        :equipamentoId, :latitude, :longitude, :coordenadaX, :coordenadaY, :heading,
                        :posicao, :distancia, :dentro,
                        :origem, :operador, :statusVmt, :workInstruction,
                        :sequencia, :capturadoEm, :recebidoEm
                    )
                    """, new MapSqlParameterSource()
                    .addValue("equipamentoId", equipamento.getId())
                    .addValue("latitude", telemetria.latitude())
                    .addValue("longitude", telemetria.longitude())
                    .addValue("coordenadaX", telemetria.coordenadaX())
                    .addValue("coordenadaY", telemetria.coordenadaY())
                    .addValue("heading", telemetria.heading())
                    .addValue("posicao", telemetria.posicaoMaisProxima())
                    .addValue("distancia", telemetria.distanciaPosicaoCentimetros())
                    .addValue("dentro", telemetria.dentroDaPosicao())
                    .addValue("origem", telemetria.origem())
                    .addValue("operador", telemetria.operadorVmt())
                    .addValue("statusVmt", telemetria.statusVmt())
                    .addValue("workInstruction", telemetria.workInstructionAtualId())
                    .addValue("sequencia", telemetria.sequencia())
                    .addValue("capturadoEm", telemetria.capturadoEm())
                    .addValue("recebidoEm", telemetria.recebidoEm() == null ? LocalDateTime.now() : telemetria.recebidoEm()));
        } catch (DuplicateKeyException ignorada) {
            return;
        }
        resolverAlarme(equipamento.getId(), "TELEMETRIA_ATRASADA", "telemetria");
        atualizarEstadoViaTelemetria(equipamento, telemetria.statusVmt());
        atualizarDispositivoPelaOrigem(equipamento.getId(), telemetria);
        streamingServico.publicar("TELEMETRIA_ATUALIZADA", telemetria);
    }

    @Scheduled(fixedDelayString = "${cloudport.control-room.avaliacao-ms:60000}")
    @Transactional
    public void avaliarSaudeAgendada() {
        if (avaliarSaudeInterna()) {
            streamingServico.publicar("SAUDE_REAVALIADA", resumoSemReavaliar());
        }
    }

    private boolean avaliarSaudeInterna() {
        LocalDateTime agora = LocalDateTime.now();
        List<Map<String, Object>> linhas = jdbc.queryForList("""
                SELECT e.id, e.identificador, t.recebido_em,
                       d.id AS dispositivo_id, d.identificador AS dispositivo,
                       d.status_integracao, d.ultimo_heartbeat_em
                FROM equipamento_patio e
                LEFT JOIN telemetria_equipamento_patio t ON t.equipamento_id = e.id
                LEFT JOIN control_room_dispositivo d ON d.equipamento_id = e.id
                """, Collections.emptyMap());
        boolean alterou = false;
        for (Map<String, Object> linha : linhas) {
            Long equipamentoId = numero(linha.get("id"));
            String identificador = String.valueOf(linha.get("identificador"));
            LocalDateTime recebidaEm = dataHora(linha.get("recebido_em"));
            if (ControlRoomEquipamentoRegras.telemetriaAtrasada(recebidaEm, agora)) {
                alterou |= abrirAlarme(equipamentoId, "TELEMETRIA_ATRASADA", "ALTA",
                        "Telemetria ausente ou sem atualizacao ha mais de dois minutos.",
                        "MONITOR_SAUDE", Map.of("equipamento", identificador));
            } else {
                alterou |= resolverAlarme(equipamentoId, "TELEMETRIA_ATRASADA", "MONITOR_SAUDE") > 0;
            }

            Long dispositivoId = numero(linha.get("dispositivo_id"));
            if (dispositivoId == null) {
                continue;
            }
            LocalDateTime heartbeat = dataHora(linha.get("ultimo_heartbeat_em"));
            boolean conectado = ControlRoomEquipamentoRegras.dispositivoConectado(heartbeat, agora)
                    && "CONECTADO".equals(String.valueOf(linha.get("status_integracao")));
            if (!conectado) {
                jdbc.update("""
                        UPDATE control_room_dispositivo
                        SET status_integracao = 'DESCONECTADO', atualizado_em = CURRENT_TIMESTAMP
                        WHERE id = :id AND status_integracao <> 'DESCONECTADO'
                        """, Map.of("id", dispositivoId));
                alterou |= abrirAlarme(equipamentoId, "DISPOSITIVO_DESCONECTADO", "CRITICA",
                        "Dispositivo sem heartbeat dentro da janela operacional.",
                        "MONITOR_SAUDE", Map.of("dispositivo", String.valueOf(linha.get("dispositivo"))));
            } else {
                alterou |= resolverAlarme(equipamentoId, "DISPOSITIVO_DESCONECTADO", "MONITOR_SAUDE") > 0;
            }
        }
        return alterou;
    }

    private ControlRoomEquipamentoDtos.Resumo resumoSemReavaliar() {
        LocalDateTime agora = LocalDateTime.now();
        MapSqlParameterSource parametros = new MapSqlParameterSource()
                .addValue("limiteHeartbeat", agora.minus(ControlRoomEquipamentoRegras.LIMITE_HEARTBEAT))
                .addValue("limiteTelemetria", agora.minus(ControlRoomEquipamentoRegras.LIMITE_TELEMETRIA));
        return jdbc.queryForObject("""
                SELECT
                    COUNT(*) AS total_equipamentos,
                    COALESCE(SUM(CASE WHEN e.status_operacional = 'OPERACIONAL' THEN 1 ELSE 0 END), 0) AS operacionais,
                    COALESCE(SUM(CASE WHEN e.status_operacional = 'MANUTENCAO' THEN 1 ELSE 0 END), 0) AS manutencao,
                    COALESCE(SUM(CASE WHEN e.status_operacional = 'INDISPONIVEL' THEN 1 ELSE 0 END), 0) AS indisponiveis,
                    (SELECT COUNT(*) FROM control_room_dispositivo d WHERE d.status_integracao = 'CONECTADO' AND d.ultimo_heartbeat_em >= :limiteHeartbeat) AS conectados,
                    (SELECT COUNT(*) FROM equipamento_patio ep LEFT JOIN telemetria_equipamento_patio t ON t.equipamento_id = ep.id WHERE t.recebido_em IS NULL OR t.recebido_em < :limiteTelemetria) AS telemetria_atrasada,
                    (SELECT COUNT(*) FROM control_room_alarme a WHERE a.status IN ('ATIVO', 'RECONHECIDO')) AS alarmes_ativos,
                    (SELECT COUNT(*) FROM control_room_comando c WHERE c.status IN ('PENDENTE', 'ENVIADO')) AS comandos_pendentes,
                    (SELECT COUNT(*) FROM control_room_indisponibilidade i WHERE i.fim_em IS NULL) AS indisponibilidades_abertas
                FROM equipamento_patio e
                """, parametros, (rs, rowNum) -> new ControlRoomEquipamentoDtos.Resumo(
                rs.getLong("total_equipamentos"), rs.getLong("operacionais"), rs.getLong("manutencao"),
                rs.getLong("indisponiveis"), rs.getLong("conectados"), rs.getLong("telemetria_atrasada"),
                rs.getLong("alarmes_ativos"), rs.getLong("comandos_pendentes"),
                rs.getLong("indisponibilidades_abertas"), agora));
    }

    private void aplicarEfeitoComando(ControlRoomEquipamentoDtos.Comando comando, String dispositivo) {
        EquipamentoPatio equipamento = equipamento(comando.equipamento());
        if ("INDISPONIBILIZAR".equals(comando.tipo())) {
            abrirIndisponibilidade(equipamento, "COMANDO_REMOTO",
                    comando.mensagem(), dispositivo, comando.id());
        } else if ("DISPONIBILIZAR".equals(comando.tipo())) {
            List<ControlRoomEquipamentoDtos.Indisponibilidade> abertas = listarIndisponibilidades(equipamento.getIdentificador()).stream()
                    .filter(item -> item.fimEm() == null)
                    .toList();
            for (ControlRoomEquipamentoDtos.Indisponibilidade aberta : abertas) {
                jdbc.update("""
                        UPDATE control_room_indisponibilidade
                        SET fim_em = CURRENT_TIMESTAMP, encerrado_por = :usuario
                        WHERE id = :id AND fim_em IS NULL
                        """, Map.of("usuario", dispositivo, "id", aberta.id()));
            }
            equipamento.setStatusOperacional(StatusEquipamento.OPERACIONAL);
            equipamentoRepositorio.save(equipamento);
            resolverAlarme(equipamento.getId(), "EQUIPAMENTO_INDISPONIVEL", dispositivo);
        }
    }

    private Long abrirIndisponibilidade(
            EquipamentoPatio equipamento,
            String motivo,
            String observacao,
            String usuario,
            Long comandoId
    ) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        try {
            jdbc.update("""
                    INSERT INTO control_room_indisponibilidade (
                        equipamento_id, motivo, observacao, inicio_em, aberto_por, comando_origem_id
                    ) VALUES (
                        :equipamentoId, :motivo, :observacao, CURRENT_TIMESTAMP, :usuario, :comandoId
                    )
                    """, new MapSqlParameterSource()
                    .addValue("equipamentoId", equipamento.getId())
                    .addValue("motivo", limparObrigatorio(motivo, 120, "Motivo"))
                    .addValue("observacao", limpar(observacao, 1000))
                    .addValue("usuario", usuario(usuario))
                    .addValue("comandoId", comandoId), keyHolder, new String[]{"id"});
        } catch (DuplicateKeyException erro) {
            throw new IllegalArgumentException("O equipamento ja possui uma indisponibilidade aberta.", erro);
        }
        equipamento.setStatusOperacional(StatusEquipamento.INDISPONIVEL);
        equipamentoRepositorio.save(equipamento);
        abrirAlarme(equipamento.getId(), "EQUIPAMENTO_INDISPONIVEL", "ALTA",
                "Equipamento marcado como indisponivel: " + limparObrigatorio(motivo, 120, "Motivo") + ".",
                "CONTROL_ROOM", null);
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    private void atualizarEstadoViaTelemetria(EquipamentoPatio equipamento, String statusVmt) {
        if (!StringUtils.hasText(statusVmt)) {
            return;
        }
        String status = statusVmt.trim().toUpperCase(Locale.ROOT);
        if (status.contains("ERRO") || status.contains("FAULT") || status.contains("OFFLINE") || status.contains("INDISPONIVEL")) {
            equipamento.setStatusOperacional(StatusEquipamento.INDISPONIVEL);
            equipamentoRepositorio.save(equipamento);
            abrirAlarme(equipamento.getId(), "FALHA_DISPOSITIVO", "CRITICA",
                    "A telemetria informou estado de falha: " + status + ".", "TELEMETRIA", Map.of("statusVmt", status));
        } else if (status.contains("MANUTENCAO") || status.contains("MAINTENANCE")) {
            equipamento.setStatusOperacional(StatusEquipamento.MANUTENCAO);
            equipamentoRepositorio.save(equipamento);
        } else if (status.contains("ONLINE") || status.contains("OPERACIONAL") || status.contains("AVAILABLE")
                || status.contains("WORKING") || status.contains("IDLE")) {
            if (!possuiIndisponibilidadeAberta(equipamento.getId())) {
                equipamento.setStatusOperacional(StatusEquipamento.OPERACIONAL);
                equipamentoRepositorio.save(equipamento);
                resolverAlarme(equipamento.getId(), "FALHA_DISPOSITIVO", "TELEMETRIA");
            }
        }
    }

    private void atualizarDispositivoPelaOrigem(Long equipamentoId, TelemetriaEquipamentoPatioDto telemetria) {
        if (!StringUtils.hasText(telemetria.origem())) {
            return;
        }
        jdbc.update("""
                UPDATE control_room_dispositivo
                SET ultimo_heartbeat_em = :recebidoEm,
                    ultima_sequencia = CASE
                        WHEN ultima_sequencia IS NULL OR ultima_sequencia < :sequencia THEN :sequencia
                        ELSE ultima_sequencia END,
                    status_integracao = 'CONECTADO',
                    atualizado_em = CURRENT_TIMESTAMP
                WHERE equipamento_id = :equipamentoId AND UPPER(identificador) = UPPER(:origem)
                """, new MapSqlParameterSource()
                .addValue("recebidoEm", telemetria.recebidoEm() == null ? LocalDateTime.now() : telemetria.recebidoEm())
                .addValue("sequencia", telemetria.sequencia())
                .addValue("equipamentoId", equipamentoId)
                .addValue("origem", telemetria.origem()));
    }

    private boolean abrirAlarme(
            Long equipamentoId,
            String tipo,
            String severidade,
            String mensagem,
            String origem,
            Map<String, Object> detalhes
    ) {
        try {
            int inseridos = jdbc.update("""
                    INSERT INTO control_room_alarme (
                        equipamento_id, tipo, severidade, status, mensagem, origem, detalhes_json, aberto_em
                    )
                    SELECT :equipamentoId, :tipo, :severidade, 'ATIVO', :mensagem, :origem, :detalhes, CURRENT_TIMESTAMP
                    WHERE NOT EXISTS (
                        SELECT 1 FROM control_room_alarme
                        WHERE equipamento_id = :equipamentoId
                          AND tipo = :tipo
                          AND status IN ('ATIVO', 'RECONHECIDO')
                    )
                    """, new MapSqlParameterSource()
                    .addValue("equipamentoId", equipamentoId)
                    .addValue("tipo", tipo)
                    .addValue("severidade", severidade)
                    .addValue("mensagem", mensagem)
                    .addValue("origem", origem)
                    .addValue("detalhes", escreverJson(detalhes)));
            return inseridos > 0;
        } catch (DuplicateKeyException ignorada) {
            return false;
        }
    }

    private int resolverAlarme(Long equipamentoId, String tipo, String usuario) {
        return jdbc.update("""
                UPDATE control_room_alarme
                SET status = 'RESOLVIDO', resolvido_em = CURRENT_TIMESTAMP, resolvido_por = :usuario
                WHERE equipamento_id = :equipamentoId
                  AND tipo = :tipo
                  AND status IN ('ATIVO', 'RECONHECIDO')
                """, new MapSqlParameterSource()
                .addValue("usuario", usuario(usuario))
                .addValue("equipamentoId", equipamentoId)
                .addValue("tipo", tipo));
    }

    private boolean possuiIndisponibilidadeAberta(Long equipamentoId) {
        Integer quantidade = jdbc.queryForObject("""
                SELECT COUNT(*) FROM control_room_indisponibilidade
                WHERE equipamento_id = :equipamentoId AND fim_em IS NULL
                """, Map.of("equipamentoId", equipamentoId), Integer.class);
        return quantidade != null && quantidade > 0;
    }

    private EquipamentoPatio equipamento(String identificador) {
        String normalizado = ControlRoomEquipamentoRegras.normalizarIdentificador(identificador);
        return equipamentoRepositorio.findByIdentificador(normalizado)
                .orElseThrow(() -> new IllegalArgumentException("Equipamento de patio nao encontrado: " + identificador + "."));
    }

    private Optional<Long> dispositivoId(Long equipamentoId) {
        try {
            return Optional.ofNullable(jdbc.queryForObject("""
                    SELECT id FROM control_room_dispositivo WHERE equipamento_id = :equipamentoId
                    """, Map.of("equipamentoId", equipamentoId), Long.class));
        } catch (EmptyResultDataAccessException erro) {
            return Optional.empty();
        }
    }

    private Optional<ControlRoomEquipamentoDtos.Dispositivo> dispositivoOpcional(String identificador) {
        try {
            return Optional.of(obterDispositivo(identificador));
        } catch (IllegalArgumentException erro) {
            return Optional.empty();
        }
    }

    private ControlRoomEquipamentoDtos.Dispositivo obterDispositivo(String identificador) {
        String normalizado = ControlRoomEquipamentoRegras.normalizarIdentificador(identificador);
        try {
            return jdbc.queryForObject("""
                    SELECT d.*, e.identificador AS equipamento, e.tipo_equipamento
                    FROM control_room_dispositivo d
                    JOIN equipamento_patio e ON e.id = d.equipamento_id
                    WHERE d.identificador = :identificador
                    """, Map.of("identificador", normalizado), this::mapearDispositivo);
        } catch (EmptyResultDataAccessException erro) {
            throw new IllegalArgumentException("Dispositivo nao encontrado: " + identificador + ".", erro);
        }
    }

    private ControlRoomEquipamentoDtos.Comando obterComando(Long id) {
        try {
            return jdbc.queryForObject("""
                    SELECT c.*, e.identificador AS equipamento, d.identificador AS dispositivo
                    FROM control_room_comando c
                    JOIN equipamento_patio e ON e.id = c.equipamento_id
                    LEFT JOIN control_room_dispositivo d ON d.id = c.dispositivo_id
                    WHERE c.id = :id
                    """, Map.of("id", id), this::mapearComando);
        } catch (EmptyResultDataAccessException erro) {
            throw new IllegalArgumentException("Comando nao encontrado: " + id + ".", erro);
        }
    }

    private ControlRoomEquipamentoDtos.Indisponibilidade obterIndisponibilidade(Long id) {
        try {
            return jdbc.queryForObject("""
                    SELECT i.*, e.identificador AS equipamento, e.tipo_equipamento
                    FROM control_room_indisponibilidade i
                    JOIN equipamento_patio e ON e.id = i.equipamento_id
                    WHERE i.id = :id
                    """, Map.of("id", id), this::mapearIndisponibilidade);
        } catch (EmptyResultDataAccessException erro) {
            throw new IllegalArgumentException("Indisponibilidade nao encontrada: " + id + ".", erro);
        }
    }

    private ControlRoomEquipamentoDtos.Alarme obterAlarme(Long id) {
        try {
            return jdbc.queryForObject("""
                    SELECT a.*, e.identificador AS equipamento, e.tipo_equipamento
                    FROM control_room_alarme a
                    JOIN equipamento_patio e ON e.id = a.equipamento_id
                    WHERE a.id = :id
                    """, Map.of("id", id), this::mapearAlarme);
        } catch (EmptyResultDataAccessException erro) {
            throw new IllegalArgumentException("Alarme nao encontrado: " + id + ".", erro);
        }
    }

    private ControlRoomEquipamentoDtos.Equipamento mapearEquipamento(ResultSet rs, int rowNum) throws SQLException {
        return new ControlRoomEquipamentoDtos.Equipamento(
                rs.getLong("id"), rs.getString("identificador"), rs.getString("tipo_equipamento"),
                rs.getString("status_operacional"), inteiro(rs, "linha"), inteiro(rs, "coluna"),
                rs.getString("conectividade"), rs.getString("dispositivo"), rs.getString("protocolo"),
                rs.getString("firmware"), dataHora(rs, "ultimo_heartbeat_em"),
                numeroDouble(rs, "latitude"), numeroDouble(rs, "longitude"), numeroDouble(rs, "coordenada_x"),
                numeroDouble(rs, "coordenada_y"), numeroDouble(rs, "heading"), rs.getString("posicao_mais_proxima"),
                booleano(rs, "dentro_da_posicao"), rs.getString("status_vmt"), numeroLong(rs, "work_instruction_atual_id"),
                numeroLong(rs, "sequencia_telemetria"), dataHora(rs, "capturado_em"), dataHora(rs, "recebido_em"),
                rs.getLong("alarmes_ativos"), numeroLong(rs, "indisponibilidade_aberta_id")
        );
    }

    private ControlRoomEquipamentoDtos.HistoricoTelemetria mapearHistorico(ResultSet rs, int rowNum) throws SQLException {
        return new ControlRoomEquipamentoDtos.HistoricoTelemetria(
                rs.getLong("id"), rs.getString("equipamento"), numeroDouble(rs, "latitude"),
                numeroDouble(rs, "longitude"), numeroDouble(rs, "coordenada_x"), numeroDouble(rs, "coordenada_y"),
                numeroDouble(rs, "heading"), rs.getString("posicao_mais_proxima"),
                inteiro(rs, "distancia_posicao_centimetros"), booleano(rs, "dentro_da_posicao"),
                rs.getString("origem"), rs.getString("operador_vmt"), rs.getString("status_vmt"),
                numeroLong(rs, "work_instruction_atual_id"), rs.getLong("sequencia"),
                dataHora(rs, "capturado_em"), dataHora(rs, "recebido_em")
        );
    }

    private ControlRoomEquipamentoDtos.Alarme mapearAlarme(ResultSet rs, int rowNum) throws SQLException {
        return new ControlRoomEquipamentoDtos.Alarme(
                rs.getLong("id"), rs.getString("equipamento"), rs.getString("tipo_equipamento"),
                rs.getString("tipo"), rs.getString("severidade"), rs.getString("status"),
                rs.getString("mensagem"), rs.getString("origem"), lerJson(rs.getString("detalhes_json")),
                dataHora(rs, "aberto_em"), dataHora(rs, "reconhecido_em"), rs.getString("reconhecido_por"),
                dataHora(rs, "resolvido_em"), rs.getString("resolvido_por")
        );
    }

    private ControlRoomEquipamentoDtos.Comando mapearComando(ResultSet rs, int rowNum) throws SQLException {
        return new ControlRoomEquipamentoDtos.Comando(
                rs.getLong("id"), rs.getString("equipamento"), rs.getString("dispositivo"),
                rs.getString("tipo"), rs.getString("status"), lerJson(rs.getString("parametros_json")),
                rs.getString("mensagem"), rs.getString("solicitado_por"), rs.getString("correlation_id"),
                dataHora(rs, "criado_em"), dataHora(rs, "enviado_em"), dataHora(rs, "confirmado_em"),
                rs.getString("retorno_dispositivo"), numeroLong(rs, "sequencia_dispositivo")
        );
    }

    private ControlRoomEquipamentoDtos.Indisponibilidade mapearIndisponibilidade(ResultSet rs, int rowNum) throws SQLException {
        return new ControlRoomEquipamentoDtos.Indisponibilidade(
                rs.getLong("id"), rs.getString("equipamento"), rs.getString("tipo_equipamento"),
                rs.getString("motivo"), rs.getString("observacao"), dataHora(rs, "inicio_em"),
                dataHora(rs, "fim_em"), rs.getString("aberto_por"), rs.getString("encerrado_por"),
                numeroLong(rs, "comando_origem_id")
        );
    }

    private ControlRoomEquipamentoDtos.Dispositivo mapearDispositivo(ResultSet rs, int rowNum) throws SQLException {
        return new ControlRoomEquipamentoDtos.Dispositivo(
                rs.getLong("id"), rs.getString("identificador"), rs.getString("equipamento"),
                rs.getString("tipo_equipamento"), rs.getString("protocolo"), rs.getString("status_integracao"),
                rs.getString("firmware"), rs.getString("endereco_rede"), numeroLong(rs, "ultima_sequencia"),
                dataHora(rs, "ultimo_heartbeat_em"), dataHora(rs, "atualizado_em")
        );
    }

    private boolean semFiltroOuIgual(String filtro, String valor) {
        return !StringUtils.hasText(filtro) || filtro.trim().equalsIgnoreCase(valor);
    }

    private String escreverJson(Map<String, Object> valor) {
        if (valor == null || valor.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(valor);
        } catch (JsonProcessingException erro) {
            throw new IllegalArgumentException("Os parametros informados nao podem ser serializados.", erro);
        }
    }

    private Map<String, Object> lerJson(String valor) {
        if (!StringUtils.hasText(valor)) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(valor, MAP_TYPE);
        } catch (JsonProcessingException erro) {
            return Map.of("conteudoInvalido", valor);
        }
    }

    private String usuario(String valor) {
        return StringUtils.hasText(valor) ? valor.trim() : "sistema";
    }

    private String limpar(String valor, int limite) {
        if (!StringUtils.hasText(valor)) {
            return null;
        }
        String limpo = valor.trim();
        return limpo.length() <= limite ? limpo : limpo.substring(0, limite);
    }

    private String limparObrigatorio(String valor, int limite, String campo) {
        String limpo = limpar(valor, limite);
        if (limpo == null) {
            throw new IllegalArgumentException(campo + " e obrigatorio.");
        }
        return limpo;
    }

    private Long numero(Object valor) {
        return valor instanceof Number numero ? numero.longValue() : null;
    }

    private LocalDateTime dataHora(Object valor) {
        if (valor instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        return valor instanceof LocalDateTime dataHora ? dataHora : null;
    }

    private LocalDateTime dataHora(ResultSet rs, String coluna) throws SQLException {
        Timestamp valor = rs.getTimestamp(coluna);
        return valor == null ? null : valor.toLocalDateTime();
    }

    private Long numeroLong(ResultSet rs, String coluna) throws SQLException {
        long valor = rs.getLong(coluna);
        return rs.wasNull() ? null : valor;
    }

    private Integer inteiro(ResultSet rs, String coluna) throws SQLException {
        int valor = rs.getInt(coluna);
        return rs.wasNull() ? null : valor;
    }

    private Double numeroDouble(ResultSet rs, String coluna) throws SQLException {
        double valor = rs.getDouble(coluna);
        return rs.wasNull() ? null : valor;
    }

    private Boolean booleano(ResultSet rs, String coluna) throws SQLException {
        boolean valor = rs.getBoolean(coluna);
        return rs.wasNull() ? null : valor;
    }
}
