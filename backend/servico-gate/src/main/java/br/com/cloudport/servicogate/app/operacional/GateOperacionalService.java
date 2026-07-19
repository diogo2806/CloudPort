package br.com.cloudport.servicogate.app.operacional;

import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.AdvanceVisitRequest;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.AttachmentDTO;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.AttachmentRequest;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.BookingDTO;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.BookingRequest;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.BusinessTaskDTO;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.BusinessTaskRequest;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.CapacityDTO;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.DocumentDTO;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.DocumentRequest;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.FacilityDTO;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.FacilityRequest;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.GateDTO;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.GateOperationalDashboardDTO;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.GateRequest;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.InspectionDTO;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.InspectionRequest;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.LaneDTO;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.LaneRequest;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.OrderDTO;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.OrderRequest;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.PreadviceDTO;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.PreadviceRequest;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.ReferenceCatalogDTO;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.StageDTO;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.StageRequest;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.TransactionDTO;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.TransactionRequest;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.TransferDTO;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.TransferRequest;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.TroubleDTO;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.TroubleRequest;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.TroubleResolutionRequest;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.TruckVisitDTO;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.TruckVisitRequest;
import br.com.cloudport.servicogate.exception.BusinessException;
import br.com.cloudport.servicogate.exception.NotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class GateOperacionalService {

    private static final Set<String> DIRECOES = Set.of("ENTRADA", "SAIDA", "BIDIRECIONAL");
    private static final Set<String> STATUS_LANE = Set.of("ABERTA", "FECHADA", "MANUTENCAO");
    private static final Set<String> TIPOS_TASK = Set.of("VALIDACAO", "CAPTURA", "INTEGRACAO", "IMPRESSAO", "DECISAO");
    private static final Set<String> TIPOS_ORDEM = Set.of("EDO", "ERO", "IDO");
    private static final Set<String> TIPOS_PREADVICE = Set.of("EXPORTACAO", "VAZIO");
    private static final Set<String> SEVERIDADES = Set.of("BAIXA", "MEDIA", "ALTA", "CRITICA");
    private static final Set<String> RESULTADOS_INSPECAO = Set.of("APROVADO", "REPROVADO", "COM_RESSALVA");
    private static final Set<String> CATEGORIAS_ANEXO = Set.of("FOTOGRAFIA", "DOCUMENTO", "AVARIA", "OCR", "INSPECAO");
    private static final Set<String> TIPOS_DOCUMENTO = Set.of("TICKET", "EIR", "COMPROVANTE_TRANSFERENCIA");
    private static final DateTimeFormatter CODIGO_DATA = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public GateOperacionalService(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public GateOperationalDashboardDTO painel(Long facilityId) {
        List<FacilityDTO> facilities = listarFacilities();
        if (facilities.isEmpty()) {
            throw new BusinessException("Nenhuma instalação de Gate está configurada.");
        }
        Long selecionada = facilityId != null ? facilityId : facilities.get(0).id();
        facilities.stream().filter(item -> item.id().equals(selecionada)).findFirst()
                .orElseThrow(() -> new NotFoundException("Instalação de Gate não encontrada."));
        List<GateDTO> gates = listarGates(selecionada);
        List<LaneDTO> lanes = listarLanes(selecionada);
        List<StageDTO> stages = listarStages(selecionada);
        List<TruckVisitDTO> visitas = listarVisitasAtivas(selecionada);
        List<TroubleDTO> troubles = listarTroublesAbertos(selecionada);
        return new GateOperationalDashboardDTO(
                selecionada,
                facilities,
                gates,
                lanes,
                stages,
                visitas,
                troubles,
                consultarCapacidade(selecionada),
                listarReferencias(),
                LocalDateTime.now());
    }

    @Transactional
    public FacilityDTO salvarFacility(FacilityRequest request) {
        String codigo = normalizarCodigo(request.codigo());
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("codigo", codigo)
                .addValue("nome", normalizarTexto(request.nome(), "O nome da instalação deve ser informado."))
                .addValue("fusoHorario", normalizarTexto(request.fusoHorario(), "O fuso horário deve ser informado."))
                .addValue("ativo", request.ativo() == null || request.ativo());
        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO gate_facility (codigo, nome, fuso_horario, ativo)
                VALUES (:codigo, :nome, :fusoHorario, :ativo)
                ON CONFLICT (codigo) DO UPDATE SET
                    nome = EXCLUDED.nome,
                    fuso_horario = EXCLUDED.fuso_horario,
                    ativo = EXCLUDED.ativo,
                    updated_at = NOW()
                RETURNING id
                """, parameters, Long.class);
        return buscarFacility(id);
    }

    @Transactional
    public GateDTO salvarGate(GateRequest request) {
        buscarFacility(request.facilityId());
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("facilityId", request.facilityId())
                .addValue("codigo", normalizarCodigo(request.codigo()))
                .addValue("nome", normalizarTexto(request.nome(), "O nome do Gate deve ser informado."))
                .addValue("ativo", request.ativo() == null || request.ativo());
        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO gate_configuracao (facility_id, codigo, nome, ativo)
                VALUES (:facilityId, :codigo, :nome, :ativo)
                ON CONFLICT (facility_id, codigo) DO UPDATE SET
                    nome = EXCLUDED.nome,
                    ativo = EXCLUDED.ativo,
                    updated_at = NOW()
                RETURNING id
                """, parameters, Long.class);
        return buscarGate(id);
    }

    @Transactional
    public LaneDTO salvarLane(LaneRequest request) {
        buscarGate(request.gateId());
        String direcao = normalizarEnum(request.direcao(), DIRECOES, "Direção de pista inválida.");
        String status = normalizarEnum(request.status(), STATUS_LANE, "Status de pista inválido.");
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("gateId", request.gateId())
                .addValue("codigo", normalizarCodigo(request.codigo()))
                .addValue("nome", normalizarTexto(request.nome(), "O nome da pista deve ser informado."))
                .addValue("direcao", direcao)
                .addValue("status", status)
                .addValue("capacidadeFila", request.capacidadeFila())
                .addValue("ocr", Boolean.TRUE.equals(request.ocrHabilitado()))
                .addValue("balanca", Boolean.TRUE.equals(request.balancaHabilitada()))
                .addValue("inspecao", Boolean.TRUE.equals(request.inspecaoHabilitada()));
        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO gate_lane (
                    gate_id, codigo, nome, direcao, status, capacidade_fila,
                    ocr_habilitado, balanca_habilitada, inspecao_habilitada
                ) VALUES (
                    :gateId, :codigo, :nome, :direcao, :status, :capacidadeFila,
                    :ocr, :balanca, :inspecao
                )
                ON CONFLICT (gate_id, codigo) DO UPDATE SET
                    nome = EXCLUDED.nome,
                    direcao = EXCLUDED.direcao,
                    status = EXCLUDED.status,
                    capacidade_fila = EXCLUDED.capacidade_fila,
                    ocr_habilitado = EXCLUDED.ocr_habilitado,
                    balanca_habilitada = EXCLUDED.balanca_habilitada,
                    inspecao_habilitada = EXCLUDED.inspecao_habilitada,
                    updated_at = NOW()
                RETURNING id
                """, parameters, Long.class);
        return buscarLane(id);
    }

    @Transactional
    public StageDTO salvarStage(StageRequest request) {
        buscarGate(request.gateId());
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("gateId", request.gateId())
                .addValue("codigo", normalizarCodigo(request.codigo()))
                .addValue("nome", normalizarTexto(request.nome(), "O nome do estágio deve ser informado."))
                .addValue("ordem", request.ordem())
                .addValue("ativo", request.ativo() == null || request.ativo())
                .addValue("permiteTrouble", request.permiteTrouble() == null || request.permiteTrouble())
                .addValue("finalizaVisita", Boolean.TRUE.equals(request.finalizaVisita()));
        Long id;
        try {
            id = jdbcTemplate.queryForObject("""
                    INSERT INTO gate_stage_config (
                        gate_id, codigo, nome, ordem, ativo, permite_trouble, finaliza_visita
                    ) VALUES (
                        :gateId, :codigo, :nome, :ordem, :ativo, :permiteTrouble, :finalizaVisita
                    )
                    ON CONFLICT (gate_id, codigo) DO UPDATE SET
                        nome = EXCLUDED.nome,
                        ordem = EXCLUDED.ordem,
                        ativo = EXCLUDED.ativo,
                        permite_trouble = EXCLUDED.permite_trouble,
                        finaliza_visita = EXCLUDED.finaliza_visita,
                        updated_at = NOW()
                    RETURNING id
                    """, parameters, Long.class);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException("Já existe outro estágio na mesma ordem para este Gate.");
        }
        if (StringUtils.hasText(request.codigoStageAnterior())) {
            Long anteriorId = consultarLong("""
                    SELECT id FROM gate_stage_config
                     WHERE gate_id = :gateId AND codigo = :codigo AND ativo = TRUE
                    """, new MapSqlParameterSource()
                    .addValue("gateId", request.gateId())
                    .addValue("codigo", normalizarCodigo(request.codigoStageAnterior())),
                    "Estágio anterior não encontrado.");
            jdbcTemplate.update("""
                    INSERT INTO gate_stage_transition (origem_stage_id, destino_stage_id, ativo)
                    VALUES (:origemId, :destinoId, TRUE)
                    ON CONFLICT (origem_stage_id, destino_stage_id) DO UPDATE SET ativo = TRUE
                    """, new MapSqlParameterSource()
                    .addValue("origemId", anteriorId)
                    .addValue("destinoId", id));
        }
        return buscarStage(id);
    }

    @Transactional
    public BusinessTaskDTO salvarTask(Long stageId, BusinessTaskRequest request) {
        buscarStage(stageId);
        String tipo = normalizarEnum(request.tipo(), TIPOS_TASK, "Tipo de business task inválido.");
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("stageId", stageId)
                .addValue("codigo", normalizarCodigo(request.codigo()))
                .addValue("nome", normalizarTexto(request.nome(), "O nome da business task deve ser informado."))
                .addValue("tipo", tipo)
                .addValue("ordem", request.ordem())
                .addValue("obrigatoria", request.obrigatoria() == null || request.obrigatoria())
                .addValue("ativa", request.ativa() == null || request.ativa())
                .addValue("configuracao", escreverJson(request.configuracao()));
        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO gate_business_task (
                    stage_id, codigo, nome, tipo, ordem, obrigatoria, ativa, configuracao
                ) VALUES (
                    :stageId, :codigo, :nome, :tipo, :ordem, :obrigatoria, :ativa,
                    CAST(:configuracao AS JSONB)
                )
                ON CONFLICT (stage_id, codigo) DO UPDATE SET
                    nome = EXCLUDED.nome,
                    tipo = EXCLUDED.tipo,
                    ordem = EXCLUDED.ordem,
                    obrigatoria = EXCLUDED.obrigatoria,
                    ativa = EXCLUDED.ativa,
                    configuracao = EXCLUDED.configuracao,
                    updated_at = NOW()
                RETURNING id
                """, parameters, Long.class);
        return buscarTask(id);
    }

    @Transactional
    public BookingDTO salvarBooking(BookingRequest request) {
        validarVigencia(request.validadeInicio(), request.validadeFim());
        if (request.transportadoraId() != null) {
            garantirExiste("transportadora", request.transportadoraId(), "Transportadora não encontrada.");
        }
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("codigo", normalizarCodigo(request.codigo()))
                .addValue("transportadoraId", request.transportadoraId())
                .addValue("armador", limpar(request.armador()))
                .addValue("viagem", limpar(request.viagem()))
                .addValue("quantidadeTotal", request.quantidadeTotal())
                .addValue("validadeInicio", request.validadeInicio())
                .addValue("validadeFim", request.validadeFim())
                .addValue("observacoes", limpar(request.observacoes()));
        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO gate_booking (
                    codigo, transportadora_id, armador, viagem, quantidade_total,
                    validade_inicio, validade_fim, observacoes
                ) VALUES (
                    :codigo, :transportadoraId, :armador, :viagem, :quantidadeTotal,
                    :validadeInicio, :validadeFim, :observacoes
                )
                ON CONFLICT (codigo) DO UPDATE SET
                    transportadora_id = EXCLUDED.transportadora_id,
                    armador = EXCLUDED.armador,
                    viagem = EXCLUDED.viagem,
                    quantidade_total = GREATEST(EXCLUDED.quantidade_total, gate_booking.quantidade_utilizada),
                    validade_inicio = EXCLUDED.validade_inicio,
                    validade_fim = EXCLUDED.validade_fim,
                    observacoes = EXCLUDED.observacoes,
                    updated_at = NOW()
                RETURNING id
                """, parameters, Long.class);
        return buscarBooking(id);
    }

    @Transactional
    public OrderDTO salvarOrder(OrderRequest request) {
        validarVigencia(request.validadeInicio(), request.validadeFim());
        String tipo = normalizarEnum(request.tipo(), TIPOS_ORDEM, "Tipo de ordem inválido.");
        if (request.bookingId() != null) {
            buscarBooking(request.bookingId());
        }
        if (request.transportadoraId() != null) {
            garantirExiste("transportadora", request.transportadoraId(), "Transportadora não encontrada.");
        }
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("tipo", tipo)
                .addValue("codigo", normalizarCodigo(request.codigo()))
                .addValue("bookingId", request.bookingId())
                .addValue("transportadoraId", request.transportadoraId())
                .addValue("unidadeReferencia", normalizarUnidade(request.unidadeReferencia()))
                .addValue("validadeInicio", request.validadeInicio())
                .addValue("validadeFim", request.validadeFim());
        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO gate_order (
                    tipo, codigo, booking_id, transportadora_id, unidade_referencia,
                    validade_inicio, validade_fim
                ) VALUES (
                    :tipo, :codigo, :bookingId, :transportadoraId, :unidadeReferencia,
                    :validadeInicio, :validadeFim
                )
                ON CONFLICT (tipo, codigo) DO UPDATE SET
                    booking_id = EXCLUDED.booking_id,
                    transportadora_id = EXCLUDED.transportadora_id,
                    unidade_referencia = EXCLUDED.unidade_referencia,
                    validade_inicio = EXCLUDED.validade_inicio,
                    validade_fim = EXCLUDED.validade_fim,
                    updated_at = NOW()
                RETURNING id
                """, parameters, Long.class);
        return buscarOrder(id);
    }

    @Transactional
    public PreadviceDTO salvarPreadvice(PreadviceRequest request) {
        String tipo = normalizarEnum(request.tipo(), TIPOS_PREADVICE, "Tipo de pré-aviso inválido.");
        if (request.bookingId() == null && request.orderId() == null) {
            throw new BusinessException("O pré-aviso deve estar vinculado a um booking ou ordem.");
        }
        if (request.bookingId() != null) {
            buscarBooking(request.bookingId());
        }
        if (request.orderId() != null) {
            buscarOrder(request.orderId());
        }
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("tipo", tipo)
                .addValue("codigo", normalizarCodigo(request.codigo()))
                .addValue("bookingId", request.bookingId())
                .addValue("orderId", request.orderId())
                .addValue("unidadeReferencia", normalizarUnidade(request.unidadeReferencia()))
                .addValue("isoType", limpar(request.isoType()))
                .addValue("pesoBrutoKg", request.pesoBrutoKg());
        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO gate_preadvice (
                    tipo, codigo, booking_id, order_id, unidade_referencia, iso_type, peso_bruto_kg
                ) VALUES (
                    :tipo, :codigo, :bookingId, :orderId, :unidadeReferencia, :isoType, :pesoBrutoKg
                )
                ON CONFLICT (codigo) DO UPDATE SET
                    tipo = EXCLUDED.tipo,
                    booking_id = EXCLUDED.booking_id,
                    order_id = EXCLUDED.order_id,
                    unidade_referencia = EXCLUDED.unidade_referencia,
                    iso_type = EXCLUDED.iso_type,
                    peso_bruto_kg = EXCLUDED.peso_bruto_kg,
                    updated_at = NOW()
                RETURNING id
                """, parameters, Long.class);
        return buscarPreadvice(id);
    }

    @Transactional
    public TruckVisitDTO criarVisita(TruckVisitRequest request) {
        if (request.transacoes() == null || request.transacoes().isEmpty()) {
            throw new BusinessException("A truck visit deve possuir ao menos uma transação.");
        }
        validarEstruturaVisita(request);
        Long stageInicialId = consultarLong("""
                SELECT id FROM gate_stage_config
                 WHERE gate_id = :gateId AND ativo = TRUE
                 ORDER BY ordem
                 LIMIT 1
                """, new MapSqlParameterSource("gateId", request.gateId()),
                "O Gate não possui estágio inicial configurado.");
        String codigo = "TV-" + CODIGO_DATA.format(LocalDateTime.now()) + "-"
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("codigo", codigo)
                .addValue("agendamentoId", request.agendamentoId())
                .addValue("facilityId", request.facilityId())
                .addValue("gateId", request.gateId())
                .addValue("laneId", request.laneId())
                .addValue("transportadoraId", request.transportadoraId())
                .addValue("motoristaId", request.motoristaId())
                .addValue("veiculoId", request.veiculoId())
                .addValue("stageId", stageInicialId);
        Long visitaId = jdbcTemplate.queryForObject("""
                INSERT INTO truck_visit (
                    codigo, agendamento_id, facility_id, gate_id, lane_id, transportadora_id,
                    motorista_id, veiculo_id, stage_atual_id, status, checkin_em, iniciado_em
                ) VALUES (
                    :codigo, :agendamentoId, :facilityId, :gateId, :laneId, :transportadoraId,
                    :motoristaId, :veiculoId, :stageId, 'CHECKIN', NOW(), NOW()
                )
                RETURNING id
                """, parameters, Long.class);
        int sequencia = 1;
        for (TransactionRequest transacao : request.transacoes()) {
            validarReferenciasTransacao(transacao, request.transportadoraId());
            Long transacaoId = jdbcTemplate.queryForObject("""
                    INSERT INTO gate_transaction (
                        truck_visit_id, sequencia, tipo_operacao, status, unidade_referencia,
                        booking_id, order_id, preadvice_id, stage_atual_id
                    ) VALUES (
                        :visitaId, :sequencia, :tipoOperacao, 'EM_PROCESSAMENTO', :unidadeReferencia,
                        :bookingId, :orderId, :preadviceId, :stageId
                    )
                    RETURNING id
                    """, new MapSqlParameterSource()
                    .addValue("visitaId", visitaId)
                    .addValue("sequencia", sequencia++)
                    .addValue("tipoOperacao", normalizarCodigo(transacao.tipoOperacao()))
                    .addValue("unidadeReferencia", normalizarUnidade(transacao.unidadeReferencia()))
                    .addValue("bookingId", transacao.bookingId())
                    .addValue("orderId", transacao.orderId())
                    .addValue("preadviceId", transacao.preadviceId())
                    .addValue("stageId", stageInicialId), Long.class);
            registrarStageEvent(visitaId, transacaoId, null, stageInicialId, request.laneId(), request.usuario(),
                    request.correlationId(), Set.of());
        }
        registrarStageEvent(visitaId, null, null, stageInicialId, request.laneId(), request.usuario(),
                request.correlationId(), Set.of());
        return buscarVisita(visitaId);
    }

    @Transactional
    public TruckVisitDTO avancarVisita(Long visitaId, AdvanceVisitRequest request) {
        Map<String, Object> visita = consultarUmaLinha("""
                SELECT tv.id, tv.gate_id, tv.lane_id, tv.stage_atual_id, tv.status
                  FROM truck_visit tv
                 WHERE tv.id = :id
                """, new MapSqlParameterSource("id", visitaId), "Truck visit não encontrada.");
        if (Set.of("FINALIZADA", "CANCELADA").contains(texto(visita, "status"))) {
            throw new BusinessException("A truck visit não permite avanço no status atual.");
        }
        Long troubleCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                  FROM gate_trouble_case tc
                  JOIN gate_transaction gt ON gt.id = tc.transaction_id
                 WHERE gt.truck_visit_id = :visitaId AND tc.status = 'ABERTO'
                """, new MapSqlParameterSource("visitaId", visitaId), Long.class);
        if (troubleCount != null && troubleCount > 0) {
            throw new BusinessException("Existem trouble transactions abertas para a truck visit.");
        }
        Long stageOrigemId = nullableLong(visita, "stage_atual_id");
        if (stageOrigemId == null) {
            throw new BusinessException("A truck visit não possui estágio atual.");
        }
        validarTarefasConcluidas(stageOrigemId, request.tarefasConcluidas());
        Long stageDestinoId = resolverStageDestino(stageOrigemId, request.destinoStageCodigo());
        Map<String, Object> destino = consultarUmaLinha("""
                SELECT id, finaliza_visita FROM gate_stage_config WHERE id = :id AND ativo = TRUE
                """, new MapSqlParameterSource("id", stageDestinoId), "Estágio de destino não encontrado.");
        Long laneId = request.laneId() != null ? request.laneId() : nullableLong(visita, "lane_id");
        if (laneId != null) {
            validarLaneParaGate(laneId, longValue(visita, "gate_id"));
        }
        boolean finaliza = boolValue(destino, "finaliza_visita");
        jdbcTemplate.update("""
                UPDATE truck_visit
                   SET stage_atual_id = :stageDestinoId,
                       lane_id = :laneId,
                       status = CASE WHEN :finaliza THEN 'FINALIZADA' ELSE 'EM_PROCESSAMENTO' END,
                       checkout_em = CASE WHEN :finaliza THEN NOW() ELSE checkout_em END,
                       finalizado_em = CASE WHEN :finaliza THEN NOW() ELSE finalizado_em END,
                       updated_at = NOW()
                 WHERE id = :visitaId
                """, new MapSqlParameterSource()
                .addValue("stageDestinoId", stageDestinoId)
                .addValue("laneId", laneId)
                .addValue("finaliza", finaliza)
                .addValue("visitaId", visitaId));
        jdbcTemplate.update("""
                UPDATE gate_transaction
                   SET stage_atual_id = :stageDestinoId,
                       status = CASE WHEN :finaliza THEN 'CONCLUIDA' ELSE 'EM_PROCESSAMENTO' END,
                       updated_at = NOW()
                 WHERE truck_visit_id = :visitaId
                   AND status NOT IN ('CANCELADA', 'CONCLUIDA')
                """, new MapSqlParameterSource()
                .addValue("stageDestinoId", stageDestinoId)
                .addValue("finaliza", finaliza)
                .addValue("visitaId", visitaId));
        registrarStageEvent(visitaId, null, stageOrigemId, stageDestinoId, laneId, request.usuario(),
                request.correlationId(), request.tarefasConcluidas());
        List<Long> transacoes = jdbcTemplate.queryForList("""
                SELECT id FROM gate_transaction WHERE truck_visit_id = :visitaId ORDER BY sequencia
                """, new MapSqlParameterSource("visitaId", visitaId), Long.class);
        for (Long transacaoId : transacoes) {
            registrarStageEvent(visitaId, transacaoId, stageOrigemId, stageDestinoId, laneId, request.usuario(),
                    request.correlationId(), request.tarefasConcluidas());
        }
        if (finaliza) {
            consumirReferencias(visitaId);
        }
        return buscarVisita(visitaId);
    }

    @Transactional
    public TroubleDTO abrirTrouble(Long transactionId, TroubleRequest request) {
        String severidade = normalizarEnum(request.severidade(), SEVERIDADES, "Severidade inválida.");
        Map<String, Object> transacao = consultarUmaLinha("""
                SELECT gt.id, gt.truck_visit_id, COALESCE(sc.permite_trouble, TRUE) AS permite_trouble
                  FROM gate_transaction gt
                  JOIN truck_visit tv ON tv.id = gt.truck_visit_id
                  LEFT JOIN gate_stage_config sc ON sc.id = tv.stage_atual_id
                 WHERE gt.id = :id
                """, new MapSqlParameterSource("id", transactionId), "Transação de Gate não encontrada.");
        if (!boolValue(transacao, "permite_trouble")) {
            throw new BusinessException("O estágio atual não permite abertura de trouble transaction.");
        }
        Long abertos = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM gate_trouble_case
                 WHERE transaction_id = :transactionId AND status = 'ABERTO'
                """, new MapSqlParameterSource("transactionId", transactionId), Long.class);
        if (abertos != null && abertos > 0) {
            throw new BusinessException("A transação já possui um trouble aberto.");
        }
        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO gate_trouble_case (
                    transaction_id, codigo, descricao, severidade, aberto_por
                ) VALUES (
                    :transactionId, :codigo, :descricao, :severidade, :usuario
                )
                RETURNING id
                """, new MapSqlParameterSource()
                .addValue("transactionId", transactionId)
                .addValue("codigo", normalizarCodigo(request.codigo()))
                .addValue("descricao", normalizarTexto(request.descricao(), "A descrição do trouble deve ser informada."))
                .addValue("severidade", severidade)
                .addValue("usuario", normalizarTexto(request.usuario(), "O usuário deve ser informado.")), Long.class);
        jdbcTemplate.update("""
                UPDATE gate_transaction
                   SET trouble_ativo = TRUE, status = 'TROUBLE', updated_at = NOW()
                 WHERE id = :transactionId
                """, new MapSqlParameterSource("transactionId", transactionId));
        jdbcTemplate.update("""
                UPDATE truck_visit SET status = 'TROUBLE', updated_at = NOW() WHERE id = :visitaId
                """, new MapSqlParameterSource("visitaId", longValue(transacao, "truck_visit_id")));
        return buscarTrouble(id);
    }

    @Transactional
    public TroubleDTO resolverTrouble(Long troubleId, TroubleResolutionRequest request) {
        Map<String, Object> trouble = consultarUmaLinha("""
                SELECT tc.id, tc.transaction_id, gt.truck_visit_id, tc.status
                  FROM gate_trouble_case tc
                  JOIN gate_transaction gt ON gt.id = tc.transaction_id
                 WHERE tc.id = :id
                """, new MapSqlParameterSource("id", troubleId), "Trouble transaction não encontrada.");
        if (!"ABERTO".equals(texto(trouble, "status"))) {
            throw new BusinessException("O trouble informado já está encerrado.");
        }
        jdbcTemplate.update("""
                UPDATE gate_trouble_case
                   SET status = 'RESOLVIDO', resolvido_por = :usuario, resolvido_em = NOW(),
                       resolucao = :resolucao, updated_at = NOW()
                 WHERE id = :id
                """, new MapSqlParameterSource()
                .addValue("usuario", normalizarTexto(request.usuario(), "O usuário deve ser informado."))
                .addValue("resolucao", normalizarTexto(request.resolucao(), "A resolução deve ser informada."))
                .addValue("id", troubleId));
        Long transactionId = longValue(trouble, "transaction_id");
        jdbcTemplate.update("""
                UPDATE gate_transaction
                   SET trouble_ativo = EXISTS (
                           SELECT 1 FROM gate_trouble_case
                            WHERE transaction_id = :transactionId AND status = 'ABERTO'
                       ),
                       status = CASE WHEN EXISTS (
                           SELECT 1 FROM gate_trouble_case
                            WHERE transaction_id = :transactionId AND status = 'ABERTO'
                       ) THEN 'TROUBLE' ELSE 'EM_PROCESSAMENTO' END,
                       updated_at = NOW()
                 WHERE id = :transactionId
                """, new MapSqlParameterSource("transactionId", transactionId));
        Long visitaId = longValue(trouble, "truck_visit_id");
        jdbcTemplate.update("""
                UPDATE truck_visit
                   SET status = CASE WHEN EXISTS (
                           SELECT 1
                             FROM gate_trouble_case tc
                             JOIN gate_transaction gt ON gt.id = tc.transaction_id
                            WHERE gt.truck_visit_id = :visitaId AND tc.status = 'ABERTO'
                       ) THEN 'TROUBLE' ELSE 'EM_PROCESSAMENTO' END,
                       updated_at = NOW()
                 WHERE id = :visitaId
                """, new MapSqlParameterSource("visitaId", visitaId));
        return buscarTrouble(troubleId);
    }

    @Transactional
    public InspectionDTO registrarInspecao(Long transactionId, InspectionRequest request) {
        String resultado = normalizarEnum(request.resultado(), RESULTADOS_INSPECAO, "Resultado de inspeção inválido.");
        garantirExiste("gate_transaction", transactionId, "Transação de Gate não encontrada.");
        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO gate_inspection (transaction_id, tipo, resultado, inspetor, observacoes)
                VALUES (:transactionId, :tipo, :resultado, :usuario, :observacoes)
                RETURNING id
                """, new MapSqlParameterSource()
                .addValue("transactionId", transactionId)
                .addValue("tipo", normalizarCodigo(request.tipo()))
                .addValue("resultado", resultado)
                .addValue("usuario", normalizarTexto(request.usuario(), "O inspetor deve ser informado."))
                .addValue("observacoes", limpar(request.observacoes())), Long.class);
        if ("REPROVADO".equals(resultado)) {
            Long abertos = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*) FROM gate_trouble_case
                     WHERE transaction_id = :transactionId AND status = 'ABERTO'
                    """, new MapSqlParameterSource("transactionId", transactionId), Long.class);
            if (abertos == null || abertos == 0) {
                abrirTrouble(transactionId, new TroubleRequest(
                        "INSPECAO_REPROVADA",
                        StringUtils.hasText(request.observacoes()) ? request.observacoes() : "Inspeção operacional reprovada.",
                        "ALTA",
                        request.usuario(),
                        request.correlationId()));
            }
        }
        return buscarInspecao(id);
    }

    @Transactional
    public AttachmentDTO anexar(AttachmentRequest request) {
        if (request.truckVisitId() == null && request.transactionId() == null) {
            throw new BusinessException("O anexo deve estar vinculado a uma truck visit ou transação.");
        }
        String categoria = normalizarEnum(request.categoria(), CATEGORIAS_ANEXO, "Categoria de anexo inválida.");
        if (request.truckVisitId() != null) {
            garantirExiste("truck_visit", request.truckVisitId(), "Truck visit não encontrada.");
        }
        if (request.transactionId() != null) {
            garantirExiste("gate_transaction", request.transactionId(), "Transação de Gate não encontrada.");
            if (request.truckVisitId() != null) {
                Long count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*) FROM gate_transaction
                         WHERE id = :transactionId AND truck_visit_id = :truckVisitId
                        """, new MapSqlParameterSource()
                        .addValue("transactionId", request.transactionId())
                        .addValue("truckVisitId", request.truckVisitId()), Long.class);
                if (count == null || count == 0) {
                    throw new BusinessException("A transação não pertence à truck visit informada.");
                }
            }
        }
        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO gate_attachment (
                    truck_visit_id, transaction_id, categoria, nome_arquivo, content_type,
                    url_documento, metadados, criado_por
                ) VALUES (
                    :truckVisitId, :transactionId, :categoria, :nomeArquivo, :contentType,
                    :urlDocumento, CAST(:metadados AS JSONB), :usuario
                )
                RETURNING id
                """, new MapSqlParameterSource()
                .addValue("truckVisitId", request.truckVisitId())
                .addValue("transactionId", request.transactionId())
                .addValue("categoria", categoria)
                .addValue("nomeArquivo", normalizarTexto(request.nomeArquivo(), "O nome do arquivo deve ser informado."))
                .addValue("contentType", limpar(request.contentType()))
                .addValue("urlDocumento", normalizarTexto(request.urlDocumento(), "A URL do documento deve ser informada."))
                .addValue("metadados", escreverJson(request.metadados()))
                .addValue("usuario", normalizarTexto(request.usuario(), "O usuário deve ser informado.")), Long.class);
        return buscarAttachment(id);
    }

    @Transactional
    public DocumentDTO emitirDocumento(Long truckVisitId, DocumentRequest request) {
        garantirExiste("truck_visit", truckVisitId, "Truck visit não encontrada.");
        String tipo = normalizarEnum(request.tipo(), TIPOS_DOCUMENTO, "Tipo de documento inválido.");
        if (request.transactionId() != null) {
            Long count = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*) FROM gate_transaction
                     WHERE id = :transactionId AND truck_visit_id = :truckVisitId
                    """, new MapSqlParameterSource()
                    .addValue("transactionId", request.transactionId())
                    .addValue("truckVisitId", truckVisitId), Long.class);
            if (count == null || count == 0) {
                throw new BusinessException("A transação não pertence à truck visit informada.");
            }
        }
        String numero = tipo + "-" + CODIGO_DATA.format(LocalDateTime.now()) + "-"
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO gate_document (
                    truck_visit_id, transaction_id, tipo, numero, conteudo, emitido_por
                ) VALUES (
                    :truckVisitId, :transactionId, :tipo, :numero, CAST(:conteudo AS JSONB), :usuario
                )
                RETURNING id
                """, new MapSqlParameterSource()
                .addValue("truckVisitId", truckVisitId)
                .addValue("transactionId", request.transactionId())
                .addValue("tipo", tipo)
                .addValue("numero", numero)
                .addValue("conteudo", escreverJson(request.conteudo()))
                .addValue("usuario", normalizarTexto(request.usuario(), "O usuário deve ser informado.")), Long.class);
        return buscarDocumento(id);
    }

    @Transactional
    public DocumentDTO reimprimirDocumento(Long documentoId) {
        int atualizados = jdbcTemplate.update("""
                UPDATE gate_document
                   SET reimpressoes = reimpressoes + 1,
                       ultima_reimpressao_em = NOW(),
                       updated_at = NOW()
                 WHERE id = :id AND status = 'EMITIDO'
                """, new MapSqlParameterSource("id", documentoId));
        if (atualizados == 0) {
            throw new NotFoundException("Documento de Gate emitido não encontrado.");
        }
        return buscarDocumento(documentoId);
    }

    @Transactional
    public TransferDTO solicitarTransferencia(Long truckVisitId, TransferRequest request) {
        Map<String, Object> visita = consultarUmaLinha("""
                SELECT id, facility_id FROM truck_visit WHERE id = :id
                """, new MapSqlParameterSource("id", truckVisitId), "Truck visit não encontrada.");
        buscarFacility(request.facilityDestinoId());
        Long origemId = longValue(visita, "facility_id");
        if (origemId.equals(request.facilityDestinoId())) {
            throw new BusinessException("A instalação de destino deve ser diferente da origem.");
        }
        String codigo = "IFT-" + CODIGO_DATA.format(LocalDateTime.now()) + "-"
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO gate_facility_transfer (
                    codigo, truck_visit_id, facility_origem_id, facility_destino_id,
                    solicitado_por, observacoes
                ) VALUES (
                    :codigo, :truckVisitId, :origemId, :destinoId, :usuario, :observacoes
                )
                RETURNING id
                """, new MapSqlParameterSource()
                .addValue("codigo", codigo)
                .addValue("truckVisitId", truckVisitId)
                .addValue("origemId", origemId)
                .addValue("destinoId", request.facilityDestinoId())
                .addValue("usuario", normalizarTexto(request.usuario(), "O usuário deve ser informado."))
                .addValue("observacoes", limpar(request.observacoes())), Long.class);
        jdbcTemplate.update("""
                UPDATE gate_facility_transfer SET status = 'EM_TRANSITO', updated_at = NOW() WHERE id = :id
                """, new MapSqlParameterSource("id", id));
        return buscarTransferencia(id);
    }

    @Transactional
    public TransferDTO receberTransferencia(Long transferenciaId) {
        Map<String, Object> transferencia = consultarUmaLinha("""
                SELECT id, truck_visit_id, facility_destino_id, status
                  FROM gate_facility_transfer
                 WHERE id = :id
                """, new MapSqlParameterSource("id", transferenciaId), "Transferência não encontrada.");
        if (!Set.of("SOLICITADA", "EM_TRANSITO").contains(texto(transferencia, "status"))) {
            throw new BusinessException("A transferência não pode ser recebida no status atual.");
        }
        Long destinoId = longValue(transferencia, "facility_destino_id");
        Map<String, Object> configuracao = consultarUmaLinha("""
                SELECT gc.id AS gate_id, sc.id AS stage_id
                  FROM gate_configuracao gc
                  JOIN gate_stage_config sc ON sc.gate_id = gc.id AND sc.ativo = TRUE
                 WHERE gc.facility_id = :facilityId AND gc.ativo = TRUE
                 ORDER BY gc.id, sc.ordem
                 LIMIT 1
                """, new MapSqlParameterSource("facilityId", destinoId),
                "A instalação de destino não possui Gate e estágio ativos.");
        jdbcTemplate.update("""
                UPDATE gate_facility_transfer
                   SET status = 'RECEBIDA', recebido_em = NOW(), updated_at = NOW()
                 WHERE id = :id
                """, new MapSqlParameterSource("id", transferenciaId));
        jdbcTemplate.update("""
                UPDATE truck_visit
                   SET facility_id = :facilityId,
                       gate_id = :gateId,
                       lane_id = NULL,
                       stage_atual_id = :stageId,
                       status = 'CHECKIN',
                       updated_at = NOW()
                 WHERE id = :visitaId
                """, new MapSqlParameterSource()
                .addValue("facilityId", destinoId)
                .addValue("gateId", longValue(configuracao, "gate_id"))
                .addValue("stageId", longValue(configuracao, "stage_id"))
                .addValue("visitaId", longValue(transferencia, "truck_visit_id")));
        jdbcTemplate.update("""
                UPDATE gate_transaction
                   SET stage_atual_id = :stageId, status = 'EM_PROCESSAMENTO', updated_at = NOW()
                 WHERE truck_visit_id = :visitaId AND status <> 'CANCELADA'
                """, new MapSqlParameterSource()
                .addValue("stageId", longValue(configuracao, "stage_id"))
                .addValue("visitaId", longValue(transferencia, "truck_visit_id")));
        return buscarTransferencia(transferenciaId);
    }

    @Transactional(readOnly = true)
    public TruckVisitDTO buscarVisita(Long id) {
        Map<String, Object> row = consultarUmaLinha("""
                SELECT tv.id, tv.codigo, tv.agendamento_id, tv.facility_id, tv.gate_id, tv.lane_id,
                       gl.codigo AS lane_codigo, tv.transportadora_id, tr.nome AS transportadora,
                       tv.motorista_id, mo.nome AS motorista, tv.veiculo_id, ve.placa,
                       tv.stage_atual_id, sc.codigo AS stage_codigo, sc.nome AS stage_nome,
                       tv.status, tv.checkin_em, tv.checkout_em, tv.iniciado_em, tv.finalizado_em
                  FROM truck_visit tv
                  JOIN transportadora tr ON tr.id = tv.transportadora_id
                  JOIN motorista mo ON mo.id = tv.motorista_id
                  JOIN veiculo ve ON ve.id = tv.veiculo_id
                  LEFT JOIN gate_lane gl ON gl.id = tv.lane_id
                  LEFT JOIN gate_stage_config sc ON sc.id = tv.stage_atual_id
                 WHERE tv.id = :id
                """, new MapSqlParameterSource("id", id), "Truck visit não encontrada.");
        return mapearVisita(row, listarTransacoes(id));
    }

    @Transactional(readOnly = true)
    public ReferenceCatalogDTO listarReferencias() {
        List<BookingDTO> bookings = jdbcTemplate.queryForList("""
                SELECT id, codigo, transportadora_id, armador, viagem, quantidade_total,
                       quantidade_utilizada, status, validade_inicio, validade_fim, observacoes
                  FROM gate_booking
                 WHERE status IN ('ABERTO', 'PARCIAL')
                 ORDER BY validade_fim NULLS LAST, codigo
                 LIMIT 200
                """, new MapSqlParameterSource()).stream().map(this::mapearBooking).collect(Collectors.toList());
        List<OrderDTO> ordens = jdbcTemplate.queryForList("""
                SELECT id, tipo, codigo, booking_id, transportadora_id, unidade_referencia,
                       status, validade_inicio, validade_fim
                  FROM gate_order
                 WHERE status = 'ATIVA'
                 ORDER BY validade_fim NULLS LAST, tipo, codigo
                 LIMIT 200
                """, new MapSqlParameterSource()).stream().map(this::mapearOrder).collect(Collectors.toList());
        List<PreadviceDTO> preAvisos = jdbcTemplate.queryForList("""
                SELECT id, tipo, codigo, booking_id, order_id, unidade_referencia,
                       iso_type, peso_bruto_kg, status
                  FROM gate_preadvice
                 WHERE status = 'ATIVO'
                 ORDER BY created_at DESC
                 LIMIT 200
                """, new MapSqlParameterSource()).stream().map(this::mapearPreadvice).collect(Collectors.toList());
        return new ReferenceCatalogDTO(bookings, ordens, preAvisos);
    }

    private List<FacilityDTO> listarFacilities() {
        return jdbcTemplate.queryForList("""
                SELECT id, codigo, nome, fuso_horario, ativo
                  FROM gate_facility
                 ORDER BY ativo DESC, nome
                """, new MapSqlParameterSource()).stream().map(this::mapearFacility).collect(Collectors.toList());
    }

    private List<GateDTO> listarGates(Long facilityId) {
        return jdbcTemplate.queryForList("""
                SELECT id, facility_id, codigo, nome, ativo
                  FROM gate_configuracao
                 WHERE facility_id = :facilityId
                 ORDER BY ativo DESC, nome
                """, new MapSqlParameterSource("facilityId", facilityId)).stream()
                .map(this::mapearGate).collect(Collectors.toList());
    }

    private List<LaneDTO> listarLanes(Long facilityId) {
        return jdbcTemplate.queryForList("""
                SELECT gl.id, gl.gate_id, gl.codigo, gl.nome, gl.direcao, gl.status,
                       gl.capacidade_fila, gl.ocr_habilitado, gl.balanca_habilitada,
                       gl.inspecao_habilitada,
                       COUNT(tv.id) FILTER (WHERE tv.status IN ('CHECKIN', 'EM_PROCESSAMENTO', 'TROUBLE')) AS fila_atual
                  FROM gate_lane gl
                  JOIN gate_configuracao gc ON gc.id = gl.gate_id
                  LEFT JOIN truck_visit tv ON tv.lane_id = gl.id
                 WHERE gc.facility_id = :facilityId
                 GROUP BY gl.id
                 ORDER BY gl.gate_id, gl.codigo
                """, new MapSqlParameterSource("facilityId", facilityId)).stream()
                .map(this::mapearLane).collect(Collectors.toList());
    }

    private List<StageDTO> listarStages(Long facilityId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT sc.id, sc.gate_id, sc.codigo, sc.nome, sc.ordem, sc.ativo,
                       sc.permite_trouble, sc.finaliza_visita
                  FROM gate_stage_config sc
                  JOIN gate_configuracao gc ON gc.id = sc.gate_id
                 WHERE gc.facility_id = :facilityId
                 ORDER BY sc.gate_id, sc.ordem
                """, new MapSqlParameterSource("facilityId", facilityId));
        return rows.stream().map(row -> mapearStage(row, listarTasks(longValue(row, "id"))))
                .collect(Collectors.toList());
    }

    private List<BusinessTaskDTO> listarTasks(Long stageId) {
        return jdbcTemplate.queryForList("""
                SELECT id, stage_id, codigo, nome, tipo, ordem, obrigatoria, ativa, configuracao
                  FROM gate_business_task
                 WHERE stage_id = :stageId
                 ORDER BY ordem, codigo
                """, new MapSqlParameterSource("stageId", stageId)).stream()
                .map(this::mapearTask).collect(Collectors.toList());
    }

    private List<TruckVisitDTO> listarVisitasAtivas(Long facilityId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT tv.id, tv.codigo, tv.agendamento_id, tv.facility_id, tv.gate_id, tv.lane_id,
                       gl.codigo AS lane_codigo, tv.transportadora_id, tr.nome AS transportadora,
                       tv.motorista_id, mo.nome AS motorista, tv.veiculo_id, ve.placa,
                       tv.stage_atual_id, sc.codigo AS stage_codigo, sc.nome AS stage_nome,
                       tv.status, tv.checkin_em, tv.checkout_em, tv.iniciado_em, tv.finalizado_em
                  FROM truck_visit tv
                  JOIN transportadora tr ON tr.id = tv.transportadora_id
                  JOIN motorista mo ON mo.id = tv.motorista_id
                  JOIN veiculo ve ON ve.id = tv.veiculo_id
                  LEFT JOIN gate_lane gl ON gl.id = tv.lane_id
                  LEFT JOIN gate_stage_config sc ON sc.id = tv.stage_atual_id
                 WHERE tv.facility_id = :facilityId
                   AND tv.status IN ('PREVISTA', 'CHECKIN', 'EM_PROCESSAMENTO', 'TROUBLE')
                 ORDER BY CASE tv.status WHEN 'TROUBLE' THEN 0 ELSE 1 END, tv.checkin_em, tv.id
                 LIMIT 200
                """, new MapSqlParameterSource("facilityId", facilityId));
        return rows.stream().map(row -> mapearVisita(row, listarTransacoes(longValue(row, "id"))))
                .collect(Collectors.toList());
    }

    private List<TransactionDTO> listarTransacoes(Long visitaId) {
        return jdbcTemplate.queryForList("""
                SELECT id, truck_visit_id, sequencia, tipo_operacao, status, unidade_referencia,
                       booking_id, order_id, preadvice_id, stage_atual_id, trouble_ativo
                  FROM gate_transaction
                 WHERE truck_visit_id = :visitaId
                 ORDER BY sequencia
                """, new MapSqlParameterSource("visitaId", visitaId)).stream()
                .map(this::mapearTransaction).collect(Collectors.toList());
    }

    private List<TroubleDTO> listarTroublesAbertos(Long facilityId) {
        return jdbcTemplate.queryForList("""
                SELECT tc.id, tc.transaction_id, gt.truck_visit_id, tv.codigo AS truck_visit_codigo,
                       tc.codigo, tc.descricao, tc.severidade, tc.status, tc.aberto_por,
                       tc.aberto_em, tc.resolvido_por, tc.resolvido_em, tc.resolucao
                  FROM gate_trouble_case tc
                  JOIN gate_transaction gt ON gt.id = tc.transaction_id
                  JOIN truck_visit tv ON tv.id = gt.truck_visit_id
                 WHERE tv.facility_id = :facilityId AND tc.status = 'ABERTO'
                 ORDER BY CASE tc.severidade
                     WHEN 'CRITICA' THEN 0 WHEN 'ALTA' THEN 1 WHEN 'MEDIA' THEN 2 ELSE 3 END,
                     tc.aberto_em
                """, new MapSqlParameterSource("facilityId", facilityId)).stream()
                .map(this::mapearTrouble).collect(Collectors.toList());
    }

    private CapacityDTO consultarCapacidade(Long facilityId) {
        Map<String, Object> row = jdbcTemplate.queryForMap("""
                SELECT COUNT(*) AS janelas,
                       COALESCE(SUM(ja.capacidade), 0) AS capacidade,
                       COALESCE(SUM(ja.capacidade_utilizada), 0) AS utilizada
                  FROM janela_atendimento ja
                  LEFT JOIN gate_configuracao gc ON gc.id = ja.gate_id
                 WHERE ja.data >= CURRENT_DATE
                   AND (ja.gate_id IS NULL OR gc.facility_id = :facilityId)
                """, new MapSqlParameterSource("facilityId", facilityId));
        long capacidade = longValue(row, "capacidade");
        long utilizada = longValue(row, "utilizada");
        long disponivel = Math.max(0, capacidade - utilizada);
        BigDecimal ocupacao = capacidade == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(utilizada)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(capacidade), 2, RoundingMode.HALF_UP);
        return new CapacityDTO(longValue(row, "janelas"), capacidade, utilizada, disponivel, ocupacao);
    }

    private void validarEstruturaVisita(TruckVisitRequest request) {
        Long gateCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM gate_configuracao
                 WHERE id = :gateId AND facility_id = :facilityId AND ativo = TRUE
                """, new MapSqlParameterSource()
                .addValue("gateId", request.gateId())
                .addValue("facilityId", request.facilityId()), Long.class);
        if (gateCount == null || gateCount == 0) {
            throw new BusinessException("O Gate informado não pertence à instalação ou está inativo.");
        }
        if (request.laneId() != null) {
            validarLaneParaGate(request.laneId(), request.gateId());
        }
        Long pessoasVeiculo = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                  FROM motorista m
                  JOIN veiculo v ON v.id = :veiculoId
                 WHERE m.id = :motoristaId
                   AND m.transportadora_id = :transportadoraId
                   AND v.transportadora_id = :transportadoraId
                """, new MapSqlParameterSource()
                .addValue("veiculoId", request.veiculoId())
                .addValue("motoristaId", request.motoristaId())
                .addValue("transportadoraId", request.transportadoraId()), Long.class);
        if (pessoasVeiculo == null || pessoasVeiculo == 0) {
            throw new BusinessException("Motorista, veículo e transportadora não possuem vínculo operacional válido.");
        }
        if (request.agendamentoId() != null) {
            Long agendamentoCount = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*) FROM agendamento
                     WHERE id = :id
                       AND transportadora_id = :transportadoraId
                       AND veiculo_id = :veiculoId
                       AND status NOT IN ('CANCELADO', 'NO_SHOW', 'COMPLETO')
                    """, new MapSqlParameterSource()
                    .addValue("id", request.agendamentoId())
                    .addValue("transportadoraId", request.transportadoraId())
                    .addValue("veiculoId", request.veiculoId()), Long.class);
            if (agendamentoCount == null || agendamentoCount == 0) {
                throw new BusinessException("O agendamento não está disponível para esta transportadora e veículo.");
            }
        }
    }

    private void validarReferenciasTransacao(TransactionRequest request, Long transportadoraId) {
        if (request.bookingId() == null && request.orderId() == null && request.preadviceId() == null) {
            throw new BusinessException("Cada transação deve informar booking, ordem ou pré-aviso.");
        }
        if (request.bookingId() != null) {
            Long count = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*) FROM gate_booking
                     WHERE id = :id
                       AND status IN ('ABERTO', 'PARCIAL')
                       AND (transportadora_id IS NULL OR transportadora_id = :transportadoraId)
                       AND (validade_inicio IS NULL OR validade_inicio <= NOW())
                       AND (validade_fim IS NULL OR validade_fim >= NOW())
                       AND quantidade_utilizada < quantidade_total
                    """, new MapSqlParameterSource()
                    .addValue("id", request.bookingId())
                    .addValue("transportadoraId", transportadoraId), Long.class);
            if (count == null || count == 0) {
                throw new BusinessException("Booking indisponível, expirado ou sem saldo.");
            }
        }
        if (request.orderId() != null) {
            Long count = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*) FROM gate_order
                     WHERE id = :id
                       AND status = 'ATIVA'
                       AND (transportadora_id IS NULL OR transportadora_id = :transportadoraId)
                       AND (validade_inicio IS NULL OR validade_inicio <= NOW())
                       AND (validade_fim IS NULL OR validade_fim >= NOW())
                    """, new MapSqlParameterSource()
                    .addValue("id", request.orderId())
                    .addValue("transportadoraId", transportadoraId), Long.class);
            if (count == null || count == 0) {
                throw new BusinessException("Ordem EDO, ERO ou IDO indisponível ou expirada.");
            }
        }
        if (request.preadviceId() != null) {
            Long count = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*) FROM gate_preadvice
                     WHERE id = :id AND status = 'ATIVO'
                    """, new MapSqlParameterSource("id", request.preadviceId()), Long.class);
            if (count == null || count == 0) {
                throw new BusinessException("Pré-aviso indisponível.");
            }
        }
    }

    private void validarLaneParaGate(Long laneId, Long gateId) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM gate_lane
                 WHERE id = :laneId AND gate_id = :gateId AND status = 'ABERTA'
                """, new MapSqlParameterSource()
                .addValue("laneId", laneId)
                .addValue("gateId", gateId), Long.class);
        if (count == null || count == 0) {
            throw new BusinessException("A pista não pertence ao Gate ou não está aberta.");
        }
    }

    private void validarTarefasConcluidas(Long stageId, Set<String> tarefasConcluidas) {
        Set<String> obrigatorias = new HashSet<>(jdbcTemplate.queryForList("""
                SELECT codigo FROM gate_business_task
                 WHERE stage_id = :stageId AND ativa = TRUE AND obrigatoria = TRUE
                """, new MapSqlParameterSource("stageId", stageId), String.class));
        Set<String> concluidas = tarefasConcluidas == null
                ? Set.of()
                : tarefasConcluidas.stream().filter(StringUtils::hasText)
                .map(this::normalizarCodigo).collect(Collectors.toSet());
        obrigatorias.removeAll(concluidas);
        if (!obrigatorias.isEmpty()) {
            throw new BusinessException("Business tasks obrigatórias não concluídas: " + String.join(", ", obrigatorias));
        }
    }

    private Long resolverStageDestino(Long stageOrigemId, String codigoDestino) {
        StringBuilder sql = new StringBuilder("""
                SELECT destino.id
                  FROM gate_stage_transition transicao
                  JOIN gate_stage_config destino ON destino.id = transicao.destino_stage_id
                 WHERE transicao.origem_stage_id = :origemId
                   AND transicao.ativo = TRUE
                   AND destino.ativo = TRUE
                """);
        MapSqlParameterSource parameters = new MapSqlParameterSource("origemId", stageOrigemId);
        if (StringUtils.hasText(codigoDestino)) {
            sql.append(" AND destino.codigo = :codigoDestino");
            parameters.addValue("codigoDestino", normalizarCodigo(codigoDestino));
        }
        sql.append(" ORDER BY destino.ordem LIMIT 1");
        return consultarLong(sql.toString(), parameters, "Não existe transição válida para o próximo estágio.");
    }

    private void consumirReferencias(Long visitaId) {
        jdbcTemplate.update("""
                UPDATE gate_booking booking
                   SET quantidade_utilizada = LEAST(booking.quantidade_total, booking.quantidade_utilizada + consumo.quantidade),
                       status = CASE
                           WHEN LEAST(booking.quantidade_total, booking.quantidade_utilizada + consumo.quantidade) >= booking.quantidade_total
                               THEN 'UTILIZADO'
                           ELSE 'PARCIAL'
                       END,
                       updated_at = NOW()
                  FROM (
                      SELECT booking_id, COUNT(*) AS quantidade
                        FROM gate_transaction
                       WHERE truck_visit_id = :visitaId AND booking_id IS NOT NULL
                       GROUP BY booking_id
                  ) consumo
                 WHERE booking.id = consumo.booking_id
                """, new MapSqlParameterSource("visitaId", visitaId));
        jdbcTemplate.update("""
                UPDATE gate_order
                   SET status = 'UTILIZADA', updated_at = NOW()
                 WHERE id IN (
                     SELECT order_id FROM gate_transaction
                      WHERE truck_visit_id = :visitaId AND order_id IS NOT NULL
                 )
                """, new MapSqlParameterSource("visitaId", visitaId));
        jdbcTemplate.update("""
                UPDATE gate_preadvice
                   SET status = 'UTILIZADO', updated_at = NOW()
                 WHERE id IN (
                     SELECT preadvice_id FROM gate_transaction
                      WHERE truck_visit_id = :visitaId AND preadvice_id IS NOT NULL
                 )
                """, new MapSqlParameterSource("visitaId", visitaId));
    }

    private void registrarStageEvent(Long visitaId, Long transacaoId, Long origemId, Long destinoId,
                                     Long laneId, String usuario, String correlationId,
                                     Set<String> tarefasConcluidas) {
        jdbcTemplate.update("""
                INSERT INTO gate_stage_event (
                    truck_visit_id, transaction_id, stage_origem_id, stage_destino_id, lane_id,
                    usuario, correlation_id, tarefas_concluidas
                ) VALUES (
                    :visitaId, :transacaoId, :origemId, :destinoId, :laneId,
                    :usuario, :correlationId, CAST(:tarefas AS JSONB)
                )
                """, new MapSqlParameterSource()
                .addValue("visitaId", visitaId)
                .addValue("transacaoId", transacaoId)
                .addValue("origemId", origemId)
                .addValue("destinoId", destinoId)
                .addValue("laneId", laneId)
                .addValue("usuario", normalizarTexto(usuario, "O usuário deve ser informado."))
                .addValue("correlationId", limpar(correlationId))
                .addValue("tarefas", escreverJson(tarefasConcluidas == null ? Set.of() : tarefasConcluidas)));
    }

    private FacilityDTO buscarFacility(Long id) {
        Map<String, Object> row = consultarUmaLinha("""
                SELECT id, codigo, nome, fuso_horario, ativo FROM gate_facility WHERE id = :id
                """, new MapSqlParameterSource("id", id), "Instalação de Gate não encontrada.");
        return mapearFacility(row);
    }

    private GateDTO buscarGate(Long id) {
        Map<String, Object> row = consultarUmaLinha("""
                SELECT id, facility_id, codigo, nome, ativo FROM gate_configuracao WHERE id = :id
                """, new MapSqlParameterSource("id", id), "Gate não encontrado.");
        return mapearGate(row);
    }

    private LaneDTO buscarLane(Long id) {
        Map<String, Object> row = consultarUmaLinha("""
                SELECT gl.id, gl.gate_id, gl.codigo, gl.nome, gl.direcao, gl.status,
                       gl.capacidade_fila, gl.ocr_habilitado, gl.balanca_habilitada,
                       gl.inspecao_habilitada, COUNT(tv.id) AS fila_atual
                  FROM gate_lane gl
                  LEFT JOIN truck_visit tv ON tv.lane_id = gl.id
                       AND tv.status IN ('CHECKIN', 'EM_PROCESSAMENTO', 'TROUBLE')
                 WHERE gl.id = :id
                 GROUP BY gl.id
                """, new MapSqlParameterSource("id", id), "Pista de Gate não encontrada.");
        return mapearLane(row);
    }

    private StageDTO buscarStage(Long id) {
        Map<String, Object> row = consultarUmaLinha("""
                SELECT id, gate_id, codigo, nome, ordem, ativo, permite_trouble, finaliza_visita
                  FROM gate_stage_config WHERE id = :id
                """, new MapSqlParameterSource("id", id), "Estágio de Gate não encontrado.");
        return mapearStage(row, listarTasks(id));
    }

    private BusinessTaskDTO buscarTask(Long id) {
        Map<String, Object> row = consultarUmaLinha("""
                SELECT id, stage_id, codigo, nome, tipo, ordem, obrigatoria, ativa, configuracao
                  FROM gate_business_task WHERE id = :id
                """, new MapSqlParameterSource("id", id), "Business task não encontrada.");
        return mapearTask(row);
    }

    private BookingDTO buscarBooking(Long id) {
        Map<String, Object> row = consultarUmaLinha("""
                SELECT id, codigo, transportadora_id, armador, viagem, quantidade_total,
                       quantidade_utilizada, status, validade_inicio, validade_fim, observacoes
                  FROM gate_booking WHERE id = :id
                """, new MapSqlParameterSource("id", id), "Booking não encontrado.");
        return mapearBooking(row);
    }

    private OrderDTO buscarOrder(Long id) {
        Map<String, Object> row = consultarUmaLinha("""
                SELECT id, tipo, codigo, booking_id, transportadora_id, unidade_referencia,
                       status, validade_inicio, validade_fim
                  FROM gate_order WHERE id = :id
                """, new MapSqlParameterSource("id", id), "Ordem de Gate não encontrada.");
        return mapearOrder(row);
    }

    private PreadviceDTO buscarPreadvice(Long id) {
        Map<String, Object> row = consultarUmaLinha("""
                SELECT id, tipo, codigo, booking_id, order_id, unidade_referencia,
                       iso_type, peso_bruto_kg, status
                  FROM gate_preadvice WHERE id = :id
                """, new MapSqlParameterSource("id", id), "Pré-aviso não encontrado.");
        return mapearPreadvice(row);
    }

    private TroubleDTO buscarTrouble(Long id) {
        Map<String, Object> row = consultarUmaLinha("""
                SELECT tc.id, tc.transaction_id, gt.truck_visit_id, tv.codigo AS truck_visit_codigo,
                       tc.codigo, tc.descricao, tc.severidade, tc.status, tc.aberto_por,
                       tc.aberto_em, tc.resolvido_por, tc.resolvido_em, tc.resolucao
                  FROM gate_trouble_case tc
                  JOIN gate_transaction gt ON gt.id = tc.transaction_id
                  JOIN truck_visit tv ON tv.id = gt.truck_visit_id
                 WHERE tc.id = :id
                """, new MapSqlParameterSource("id", id), "Trouble transaction não encontrada.");
        return mapearTrouble(row);
    }

    private InspectionDTO buscarInspecao(Long id) {
        Map<String, Object> row = consultarUmaLinha("""
                SELECT id, transaction_id, tipo, resultado, inspetor, observacoes, executado_em
                  FROM gate_inspection WHERE id = :id
                """, new MapSqlParameterSource("id", id), "Inspeção não encontrada.");
        return new InspectionDTO(
                longValue(row, "id"),
                longValue(row, "transaction_id"),
                texto(row, "tipo"),
                texto(row, "resultado"),
                texto(row, "inspetor"),
                texto(row, "observacoes"),
                localDateTime(row.get("executado_em")));
    }

    private AttachmentDTO buscarAttachment(Long id) {
        Map<String, Object> row = consultarUmaLinha("""
                SELECT id, truck_visit_id, transaction_id, categoria, nome_arquivo, content_type,
                       url_documento, metadados, criado_por, created_at
                  FROM gate_attachment WHERE id = :id
                """, new MapSqlParameterSource("id", id), "Anexo não encontrado.");
        return new AttachmentDTO(
                longValue(row, "id"),
                nullableLong(row, "truck_visit_id"),
                nullableLong(row, "transaction_id"),
                texto(row, "categoria"),
                texto(row, "nome_arquivo"),
                texto(row, "content_type"),
                texto(row, "url_documento"),
                lerMapaJson(row.get("metadados")),
                texto(row, "criado_por"),
                localDateTime(row.get("created_at")));
    }

    private DocumentDTO buscarDocumento(Long id) {
        Map<String, Object> row = consultarUmaLinha("""
                SELECT id, truck_visit_id, transaction_id, tipo, numero, status, conteudo,
                       emitido_por, emitido_em, reimpressoes, ultima_reimpressao_em
                  FROM gate_document WHERE id = :id
                """, new MapSqlParameterSource("id", id), "Documento de Gate não encontrado.");
        return new DocumentDTO(
                longValue(row, "id"),
                longValue(row, "truck_visit_id"),
                nullableLong(row, "transaction_id"),
                texto(row, "tipo"),
                texto(row, "numero"),
                texto(row, "status"),
                lerMapaJson(row.get("conteudo")),
                texto(row, "emitido_por"),
                localDateTime(row.get("emitido_em")),
                intValue(row, "reimpressoes"),
                localDateTime(row.get("ultima_reimpressao_em")));
    }

    private TransferDTO buscarTransferencia(Long id) {
        Map<String, Object> row = consultarUmaLinha("""
                SELECT id, codigo, truck_visit_id, facility_origem_id, facility_destino_id,
                       status, solicitado_por, solicitado_em, recebido_em, observacoes
                  FROM gate_facility_transfer WHERE id = :id
                """, new MapSqlParameterSource("id", id), "Transferência não encontrada.");
        return new TransferDTO(
                longValue(row, "id"),
                texto(row, "codigo"),
                longValue(row, "truck_visit_id"),
                longValue(row, "facility_origem_id"),
                longValue(row, "facility_destino_id"),
                texto(row, "status"),
                texto(row, "solicitado_por"),
                localDateTime(row.get("solicitado_em")),
                localDateTime(row.get("recebido_em")),
                texto(row, "observacoes"));
    }

    private FacilityDTO mapearFacility(Map<String, Object> row) {
        return new FacilityDTO(longValue(row, "id"), texto(row, "codigo"), texto(row, "nome"),
                texto(row, "fuso_horario"), boolValue(row, "ativo"));
    }

    private GateDTO mapearGate(Map<String, Object> row) {
        return new GateDTO(longValue(row, "id"), longValue(row, "facility_id"), texto(row, "codigo"),
                texto(row, "nome"), boolValue(row, "ativo"));
    }

    private LaneDTO mapearLane(Map<String, Object> row) {
        return new LaneDTO(
                longValue(row, "id"), longValue(row, "gate_id"), texto(row, "codigo"), texto(row, "nome"),
                texto(row, "direcao"), texto(row, "status"), intValue(row, "capacidade_fila"),
                intValue(row, "fila_atual"), boolValue(row, "ocr_habilitado"),
                boolValue(row, "balanca_habilitada"), boolValue(row, "inspecao_habilitada"));
    }

    private StageDTO mapearStage(Map<String, Object> row, List<BusinessTaskDTO> tarefas) {
        return new StageDTO(
                longValue(row, "id"), longValue(row, "gate_id"), texto(row, "codigo"), texto(row, "nome"),
                intValue(row, "ordem"), boolValue(row, "ativo"), boolValue(row, "permite_trouble"),
                boolValue(row, "finaliza_visita"), tarefas);
    }

    private BusinessTaskDTO mapearTask(Map<String, Object> row) {
        return new BusinessTaskDTO(
                longValue(row, "id"), longValue(row, "stage_id"), texto(row, "codigo"), texto(row, "nome"),
                texto(row, "tipo"), intValue(row, "ordem"), boolValue(row, "obrigatoria"),
                boolValue(row, "ativa"), lerMapaJson(row.get("configuracao")));
    }

    private BookingDTO mapearBooking(Map<String, Object> row) {
        return new BookingDTO(
                longValue(row, "id"), texto(row, "codigo"), nullableLong(row, "transportadora_id"),
                texto(row, "armador"), texto(row, "viagem"), intValue(row, "quantidade_total"),
                intValue(row, "quantidade_utilizada"), texto(row, "status"),
                localDateTime(row.get("validade_inicio")), localDateTime(row.get("validade_fim")),
                texto(row, "observacoes"));
    }

    private OrderDTO mapearOrder(Map<String, Object> row) {
        return new OrderDTO(
                longValue(row, "id"), texto(row, "tipo"), texto(row, "codigo"), nullableLong(row, "booking_id"),
                nullableLong(row, "transportadora_id"), texto(row, "unidade_referencia"), texto(row, "status"),
                localDateTime(row.get("validade_inicio")), localDateTime(row.get("validade_fim")));
    }

    private PreadviceDTO mapearPreadvice(Map<String, Object> row) {
        return new PreadviceDTO(
                longValue(row, "id"), texto(row, "tipo"), texto(row, "codigo"), nullableLong(row, "booking_id"),
                nullableLong(row, "order_id"), texto(row, "unidade_referencia"), texto(row, "iso_type"),
                decimalValue(row, "peso_bruto_kg"), texto(row, "status"));
    }

    private TruckVisitDTO mapearVisita(Map<String, Object> row, List<TransactionDTO> transacoes) {
        return new TruckVisitDTO(
                longValue(row, "id"), texto(row, "codigo"), nullableLong(row, "agendamento_id"),
                longValue(row, "facility_id"), longValue(row, "gate_id"), nullableLong(row, "lane_id"),
                texto(row, "lane_codigo"), longValue(row, "transportadora_id"), texto(row, "transportadora"),
                longValue(row, "motorista_id"), texto(row, "motorista"), longValue(row, "veiculo_id"),
                texto(row, "placa"), nullableLong(row, "stage_atual_id"), texto(row, "stage_codigo"),
                texto(row, "stage_nome"), texto(row, "status"), localDateTime(row.get("checkin_em")),
                localDateTime(row.get("checkout_em")), localDateTime(row.get("iniciado_em")),
                localDateTime(row.get("finalizado_em")), transacoes);
    }

    private TransactionDTO mapearTransaction(Map<String, Object> row) {
        return new TransactionDTO(
                longValue(row, "id"), longValue(row, "truck_visit_id"), intValue(row, "sequencia"),
                texto(row, "tipo_operacao"), texto(row, "status"), texto(row, "unidade_referencia"),
                nullableLong(row, "booking_id"), nullableLong(row, "order_id"), nullableLong(row, "preadvice_id"),
                nullableLong(row, "stage_atual_id"), boolValue(row, "trouble_ativo"));
    }

    private TroubleDTO mapearTrouble(Map<String, Object> row) {
        return new TroubleDTO(
                longValue(row, "id"), longValue(row, "transaction_id"), longValue(row, "truck_visit_id"),
                texto(row, "truck_visit_codigo"), texto(row, "codigo"), texto(row, "descricao"),
                texto(row, "severidade"), texto(row, "status"), texto(row, "aberto_por"),
                localDateTime(row.get("aberto_em")), texto(row, "resolvido_por"),
                localDateTime(row.get("resolvido_em")), texto(row, "resolucao"));
    }

    private Map<String, Object> consultarUmaLinha(String sql, MapSqlParameterSource parameters, String mensagem) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, parameters);
        if (rows.isEmpty()) {
            throw new NotFoundException(mensagem);
        }
        return rows.get(0);
    }

    private Long consultarLong(String sql, MapSqlParameterSource parameters, String mensagem) {
        List<Long> rows = jdbcTemplate.queryForList(sql, parameters, Long.class);
        if (rows.isEmpty() || rows.get(0) == null) {
            throw new NotFoundException(mensagem);
        }
        return rows.get(0);
    }

    private void garantirExiste(String tabela, Long id, String mensagem) {
        if (!Set.of("transportadora", "truck_visit", "gate_transaction").contains(tabela)) {
            throw new IllegalArgumentException("Tabela não autorizada para validação.");
        }
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tabela + " WHERE id = :id",
                new MapSqlParameterSource("id", id), Long.class);
        if (count == null || count == 0) {
            throw new NotFoundException(mensagem);
        }
    }

    private void validarVigencia(LocalDateTime inicio, LocalDateTime fim) {
        if (inicio != null && fim != null && fim.isBefore(inicio)) {
            throw new BusinessException("A validade final não pode ser anterior à validade inicial.");
        }
    }

    private String normalizarCodigo(String valor) {
        return normalizarTexto(valor, "O código deve ser informado.")
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9._/-]", "_");
    }

    private String normalizarUnidade(String valor) {
        if (!StringUtils.hasText(valor)) {
            return null;
        }
        return valor.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9-]", "");
    }

    private String normalizarEnum(String valor, Set<String> permitidos, String mensagem) {
        String normalizado = normalizarCodigo(valor);
        if (!permitidos.contains(normalizado)) {
            throw new BusinessException(mensagem);
        }
        return normalizado;
    }

    private String normalizarTexto(String valor, String mensagem) {
        if (!StringUtils.hasText(valor)) {
            throw new BusinessException(mensagem);
        }
        return valor.trim();
    }

    private String limpar(String valor) {
        return StringUtils.hasText(valor) ? valor.trim() : null;
    }

    private String escreverJson(Object valor) {
        try {
            return objectMapper.writeValueAsString(valor == null ? Map.of() : valor);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("Não foi possível serializar os dados operacionais.");
        }
    }

    private Map<String, Object> lerMapaJson(Object valor) {
        if (valor == null) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(valor.toString(), new TypeReference<Map<String, Object>>() { });
        } catch (JsonProcessingException exception) {
            return Map.of("valor", valor.toString());
        }
    }

    private long longValue(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return 0L;
        }
        return Long.parseLong(value.toString());
    }

    private Long nullableLong(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(value.toString());
    }

    private int intValue(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return 0;
        }
        return Integer.parseInt(value.toString());
    }

    private boolean boolValue(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value instanceof Boolean bool) {
            return bool;
        }
        return value != null && Boolean.parseBoolean(value.toString());
    }

    private BigDecimal decimalValue(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return value == null ? null : new BigDecimal(value.toString());
    }

    private String texto(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value == null ? null : value.toString();
    }

    private LocalDateTime localDateTime(Object value) {
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (value instanceof LocalDate localDate) {
            return localDate.atStartOfDay();
        }
        return value == null ? null : LocalDateTime.parse(value.toString());
    }
}