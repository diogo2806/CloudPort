package br.com.cloudport.servicogate.app.billing;

import br.com.cloudport.servicogate.app.billing.dto.BillingCapDtos.CapAgendamentoDTO;
import br.com.cloudport.servicogate.app.billing.dto.BillingCapDtos.CapResumoDTO;
import br.com.cloudport.servicogate.app.billing.dto.BillingCapDtos.CobrancaDTO;
import br.com.cloudport.servicogate.app.billing.dto.BillingCapDtos.FaturaDTO;
import br.com.cloudport.servicogate.app.billing.dto.BillingCapDtos.FaturaGeracaoRequest;
import br.com.cloudport.servicogate.app.billing.dto.BillingCapDtos.FaturaItemDTO;
import br.com.cloudport.servicogate.app.billing.dto.BillingCapDtos.PagamentoDTO;
import br.com.cloudport.servicogate.app.billing.dto.BillingCapDtos.PagamentoRequest;
import br.com.cloudport.servicogate.app.billing.dto.BillingCapDtos.TarifaDTO;
import br.com.cloudport.servicogate.app.billing.dto.BillingCapDtos.TarifaRequest;
import br.com.cloudport.servicogate.app.configuracoes.TransportadoraRepository;
import br.com.cloudport.servicogate.exception.BusinessException;
import br.com.cloudport.servicogate.exception.NotFoundException;
import br.com.cloudport.servicogate.model.Transportadora;
import br.com.cloudport.servicogate.model.enums.TipoOperacao;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class BillingCapService {

    private static final Set<String> STATUS_COBRANCA = Set.of("PENDENTE", "FATURADA", "CANCELADA");
    private static final Set<String> STATUS_FATURA = Set.of("ABERTA", "PAGA", "CANCELADA");

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final TransportadoraRepository transportadoraRepository;

    public BillingCapService(NamedParameterJdbcTemplate jdbcTemplate,
                             TransportadoraRepository transportadoraRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.transportadoraRepository = transportadoraRepository;
    }

    @Transactional(readOnly = true)
    public List<TarifaDTO> listarTarifas(Boolean ativas) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, codigo, descricao, tipo_operacao, valor, inicio_vigencia, fim_vigencia, ativa
                  FROM billing_tarifa
                 WHERE 1 = 1
                """);
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        if (ativas != null) {
            sql.append(" AND ativa = :ativas");
            parameters.addValue("ativas", ativas);
        }
        sql.append(" ORDER BY ativa DESC, tipo_operacao, inicio_vigencia DESC, codigo");
        return jdbcTemplate.queryForList(sql.toString(), parameters).stream()
                .map(this::mapearTarifa)
                .collect(Collectors.toList());
    }

    @Transactional
    public TarifaDTO salvarTarifa(TarifaRequest request) {
        validarVigencia(request.inicioVigencia(), request.fimVigencia());
        String codigo = normalizarCodigo(request.codigo());
        String descricao = normalizarTextoObrigatorio(request.descricao(), "A descrição da tarifa deve ser informada.");
        boolean ativa = request.ativa() == null || request.ativa();
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("codigo", codigo)
                .addValue("descricao", descricao)
                .addValue("tipoOperacao", request.tipoOperacao().name())
                .addValue("valor", request.valor())
                .addValue("inicioVigencia", request.inicioVigencia())
                .addValue("fimVigencia", request.fimVigencia())
                .addValue("ativa", ativa);
        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO billing_tarifa (
                    codigo, descricao, tipo_operacao, valor, inicio_vigencia, fim_vigencia, ativa
                ) VALUES (
                    :codigo, :descricao, :tipoOperacao, :valor, :inicioVigencia, :fimVigencia, :ativa
                )
                ON CONFLICT (codigo) DO UPDATE SET
                    descricao = EXCLUDED.descricao,
                    tipo_operacao = EXCLUDED.tipo_operacao,
                    valor = EXCLUDED.valor,
                    inicio_vigencia = EXCLUDED.inicio_vigencia,
                    fim_vigencia = EXCLUDED.fim_vigencia,
                    ativa = EXCLUDED.ativa,
                    updated_at = NOW()
                RETURNING id
                """, parameters, Long.class);
        return buscarTarifa(id);
    }

    @Transactional
    public CobrancaDTO gerarCobrancaAgendamento(Long agendamentoId) {
        Map<String, Object> agendamento = consultarUmaLinha("""
                SELECT a.id,
                       a.codigo,
                       a.tipo_operacao,
                       a.status,
                       a.transportadora_id,
                       t.nome AS transportadora,
                       COALESCE(a.horario_real_saida, a.horario_real_chegada, a.horario_previsto_chegada, NOW()) AS ocorrido_em
                  FROM agendamento a
                  JOIN transportadora t ON t.id = a.transportadora_id
                 WHERE a.id = :id
                """, new MapSqlParameterSource("id", agendamentoId), "Agendamento não encontrado.");
        String status = texto(agendamento, "status");
        if (!"CONCLUIDO".equals(status) && !"COMPLETO".equals(status)) {
            throw new BusinessException("A cobrança somente pode ser gerada após a conclusão do atendimento.");
        }
        String tipoOperacao = texto(agendamento, "tipo_operacao");
        Map<String, Object> tarifa = consultarUmaLinha("""
                SELECT id, codigo, descricao, valor
                  FROM billing_tarifa
                 WHERE ativa = TRUE
                   AND tipo_operacao = :tipoOperacao
                   AND inicio_vigencia <= CURRENT_DATE
                   AND (fim_vigencia IS NULL OR fim_vigencia >= CURRENT_DATE)
                 ORDER BY inicio_vigencia DESC, id DESC
                 LIMIT 1
                """, new MapSqlParameterSource("tipoOperacao", tipoOperacao),
                "Não existe tarifa ativa para a operação " + tipoOperacao + ".");

        String referencia = "AGENDAMENTO:" + agendamentoId + ":" + longValue(tarifa, "id");
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("referencia", referencia)
                .addValue("transportadoraId", longValue(agendamento, "transportadora_id"))
                .addValue("agendamentoId", agendamentoId)
                .addValue("tarifaId", longValue(tarifa, "id"))
                .addValue("descricao", texto(tarifa, "descricao") + " - " + texto(agendamento, "codigo"))
                .addValue("valor", decimalValue(tarifa, "valor"))
                .addValue("ocorridoEm", localDateTime(agendamento.get("ocorrido_em")));
        Long cobrancaId = jdbcTemplate.queryForObject("""
                INSERT INTO billing_cobranca (
                    referencia, transportadora_id, agendamento_id, tarifa_id, descricao, valor, status, ocorrido_em
                ) VALUES (
                    :referencia, :transportadoraId, :agendamentoId, :tarifaId, :descricao, :valor, 'PENDENTE', :ocorridoEm
                )
                ON CONFLICT (referencia) DO UPDATE SET updated_at = billing_cobranca.updated_at
                RETURNING id
                """, parameters, Long.class);
        return buscarCobranca(cobrancaId, Optional.empty());
    }

    @Transactional(readOnly = true)
    public List<CobrancaDTO> listarCobrancas(Long transportadoraId, String status) {
        Optional<Long> transportadoraLogada = resolverTransportadoraLogada();
        Long filtroTransportadora = transportadoraLogada.orElse(transportadoraId);
        String statusNormalizado = normalizarStatus(status, STATUS_COBRANCA);
        StringBuilder sql = new StringBuilder("""
                SELECT c.id,
                       c.referencia,
                       c.transportadora_id,
                       t.nome AS transportadora,
                       c.agendamento_id,
                       a.codigo AS agendamento,
                       c.descricao,
                       c.valor,
                       c.status,
                       c.ocorrido_em,
                       c.faturado_em
                  FROM billing_cobranca c
                  JOIN transportadora t ON t.id = c.transportadora_id
                  LEFT JOIN agendamento a ON a.id = c.agendamento_id
                 WHERE 1 = 1
                """);
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        if (filtroTransportadora != null) {
            sql.append(" AND c.transportadora_id = :transportadoraId");
            parameters.addValue("transportadoraId", filtroTransportadora);
        }
        if (statusNormalizado != null) {
            sql.append(" AND c.status = :status");
            parameters.addValue("status", statusNormalizado);
        }
        sql.append(" ORDER BY c.ocorrido_em DESC, c.id DESC");
        return jdbcTemplate.queryForList(sql.toString(), parameters).stream()
                .map(this::mapearCobranca)
                .collect(Collectors.toList());
    }

    @Transactional
    public FaturaDTO gerarFatura(FaturaGeracaoRequest request) {
        if (request.vencimento().isBefore(LocalDate.now())) {
            throw new BusinessException("O vencimento da fatura não pode estar no passado.");
        }
        Transportadora transportadora = transportadoraRepository.findById(request.transportadoraId())
                .orElseThrow(() -> new NotFoundException("Transportadora não encontrada"));
        List<Long> cobrancaIdsSolicitadas = request.cobrancaIds() == null
                ? Collections.emptyList()
                : request.cobrancaIds().stream().filter(id -> id != null && id > 0).distinct().collect(Collectors.toList());

        StringBuilder sql = new StringBuilder("""
                SELECT id, descricao, valor
                  FROM billing_cobranca
                 WHERE transportadora_id = :transportadoraId
                   AND status = 'PENDENTE'
                """);
        MapSqlParameterSource parameters = new MapSqlParameterSource("transportadoraId", transportadora.getId());
        if (!cobrancaIdsSolicitadas.isEmpty()) {
            sql.append(" AND id IN (:cobrancaIds)");
            parameters.addValue("cobrancaIds", cobrancaIdsSolicitadas);
        }
        sql.append(" ORDER BY ocorrido_em, id");
        List<Map<String, Object>> cobrancas = jdbcTemplate.queryForList(sql.toString(), parameters);
        if (cobrancas.isEmpty()) {
            throw new BusinessException("Não existem cobranças pendentes para gerar a fatura.");
        }
        if (!cobrancaIdsSolicitadas.isEmpty() && cobrancas.size() != cobrancaIdsSolicitadas.size()) {
            throw new BusinessException("Uma ou mais cobranças não pertencem à transportadora ou já foram faturadas.");
        }

        BigDecimal total = cobrancas.stream()
                .map(row -> decimalValue(row, "valor"))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        MapSqlParameterSource invoiceParameters = new MapSqlParameterSource()
                .addValue("transportadoraId", transportadora.getId())
                .addValue("vencimento", request.vencimento())
                .addValue("total", total);
        Long faturaId = jdbcTemplate.queryForObject("""
                INSERT INTO billing_fatura (
                    numero, transportadora_id, emitida_em, vencimento, status, subtotal, total
                ) VALUES (
                    'FAT-' || TO_CHAR(CURRENT_DATE, 'YYYYMM') || '-' ||
                    LPAD(NEXTVAL('billing_fatura_numero_seq')::TEXT, 6, '0'),
                    :transportadoraId, NOW(), :vencimento, 'ABERTA', :total, :total
                )
                RETURNING id
                """, invoiceParameters, Long.class);

        SqlParameterSource[] itemParameters = cobrancas.stream()
                .map(row -> new MapSqlParameterSource()
                        .addValue("faturaId", faturaId)
                        .addValue("cobrancaId", longValue(row, "id"))
                        .addValue("descricao", texto(row, "descricao"))
                        .addValue("valor", decimalValue(row, "valor")))
                .toArray(SqlParameterSource[]::new);
        jdbcTemplate.batchUpdate("""
                INSERT INTO billing_fatura_item (fatura_id, cobranca_id, descricao, valor)
                VALUES (:faturaId, :cobrancaId, :descricao, :valor)
                """, itemParameters);

        List<Long> cobrancaIds = cobrancas.stream().map(row -> longValue(row, "id")).collect(Collectors.toList());
        jdbcTemplate.update("""
                UPDATE billing_cobranca
                   SET status = 'FATURADA', faturado_em = NOW(), updated_at = NOW()
                 WHERE id IN (:cobrancaIds)
                """, new MapSqlParameterSource("cobrancaIds", cobrancaIds));
        return buscarFatura(faturaId, Optional.empty());
    }

    @Transactional(readOnly = true)
    public List<FaturaDTO> listarFaturas(Long transportadoraId, String status) {
        Optional<Long> transportadoraLogada = resolverTransportadoraLogada();
        Long filtroTransportadora = transportadoraLogada.orElse(transportadoraId);
        return listarFaturasInterno(filtroTransportadora, status, null);
    }

    @Transactional
    public FaturaDTO registrarPagamento(Long faturaId, PagamentoRequest request) {
        Map<String, Object> fatura = consultarUmaLinha("""
                SELECT id, status, total,
                       COALESCE((SELECT SUM(valor) FROM billing_pagamento WHERE fatura_id = f.id), 0) AS valor_pago
                  FROM billing_fatura f
                 WHERE id = :id
                """, new MapSqlParameterSource("id", faturaId), "Fatura não encontrada.");
        if (!"ABERTA".equals(texto(fatura, "status"))) {
            throw new BusinessException("Somente faturas abertas podem receber pagamentos.");
        }
        BigDecimal total = decimalValue(fatura, "total");
        BigDecimal pago = decimalValue(fatura, "valor_pago");
        BigDecimal saldo = total.subtract(pago);
        if (request.valor().compareTo(saldo) > 0) {
            throw new BusinessException("O pagamento não pode ser maior que o saldo da fatura.");
        }
        LocalDateTime pagoEm = request.pagoEm() == null ? LocalDateTime.now() : request.pagoEm();
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("faturaId", faturaId)
                .addValue("valor", request.valor())
                .addValue("forma", normalizarTextoObrigatorio(request.forma(), "A forma de pagamento deve ser informada.").toUpperCase(Locale.ROOT))
                .addValue("referencia", normalizarTexto(request.referencia()))
                .addValue("pagoEm", pagoEm);
        jdbcTemplate.update("""
                INSERT INTO billing_pagamento (fatura_id, valor, forma, referencia, pago_em)
                VALUES (:faturaId, :valor, :forma, :referencia, :pagoEm)
                """, parameters);
        BigDecimal novoPago = pago.add(request.valor());
        if (novoPago.compareTo(total) >= 0) {
            jdbcTemplate.update("""
                    UPDATE billing_fatura
                       SET status = 'PAGA', pago_em = :pagoEm, updated_at = NOW()
                     WHERE id = :faturaId
                    """, parameters);
        }
        return buscarFatura(faturaId, Optional.empty());
    }

    @Transactional(readOnly = true)
    public CapResumoDTO consultarCap() {
        Long transportadoraId = resolverTransportadoraLogada()
                .orElseThrow(() -> new AccessDeniedException("O portal CAP requer uma transportadora vinculada."));
        Transportadora transportadora = transportadoraRepository.findById(transportadoraId)
                .orElseThrow(() -> new AccessDeniedException("Transportadora autenticada sem vínculo válido."));

        Map<String, Object> agendamentos = consultarUmaLinha("""
                SELECT COUNT(*) AS total,
                       COALESCE(SUM(CASE WHEN status IN ('PENDENTE', 'CONFIRMADO', 'EM_ATENDIMENTO', 'EM_EXECUCAO') THEN 1 ELSE 0 END), 0) AS pendentes,
                       COALESCE(SUM(CASE WHEN status IN ('CONCLUIDO', 'COMPLETO') THEN 1 ELSE 0 END), 0) AS concluidos
                  FROM agendamento
                 WHERE transportadora_id = :transportadoraId
                """, new MapSqlParameterSource("transportadoraId", transportadoraId), "Não foi possível consolidar os agendamentos.");
        Map<String, Object> cobrancas = consultarUmaLinha("""
                SELECT COUNT(*) AS total, COALESCE(SUM(valor), 0) AS valor
                  FROM billing_cobranca
                 WHERE transportadora_id = :transportadoraId
                   AND status = 'PENDENTE'
                """, new MapSqlParameterSource("transportadoraId", transportadoraId), "Não foi possível consolidar as cobranças.");
        Map<String, Object> faturas = consultarUmaLinha("""
                SELECT COUNT(*) AS total, COALESCE(SUM(total), 0) AS valor
                  FROM billing_fatura
                 WHERE transportadora_id = :transportadoraId
                   AND status = 'ABERTA'
                """, new MapSqlParameterSource("transportadoraId", transportadoraId), "Não foi possível consolidar as faturas.");

        List<CapAgendamentoDTO> recentes = jdbcTemplate.queryForList("""
                SELECT id, codigo, tipo_operacao, status,
                       horario_previsto_chegada, horario_real_chegada, horario_real_saida
                  FROM agendamento
                 WHERE transportadora_id = :transportadoraId
                 ORDER BY COALESCE(horario_real_saida, horario_real_chegada, horario_previsto_chegada, created_at) DESC
                 LIMIT 10
                """, new MapSqlParameterSource("transportadoraId", transportadoraId)).stream()
                .map(this::mapearCapAgendamento)
                .collect(Collectors.toList());

        return new CapResumoDTO(
                transportadoraId,
                transportadora.getNome(),
                longValue(agendamentos, "total"),
                longValue(agendamentos, "pendentes"),
                longValue(agendamentos, "concluidos"),
                longValue(cobrancas, "total"),
                decimalValue(cobrancas, "valor"),
                longValue(faturas, "total"),
                decimalValue(faturas, "valor"),
                recentes,
                listarFaturasInterno(transportadoraId, null, 10));
    }

    private List<FaturaDTO> listarFaturasInterno(Long transportadoraId, String status, Integer limite) {
        String statusNormalizado = normalizarStatus(status, STATUS_FATURA);
        StringBuilder sql = new StringBuilder("""
                SELECT f.id,
                       f.numero,
                       f.transportadora_id,
                       t.nome AS transportadora,
                       f.emitida_em,
                       f.vencimento,
                       f.status,
                       f.subtotal,
                       f.total,
                       f.pago_em,
                       COALESCE((SELECT SUM(p.valor) FROM billing_pagamento p WHERE p.fatura_id = f.id), 0) AS valor_pago
                  FROM billing_fatura f
                  JOIN transportadora t ON t.id = f.transportadora_id
                 WHERE 1 = 1
                """);
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        if (transportadoraId != null) {
            sql.append(" AND f.transportadora_id = :transportadoraId");
            parameters.addValue("transportadoraId", transportadoraId);
        }
        if (statusNormalizado != null) {
            sql.append(" AND f.status = :status");
            parameters.addValue("status", statusNormalizado);
        }
        sql.append(" ORDER BY f.emitida_em DESC, f.id DESC");
        if (limite != null) {
            sql.append(" LIMIT :limite");
            parameters.addValue("limite", limite);
        }
        return jdbcTemplate.queryForList(sql.toString(), parameters).stream()
                .map(row -> mapearFatura(row, carregarItens(longValue(row, "id")), carregarPagamentos(longValue(row, "id"))))
                .collect(Collectors.toList());
    }

    private TarifaDTO buscarTarifa(Long id) {
        Map<String, Object> row = consultarUmaLinha("""
                SELECT id, codigo, descricao, tipo_operacao, valor, inicio_vigencia, fim_vigencia, ativa
                  FROM billing_tarifa
                 WHERE id = :id
                """, new MapSqlParameterSource("id", id), "Tarifa não encontrada.");
        return mapearTarifa(row);
    }

    private CobrancaDTO buscarCobranca(Long id, Optional<Long> transportadoraPermitida) {
        StringBuilder sql = new StringBuilder("""
                SELECT c.id, c.referencia, c.transportadora_id, t.nome AS transportadora,
                       c.agendamento_id, a.codigo AS agendamento, c.descricao, c.valor,
                       c.status, c.ocorrido_em, c.faturado_em
                  FROM billing_cobranca c
                  JOIN transportadora t ON t.id = c.transportadora_id
                  LEFT JOIN agendamento a ON a.id = c.agendamento_id
                 WHERE c.id = :id
                """);
        MapSqlParameterSource parameters = new MapSqlParameterSource("id", id);
        transportadoraPermitida.ifPresent(value -> {
            sql.append(" AND c.transportadora_id = :transportadoraId");
            parameters.addValue("transportadoraId", value);
        });
        return mapearCobranca(consultarUmaLinha(sql.toString(), parameters, "Cobrança não encontrada."));
    }

    private FaturaDTO buscarFatura(Long id, Optional<Long> transportadoraPermitida) {
        StringBuilder sql = new StringBuilder("""
                SELECT f.id, f.numero, f.transportadora_id, t.nome AS transportadora,
                       f.emitida_em, f.vencimento, f.status, f.subtotal, f.total, f.pago_em,
                       COALESCE((SELECT SUM(p.valor) FROM billing_pagamento p WHERE p.fatura_id = f.id), 0) AS valor_pago
                  FROM billing_fatura f
                  JOIN transportadora t ON t.id = f.transportadora_id
                 WHERE f.id = :id
                """);
        MapSqlParameterSource parameters = new MapSqlParameterSource("id", id);
        transportadoraPermitida.ifPresent(value -> {
            sql.append(" AND f.transportadora_id = :transportadoraId");
            parameters.addValue("transportadoraId", value);
        });
        Map<String, Object> row = consultarUmaLinha(sql.toString(), parameters, "Fatura não encontrada.");
        return mapearFatura(row, carregarItens(id), carregarPagamentos(id));
    }

    private List<FaturaItemDTO> carregarItens(Long faturaId) {
        return jdbcTemplate.queryForList("""
                SELECT id, cobranca_id, descricao, valor
                  FROM billing_fatura_item
                 WHERE fatura_id = :faturaId
                 ORDER BY id
                """, new MapSqlParameterSource("faturaId", faturaId)).stream()
                .map(row -> new FaturaItemDTO(
                        longValue(row, "id"),
                        longValue(row, "cobranca_id"),
                        texto(row, "descricao"),
                        decimalValue(row, "valor")))
                .collect(Collectors.toList());
    }

    private List<PagamentoDTO> carregarPagamentos(Long faturaId) {
        return jdbcTemplate.queryForList("""
                SELECT id, valor, forma, referencia, pago_em
                  FROM billing_pagamento
                 WHERE fatura_id = :faturaId
                 ORDER BY pago_em, id
                """, new MapSqlParameterSource("faturaId", faturaId)).stream()
                .map(row -> new PagamentoDTO(
                        longValue(row, "id"),
                        decimalValue(row, "valor"),
                        texto(row, "forma"),
                        textoNullable(row, "referencia"),
                        localDateTime(row.get("pago_em"))))
                .collect(Collectors.toList());
    }

    private TarifaDTO mapearTarifa(Map<String, Object> row) {
        return new TarifaDTO(
                longValue(row, "id"),
                texto(row, "codigo"),
                texto(row, "descricao"),
                TipoOperacao.valueOf(texto(row, "tipo_operacao")),
                decimalValue(row, "valor"),
                localDate(row.get("inicio_vigencia")),
                localDate(row.get("fim_vigencia")),
                Boolean.TRUE.equals(row.get("ativa")));
    }

    private CobrancaDTO mapearCobranca(Map<String, Object> row) {
        return new CobrancaDTO(
                longValue(row, "id"),
                texto(row, "referencia"),
                longValue(row, "transportadora_id"),
                texto(row, "transportadora"),
                longValueNullable(row, "agendamento_id"),
                textoNullable(row, "agendamento"),
                texto(row, "descricao"),
                decimalValue(row, "valor"),
                texto(row, "status"),
                localDateTime(row.get("ocorrido_em")),
                localDateTime(row.get("faturado_em")));
    }

    private FaturaDTO mapearFatura(Map<String, Object> row,
                                    List<FaturaItemDTO> itens,
                                    List<PagamentoDTO> pagamentos) {
        BigDecimal total = decimalValue(row, "total");
        BigDecimal valorPago = decimalValue(row, "valor_pago");
        BigDecimal saldo = total.subtract(valorPago).max(BigDecimal.ZERO);
        return new FaturaDTO(
                longValue(row, "id"),
                texto(row, "numero"),
                longValue(row, "transportadora_id"),
                texto(row, "transportadora"),
                localDateTime(row.get("emitida_em")),
                localDate(row.get("vencimento")),
                texto(row, "status"),
                decimalValue(row, "subtotal"),
                total,
                valorPago,
                saldo,
                localDateTime(row.get("pago_em")),
                List.copyOf(itens),
                List.copyOf(pagamentos));
    }

    private CapAgendamentoDTO mapearCapAgendamento(Map<String, Object> row) {
        return new CapAgendamentoDTO(
                longValue(row, "id"),
                texto(row, "codigo"),
                TipoOperacao.valueOf(texto(row, "tipo_operacao")),
                texto(row, "status"),
                localDateTime(row.get("horario_previsto_chegada")),
                localDateTime(row.get("horario_real_chegada")),
                localDateTime(row.get("horario_real_saida")));
    }

    private Optional<Long> resolverTransportadoraLogada() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !possuiRole(authentication, "ROLE_TRANSPORTADORA")) {
            return Optional.empty();
        }
        if (!(authentication instanceof JwtAuthenticationToken)) {
            throw new AccessDeniedException("A conta de transportadora deve usar autenticação JWT.");
        }
        Jwt token = ((JwtAuthenticationToken) authentication).getToken();
        String documento = token.getClaimAsString("transportadoraDocumento");
        if (!StringUtils.hasText(documento)) {
            documento = token.getClaimAsString("transportadoraCnpj");
        }
        if (!StringUtils.hasText(documento)) {
            throw new AccessDeniedException("Transportadora autenticada sem documento vinculado.");
        }
        String documentoNormalizado = documento.replaceAll("[^0-9A-Za-z]", "").toUpperCase(Locale.ROOT);
        return Optional.of(transportadoraRepository.findByDocumento(documentoNormalizado)
                .map(Transportadora::getId)
                .orElseThrow(() -> new AccessDeniedException("Transportadora autenticada sem vínculo válido.")));
    }

    private boolean possuiRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role::equals);
    }

    private Map<String, Object> consultarUmaLinha(String sql,
                                                  MapSqlParameterSource parameters,
                                                  String mensagemNaoEncontrado) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, parameters);
        if (rows.isEmpty()) {
            throw new NotFoundException(mensagemNaoEncontrado);
        }
        return rows.get(0);
    }

    private String normalizarCodigo(String valor) {
        String normalizado = normalizarTextoObrigatorio(valor, "O código da tarifa deve ser informado.")
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        if (!StringUtils.hasText(normalizado)) {
            throw new BusinessException("O código da tarifa é inválido.");
        }
        return normalizado;
    }

    private String normalizarTextoObrigatorio(String valor, String mensagem) {
        String normalizado = normalizarTexto(valor);
        if (!StringUtils.hasText(normalizado)) {
            throw new BusinessException(mensagem);
        }
        return normalizado;
    }

    private String normalizarTexto(String valor) {
        if (!StringUtils.hasText(valor)) {
            return null;
        }
        return valor.trim().replaceAll("[<>\"'`]", "");
    }

    private void validarVigencia(LocalDate inicio, LocalDate fim) {
        if (fim != null && fim.isBefore(inicio)) {
            throw new BusinessException("O fim da vigência não pode ser anterior ao início.");
        }
    }

    private String normalizarStatus(String status, Set<String> permitidos) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        String normalizado = status.trim().toUpperCase(Locale.ROOT);
        if (!permitidos.contains(normalizado)) {
            throw new BusinessException("Status inválido: " + status);
        }
        return normalizado;
    }

    private String texto(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value == null) {
            throw new IllegalStateException("Campo obrigatório ausente na consulta: " + key);
        }
        return String.valueOf(value);
    }

    private String textoNullable(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private Long longValue(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value == null) {
            throw new IllegalStateException("Campo numérico obrigatório ausente: " + key);
        }
        return Long.valueOf(String.valueOf(value));
    }

    private Long longValueNullable(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value == null ? null : value instanceof Number
                ? ((Number) value).longValue()
                : Long.valueOf(String.valueOf(value));
    }

    private BigDecimal decimalValue(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        return new BigDecimal(String.valueOf(value));
    }

    private LocalDate localDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate) {
            return (LocalDate) value;
        }
        if (value instanceof Date) {
            return ((Date) value).toLocalDate();
        }
        return LocalDate.parse(String.valueOf(value));
    }

    private LocalDateTime localDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime) {
            return (LocalDateTime) value;
        }
        if (value instanceof Timestamp) {
            return ((Timestamp) value).toLocalDateTime();
        }
        return LocalDateTime.parse(String.valueOf(value).replace(' ', 'T'));
    }
}
