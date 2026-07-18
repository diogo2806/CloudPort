package br.com.cloudport.servicogate.app.operacional;

import br.com.cloudport.servicogate.app.operacional.dto.GateComplementarDtos.GateComplementarDTO;
import br.com.cloudport.servicogate.app.operacional.dto.GateComplementarDtos.VinculoBillOfLadingDTO;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.AccessRuleDTO;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.AccessRuleRequest;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.BillOfLadingDTO;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.BillOfLadingRequest;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.OrderDTO;
import br.com.cloudport.servicogate.exception.BusinessException;
import br.com.cloudport.servicogate.exception.NotFoundException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class GateComplementarService {

    private static final Set<String> ESCOPOS = Set.of("MOTORISTA", "TRANSPORTADORA", "VEICULO");
    private static final Set<String> TIPOS_REGRA = Set.of("BLOQUEIO", "PERMISSAO");

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public GateComplementarService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public GateComplementarDTO listar(Long facilityId) {
        return new GateComplementarDTO(listarBillsOfLading(), listarRegras(facilityId));
    }

    @Transactional
    public BillOfLadingDTO salvarBillOfLading(BillOfLadingRequest request) {
        validarVigencia(request.validadeInicio(), request.validadeFim());
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("numero", normalizarCodigo(request.numero()))
                .addValue("armador", limpar(request.armador()))
                .addValue("viagem", limpar(request.viagem()))
                .addValue("consignatario", limpar(request.consignatario()))
                .addValue("quantidadeTotal", request.quantidadeTotal())
                .addValue("validadeInicio", request.validadeInicio())
                .addValue("validadeFim", request.validadeFim())
                .addValue("observacoes", limpar(request.observacoes()));
        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO gate_bill_of_lading (
                    numero, armador, viagem, consignatario, quantidade_total,
                    validade_inicio, validade_fim, observacoes
                ) VALUES (
                    :numero, :armador, :viagem, :consignatario, :quantidadeTotal,
                    :validadeInicio, :validadeFim, :observacoes
                )
                ON CONFLICT (numero) DO UPDATE SET
                    armador = EXCLUDED.armador,
                    viagem = EXCLUDED.viagem,
                    consignatario = EXCLUDED.consignatario,
                    quantidade_total = GREATEST(
                        EXCLUDED.quantidade_total,
                        gate_bill_of_lading.quantidade_liberada
                    ),
                    validade_inicio = EXCLUDED.validade_inicio,
                    validade_fim = EXCLUDED.validade_fim,
                    observacoes = EXCLUDED.observacoes,
                    updated_at = NOW()
                RETURNING id
                """, parameters, Long.class);
        return buscarBillOfLading(id);
    }

    @Transactional
    public AccessRuleDTO salvarRegra(AccessRuleRequest request) {
        validarVigencia(request.inicioVigencia(), request.fimVigencia());
        String escopo = normalizarEnum(request.escopo(), ESCOPOS, "Escopo da regra de acesso inválido.");
        String tipo = normalizarEnum(request.tipo(), TIPOS_REGRA, "Tipo da regra de acesso inválido.");
        validarReferencia(escopo, request.referenciaId());
        garantirGate(request.gateId());
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("gateId", request.gateId())
                .addValue("escopo", escopo)
                .addValue("referenciaId", request.referenciaId())
                .addValue("tipo", tipo)
                .addValue("motivo", normalizarTexto(request.motivo(), "O motivo da regra deve ser informado."))
                .addValue("inicioVigencia", request.inicioVigencia())
                .addValue("fimVigencia", request.fimVigencia())
                .addValue("ativo", request.ativo() == null || request.ativo());
        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO gate_access_rule (
                    gate_id, escopo, referencia_id, tipo, motivo,
                    inicio_vigencia, fim_vigencia, ativo
                ) VALUES (
                    :gateId, :escopo, :referenciaId, :tipo, :motivo,
                    :inicioVigencia, :fimVigencia, :ativo
                )
                ON CONFLICT (gate_id, escopo, referencia_id, tipo) DO UPDATE SET
                    motivo = EXCLUDED.motivo,
                    inicio_vigencia = EXCLUDED.inicio_vigencia,
                    fim_vigencia = EXCLUDED.fim_vigencia,
                    ativo = EXCLUDED.ativo,
                    updated_at = NOW()
                RETURNING id
                """, parameters, Long.class);
        return buscarRegra(id);
    }

    @Transactional
    public VinculoBillOfLadingDTO vincularBillOfLading(Long ordemId, Long billOfLadingId) {
        BillOfLadingDTO billOfLading = buscarBillOfLading(billOfLadingId);
        if (!Set.of("ATIVO", "PARCIAL").contains(billOfLading.status())) {
            throw new BusinessException("O Bill of Lading não está disponível para vínculo.");
        }
        int atualizados = jdbcTemplate.update("""
                UPDATE gate_order
                   SET bill_of_lading_id = :billOfLadingId,
                       updated_at = NOW()
                 WHERE id = :ordemId
                   AND status = 'ATIVA'
                """, new MapSqlParameterSource()
                .addValue("billOfLadingId", billOfLadingId)
                .addValue("ordemId", ordemId));
        if (atualizados == 0) {
            throw new NotFoundException("Ordem ativa de Gate não encontrada.");
        }
        return new VinculoBillOfLadingDTO(buscarOrdem(ordemId), billOfLading);
    }

    @Transactional(readOnly = true)
    public List<BillOfLadingDTO> listarBillsOfLading() {
        return jdbcTemplate.queryForList("""
                SELECT id, numero, armador, viagem, consignatario, quantidade_total,
                       quantidade_liberada, status, validade_inicio, validade_fim, observacoes
                  FROM gate_bill_of_lading
                 ORDER BY CASE status
                     WHEN 'ATIVO' THEN 0
                     WHEN 'PARCIAL' THEN 1
                     ELSE 2
                 END,
                 validade_fim NULLS LAST,
                 numero
                 LIMIT 500
                """, new MapSqlParameterSource()).stream()
                .map(this::mapearBillOfLading)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AccessRuleDTO> listarRegras(Long facilityId) {
        StringBuilder sql = new StringBuilder("""
                SELECT regra.id, regra.gate_id, regra.escopo, regra.referencia_id,
                       regra.tipo, regra.motivo, regra.inicio_vigencia,
                       regra.fim_vigencia, regra.ativo
                  FROM gate_access_rule regra
                  JOIN gate_configuracao gate_cfg ON gate_cfg.id = regra.gate_id
                 WHERE 1 = 1
                """);
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        if (facilityId != null) {
            sql.append(" AND gate_cfg.facility_id = :facilityId");
            parameters.addValue("facilityId", facilityId);
        }
        sql.append(" ORDER BY regra.ativo DESC, regra.tipo, regra.escopo, regra.id DESC");
        return jdbcTemplate.queryForList(sql.toString(), parameters).stream()
                .map(this::mapearRegra)
                .collect(Collectors.toList());
    }

    private BillOfLadingDTO buscarBillOfLading(Long id) {
        Map<String, Object> row = consultarUmaLinha("""
                SELECT id, numero, armador, viagem, consignatario, quantidade_total,
                       quantidade_liberada, status, validade_inicio, validade_fim, observacoes
                  FROM gate_bill_of_lading
                 WHERE id = :id
                """, new MapSqlParameterSource("id", id), "Bill of Lading não encontrado.");
        return mapearBillOfLading(row);
    }

    private AccessRuleDTO buscarRegra(Long id) {
        Map<String, Object> row = consultarUmaLinha("""
                SELECT id, gate_id, escopo, referencia_id, tipo, motivo,
                       inicio_vigencia, fim_vigencia, ativo
                  FROM gate_access_rule
                 WHERE id = :id
                """, new MapSqlParameterSource("id", id), "Regra de acesso não encontrada.");
        return mapearRegra(row);
    }

    private OrderDTO buscarOrdem(Long id) {
        Map<String, Object> row = consultarUmaLinha("""
                SELECT id, tipo, codigo, booking_id, bill_of_lading_id, transportadora_id,
                       unidade_referencia, status, validade_inicio, validade_fim
                  FROM gate_order
                 WHERE id = :id
                """, new MapSqlParameterSource("id", id), "Ordem de Gate não encontrada.");
        return new OrderDTO(
                longValue(row, "id"),
                texto(row, "tipo"),
                texto(row, "codigo"),
                nullableLong(row, "booking_id"),
                nullableLong(row, "bill_of_lading_id"),
                nullableLong(row, "transportadora_id"),
                texto(row, "unidade_referencia"),
                texto(row, "status"),
                localDateTime(row.get("validade_inicio")),
                localDateTime(row.get("validade_fim")));
    }

    private void validarReferencia(String escopo, Long referenciaId) {
        String tabela = switch (escopo) {
            case "MOTORISTA" -> "motorista";
            case "TRANSPORTADORA" -> "transportadora";
            case "VEICULO" -> "veiculo";
            default -> throw new BusinessException("Escopo da regra de acesso inválido.");
        };
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + tabela + " WHERE id = :id",
                new MapSqlParameterSource("id", referenciaId),
                Long.class);
        if (count == null || count == 0) {
            throw new NotFoundException("Referência da regra de acesso não encontrada.");
        }
    }

    private void garantirGate(Long gateId) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM gate_configuracao WHERE id = :id
                """, new MapSqlParameterSource("id", gateId), Long.class);
        if (count == null || count == 0) {
            throw new NotFoundException("Gate não encontrado.");
        }
    }

    private Map<String, Object> consultarUmaLinha(String sql, MapSqlParameterSource parameters,
                                                  String mensagem) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, parameters);
        if (rows.isEmpty()) {
            throw new NotFoundException(mensagem);
        }
        return rows.get(0);
    }

    private BillOfLadingDTO mapearBillOfLading(Map<String, Object> row) {
        return new BillOfLadingDTO(
                longValue(row, "id"),
                texto(row, "numero"),
                texto(row, "armador"),
                texto(row, "viagem"),
                texto(row, "consignatario"),
                intValue(row, "quantidade_total"),
                intValue(row, "quantidade_liberada"),
                texto(row, "status"),
                localDateTime(row.get("validade_inicio")),
                localDateTime(row.get("validade_fim")),
                texto(row, "observacoes"));
    }

    private AccessRuleDTO mapearRegra(Map<String, Object> row) {
        return new AccessRuleDTO(
                longValue(row, "id"),
                longValue(row, "gate_id"),
                texto(row, "escopo"),
                longValue(row, "referencia_id"),
                texto(row, "tipo"),
                texto(row, "motivo"),
                localDateTime(row.get("inicio_vigencia")),
                localDateTime(row.get("fim_vigencia")),
                boolValue(row, "ativo"));
    }

    private void validarVigencia(LocalDateTime inicio, LocalDateTime fim) {
        if (inicio != null && fim != null && fim.isBefore(inicio)) {
            throw new BusinessException("A validade final não pode ser anterior à validade inicial.");
        }
    }

    private String normalizarEnum(String valor, Set<String> permitidos, String mensagem) {
        String normalizado = normalizarCodigo(valor);
        if (!permitidos.contains(normalizado)) {
            throw new BusinessException(mensagem);
        }
        return normalizado;
    }

    private String normalizarCodigo(String valor) {
        return normalizarTexto(valor, "O código deve ser informado.")
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9._/-]", "_");
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

    private long longValue(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value instanceof Number number) {
            return number.longValue();
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
        return Integer.parseInt(value.toString());
    }

    private boolean boolValue(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value instanceof Boolean bool) {
            return bool;
        }
        return value != null && Boolean.parseBoolean(value.toString());
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