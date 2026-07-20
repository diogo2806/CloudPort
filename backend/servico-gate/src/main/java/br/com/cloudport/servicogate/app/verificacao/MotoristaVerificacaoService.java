package br.com.cloudport.servicogate.app.verificacao;

import br.com.cloudport.servicogate.app.gestor.dto.GateFlowRequest;
import br.com.cloudport.servicogate.app.verificacao.MotoristaVerificacaoDtos.CredencialMotoristaDTO;
import br.com.cloudport.servicogate.app.verificacao.MotoristaVerificacaoDtos.CredencialMotoristaRequest;
import br.com.cloudport.servicogate.app.verificacao.MotoristaVerificacaoDtos.OverrideVerificacaoMotoristaRequest;
import br.com.cloudport.servicogate.app.verificacao.MotoristaVerificacaoDtos.VerificacaoMotoristaDTO;
import br.com.cloudport.servicogate.app.verificacao.MotoristaVerificacaoDtos.VerificacaoMotoristaRequest;
import br.com.cloudport.servicogate.exception.BusinessException;
import br.com.cloudport.servicogate.exception.NotFoundException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class MotoristaVerificacaoService {

    private static final Set<String> METODOS = Set.of("PIN", "DOCUMENTO", "CREDENCIAL");
    private static final Set<String> TIPOS_CREDENCIAL = Set.of("PIN", "CREDENCIAL");
    private static final Duration DURACAO_VERIFICACAO = Duration.ofMinutes(30);
    private static final Duration DURACAO_BLOQUEIO = Duration.ofMinutes(15);
    private static final int LIMITE_TENTATIVAS = 3;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    public MotoristaVerificacaoService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public VerificacaoMotoristaDTO consultarVisita(Long visitaId) {
        IdentidadeOperacional identidade = resolverIdentidadeVisita(visitaId);
        garantirEstado(identidade);
        atualizarEstadoTemporal(identidade.chaveOperacional());
        return montarDto(identidade);
    }

    public VerificacaoMotoristaDTO consultarAgendamento(Long agendamentoId) {
        IdentidadeOperacional identidade = resolverIdentidadeAgendamento(agendamentoId);
        garantirEstado(identidade);
        atualizarEstadoTemporal(identidade.chaveOperacional());
        return montarDto(identidade);
    }

    public VerificacaoMotoristaDTO validarVisita(Long visitaId, VerificacaoMotoristaRequest request) {
        return validar(resolverIdentidadeVisita(visitaId), request);
    }

    public VerificacaoMotoristaDTO validarAgendamento(Long agendamentoId, VerificacaoMotoristaRequest request) {
        return validar(resolverIdentidadeAgendamento(agendamentoId), request);
    }

    public VerificacaoMotoristaDTO autorizarOverrideVisita(
            Long visitaId,
            OverrideVerificacaoMotoristaRequest request) {
        return autorizarOverride(resolverIdentidadeVisita(visitaId), request);
    }

    public VerificacaoMotoristaDTO autorizarOverrideAgendamento(
            Long agendamentoId,
            OverrideVerificacaoMotoristaRequest request) {
        return autorizarOverride(resolverIdentidadeAgendamento(agendamentoId), request);
    }

    public CredencialMotoristaDTO cadastrarCredencial(
            Long motoristaId,
            CredencialMotoristaRequest request) {
        String tipo = normalizarEnum(request.tipo(), TIPOS_CREDENCIAL, "Tipo de credencial inválido.");
        String valor = normalizarSegredo(tipo, request.valor());
        if (valor.length() < 4) {
            throw new BusinessException("A credencial deve possuir ao menos quatro caracteres.");
        }
        if (request.validadeInicio() != null && request.validadeFim() != null
                && !request.validadeFim().isAfter(request.validadeInicio())) {
            throw new BusinessException("A validade final deve ser posterior à validade inicial.");
        }

        Map<String, Object> motorista = consultarUmaLinha("""
                SELECT m.id, m.transportadora_id
                  FROM motorista m
                 WHERE m.id = :motoristaId
                """, new MapSqlParameterSource("motoristaId", motoristaId), "Motorista não encontrado.");
        Long transportadoraId = longValue(motorista, "transportadora_id");
        String usuario = normalizarUsuario(request.usuario());

        jdbcTemplate.update("""
                UPDATE gate_driver_credential
                   SET status = 'REVOGADA', revogado_por = :usuario, revogado_em = NOW(), updated_at = NOW()
                 WHERE motorista_id = :motoristaId
                   AND transportadora_id = :transportadoraId
                   AND tipo = :tipo
                   AND status = 'ATIVA'
                """, new MapSqlParameterSource()
                .addValue("usuario", usuario)
                .addValue("motoristaId", motoristaId)
                .addValue("transportadoraId", transportadoraId)
                .addValue("tipo", tipo));

        byte[] salt = new byte[16];
        secureRandom.nextBytes(salt);
        String saltBase64 = Base64.getEncoder().encodeToString(salt);
        String hashBase64 = calcularHash(valor, salt);
        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO gate_driver_credential (
                    motorista_id, transportadora_id, tipo, segredo_hash, salt, status,
                    validade_inicio, validade_fim, cadastrado_por
                ) VALUES (
                    :motoristaId, :transportadoraId, :tipo, :hash, :salt, 'ATIVA',
                    :validadeInicio, :validadeFim, :usuario
                )
                RETURNING id
                """, new MapSqlParameterSource()
                .addValue("motoristaId", motoristaId)
                .addValue("transportadoraId", transportadoraId)
                .addValue("tipo", tipo)
                .addValue("hash", hashBase64)
                .addValue("salt", saltBase64)
                .addValue("validadeInicio", request.validadeInicio())
                .addValue("validadeFim", request.validadeFim())
                .addValue("usuario", usuario), Long.class);

        return new CredencialMotoristaDTO(
                id,
                motoristaId,
                transportadoraId,
                tipo,
                "ATIVA",
                request.validadeInicio(),
                request.validadeFim(),
                usuario,
                LocalDateTime.now());
    }

    public void exigirVerificacaoVisita(Long visitaId) {
        exigirVerificacao(resolverIdentidadeVisita(visitaId));
    }

    public void exigirVerificacaoEntrada(GateFlowRequest request) {
        if (request == null) {
            throw new BusinessException("Dados da entrada não informados.");
        }
        Long agendamentoId = localizarAgendamento(request);
        exigirVerificacao(resolverIdentidadeAgendamento(agendamentoId));
    }

    private VerificacaoMotoristaDTO validar(
            IdentidadeOperacional identidade,
            VerificacaoMotoristaRequest request) {
        String metodo = normalizarEnum(request.metodo(), METODOS, "Método de verificação inválido.");
        String usuario = normalizarUsuario(request.usuario());
        garantirEstado(identidade);
        atualizarEstadoTemporal(identidade.chaveOperacional());
        Map<String, Object> estado = consultarEstado(identidade.chaveOperacional());
        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime bloqueadoAte = dateTimeValue(estado, "bloqueado_ate");

        if ("BLOQUEADA".equals(textValue(estado, "status"))
                && bloqueadoAte != null
                && bloqueadoAte.isAfter(agora)) {
            registrarTentativa(
                    longValue(estado, "id"),
                    metodo,
                    "BLOQUEADA",
                    "Verificação bloqueada até " + bloqueadoAte,
                    usuario);
            return montarDto(identidade);
        }

        boolean aprovada = conferirValor(identidade, metodo, request.valor());
        int tentativasAtuais = intValue(estado, "tentativas");
        int limite = intValue(estado, "limite_tentativas");
        if (limite <= 0) {
            limite = LIMITE_TENTATIVAS;
        }

        if (aprovada) {
            LocalDateTime expiraEm = agora.plus(DURACAO_VERIFICACAO);
            jdbcTemplate.update("""
                    UPDATE gate_driver_verification
                       SET status = 'VERIFICADA', metodo = :metodo, tentativas = 0,
                           bloqueado_ate = NULL, verificado_em = :agora, expira_em = :expiraEm,
                           verificado_por = :usuario, override_por = NULL, motivo_override = NULL,
                           ultimo_motivo = NULL, updated_at = NOW()
                     WHERE chave_operacional = :chave
                    """, new MapSqlParameterSource()
                    .addValue("metodo", metodo)
                    .addValue("agora", agora)
                    .addValue("expiraEm", expiraEm)
                    .addValue("usuario", usuario)
                    .addValue("chave", identidade.chaveOperacional()));
            registrarTentativa(longValue(estado, "id"), metodo, "APROVADA", null, usuario);
            return montarDto(identidade);
        }

        int novasTentativas = tentativasAtuais + 1;
        boolean bloquear = novasTentativas >= limite;
        LocalDateTime novoBloqueio = bloquear ? agora.plus(DURACAO_BLOQUEIO) : null;
        String motivo = bloquear
                ? "Limite de tentativas atingido. Verificação bloqueada temporariamente."
                : "Credencial inválida para o motorista, transportadora e visita informados.";
        jdbcTemplate.update("""
                UPDATE gate_driver_verification
                   SET status = :status, metodo = :metodo, tentativas = :tentativas,
                       bloqueado_ate = :bloqueadoAte, verificado_em = NULL, expira_em = NULL,
                       verificado_por = NULL, override_por = NULL, motivo_override = NULL,
                       ultimo_motivo = :motivo, updated_at = NOW()
                 WHERE chave_operacional = :chave
                """, new MapSqlParameterSource()
                .addValue("status", bloquear ? "BLOQUEADA" : "PENDENTE")
                .addValue("metodo", metodo)
                .addValue("tentativas", novasTentativas)
                .addValue("bloqueadoAte", novoBloqueio)
                .addValue("motivo", motivo)
                .addValue("chave", identidade.chaveOperacional()));
        registrarTentativa(
                longValue(estado, "id"),
                metodo,
                bloquear ? "BLOQUEADA" : "NEGADA",
                motivo,
                usuario);
        return montarDto(identidade);
    }

    private VerificacaoMotoristaDTO autorizarOverride(
            IdentidadeOperacional identidade,
            OverrideVerificacaoMotoristaRequest request) {
        garantirEstado(identidade);
        String usuario = normalizarUsuario(request.usuario());
        String motivo = normalizarTexto(request.motivo(), "O motivo do override deve ser informado.");
        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime expiraEm = agora.plus(DURACAO_VERIFICACAO);
        jdbcTemplate.update("""
                UPDATE gate_driver_verification
                   SET status = 'OVERRIDE', metodo = 'OVERRIDE', tentativas = 0,
                       bloqueado_ate = NULL, verificado_em = :agora, expira_em = :expiraEm,
                       verificado_por = :usuario, override_por = :usuario,
                       motivo_override = :motivo, ultimo_motivo = NULL, updated_at = NOW()
                 WHERE chave_operacional = :chave
                """, new MapSqlParameterSource()
                .addValue("agora", agora)
                .addValue("expiraEm", expiraEm)
                .addValue("usuario", usuario)
                .addValue("motivo", motivo)
                .addValue("chave", identidade.chaveOperacional()));
        Map<String, Object> estado = consultarEstado(identidade.chaveOperacional());
        registrarTentativa(longValue(estado, "id"), "OVERRIDE", "OVERRIDE", motivo, usuario);
        return montarDto(identidade);
    }

    private void exigirVerificacao(IdentidadeOperacional identidade) {
        garantirEstado(identidade);
        atualizarEstadoTemporal(identidade.chaveOperacional());
        Map<String, Object> estado = consultarEstado(identidade.chaveOperacional());
        String status = textValue(estado, "status");
        if (Set.of("VERIFICADA", "OVERRIDE").contains(status)) {
            return;
        }
        if ("BLOQUEADA".equals(status)) {
            LocalDateTime bloqueadoAte = dateTimeValue(estado, "bloqueado_ate");
            throw new BusinessException("Verificação do motorista bloqueada até " + bloqueadoAte + ".");
        }
        if ("EXPIRADA".equals(status)) {
            throw new BusinessException("A verificação do motorista expirou e deve ser realizada novamente.");
        }
        throw new BusinessException("Verifique a identidade do motorista antes de avançar a visita no Gate.");
    }

    private boolean conferirValor(
            IdentidadeOperacional identidade,
            String metodo,
            String valorInformado) {
        if ("DOCUMENTO".equals(metodo)) {
            String informado = normalizarDocumento(valorInformado);
            String cadastrado = normalizarDocumento(identidade.documentoMotorista());
            return StringUtils.hasText(informado)
                    && StringUtils.hasText(cadastrado)
                    && MessageDigest.isEqual(
                            informado.getBytes(StandardCharsets.UTF_8),
                            cadastrado.getBytes(StandardCharsets.UTF_8));
        }

        String segredo = normalizarSegredo(metodo, valorInformado);
        List<Map<String, Object>> credenciais = jdbcTemplate.queryForList("""
                SELECT segredo_hash, salt
                  FROM gate_driver_credential
                 WHERE motorista_id = :motoristaId
                   AND transportadora_id = :transportadoraId
                   AND tipo = :tipo
                   AND status = 'ATIVA'
                   AND (validade_inicio IS NULL OR validade_inicio <= NOW())
                   AND (validade_fim IS NULL OR validade_fim > NOW())
                """, new MapSqlParameterSource()
                .addValue("motoristaId", identidade.motoristaId())
                .addValue("transportadoraId", identidade.transportadoraId())
                .addValue("tipo", metodo));
        for (Map<String, Object> credencial : credenciais) {
            byte[] salt = Base64.getDecoder().decode(String.valueOf(credencial.get("salt")));
            byte[] esperado = Base64.getDecoder().decode(String.valueOf(credencial.get("segredo_hash")));
            byte[] calculado = Base64.getDecoder().decode(calcularHash(segredo, salt));
            if (MessageDigest.isEqual(esperado, calculado)) {
                return true;
            }
        }
        return false;
    }

    private void garantirEstado(IdentidadeOperacional identidade) {
        jdbcTemplate.update("""
                INSERT INTO gate_driver_verification (
                    chave_operacional, truck_visit_id, agendamento_id, motorista_id,
                    transportadora_id, status, tentativas, limite_tentativas
                ) VALUES (
                    :chave, :truckVisitId, :agendamentoId, :motoristaId,
                    :transportadoraId, 'PENDENTE', 0, :limite
                )
                ON CONFLICT (chave_operacional) DO UPDATE SET
                    truck_visit_id = COALESCE(EXCLUDED.truck_visit_id, gate_driver_verification.truck_visit_id),
                    agendamento_id = COALESCE(EXCLUDED.agendamento_id, gate_driver_verification.agendamento_id),
                    motorista_id = EXCLUDED.motorista_id,
                    transportadora_id = EXCLUDED.transportadora_id,
                    updated_at = NOW()
                """, new MapSqlParameterSource()
                .addValue("chave", identidade.chaveOperacional())
                .addValue("truckVisitId", identidade.truckVisitId())
                .addValue("agendamentoId", identidade.agendamentoId())
                .addValue("motoristaId", identidade.motoristaId())
                .addValue("transportadoraId", identidade.transportadoraId())
                .addValue("limite", LIMITE_TENTATIVAS));
    }

    private void atualizarEstadoTemporal(String chaveOperacional) {
        jdbcTemplate.update("""
                UPDATE gate_driver_verification
                   SET status = 'PENDENTE', tentativas = 0, bloqueado_ate = NULL,
                       ultimo_motivo = NULL, updated_at = NOW()
                 WHERE chave_operacional = :chave
                   AND status = 'BLOQUEADA'
                   AND bloqueado_ate IS NOT NULL
                   AND bloqueado_ate <= NOW()
                """, new MapSqlParameterSource("chave", chaveOperacional));
        jdbcTemplate.update("""
                UPDATE gate_driver_verification
                   SET status = 'EXPIRADA', ultimo_motivo = 'A verificação operacional expirou.',
                       updated_at = NOW()
                 WHERE chave_operacional = :chave
                   AND status IN ('VERIFICADA', 'OVERRIDE')
                   AND expira_em IS NOT NULL
                   AND expira_em <= NOW()
                """, new MapSqlParameterSource("chave", chaveOperacional));
    }

    private void registrarTentativa(
            Long verificacaoId,
            String metodo,
            String resultado,
            String motivo,
            String operador) {
        jdbcTemplate.update("""
                INSERT INTO gate_driver_verification_attempt (
                    verificacao_id, metodo, resultado, motivo, operador
                ) VALUES (
                    :verificacaoId, :metodo, :resultado, :motivo, :operador
                )
                """, new MapSqlParameterSource()
                .addValue("verificacaoId", verificacaoId)
                .addValue("metodo", metodo)
                .addValue("resultado", resultado)
                .addValue("motivo", motivo)
                .addValue("operador", operador));
    }

    private Long localizarAgendamento(GateFlowRequest request) {
        if (StringUtils.hasText(request.getQrCode())) {
            return jdbcTemplate.queryForObject("""
                    SELECT id FROM agendamento WHERE codigo = :codigo
                    """, new MapSqlParameterSource("codigo", request.getQrCode().trim()), Long.class);
        }
        if (StringUtils.hasText(request.getPlaca())) {
            List<Long> ids = jdbcTemplate.queryForList("""
                    SELECT a.id
                      FROM agendamento a
                      JOIN veiculo v ON v.id = a.veiculo_id
                     WHERE UPPER(v.placa) = UPPER(:placa)
                       AND a.status IN ('CONFIRMADO', 'EM_ATENDIMENTO', 'EM_EXECUCAO')
                     ORDER BY a.horario_previsto_chegada ASC
                     LIMIT 1
                    """, new MapSqlParameterSource("placa", request.getPlaca().trim()), Long.class);
            if (!ids.isEmpty()) {
                return ids.get(0);
            }
        }
        throw new NotFoundException("Agendamento não encontrado para verificar o motorista.");
    }

    private IdentidadeOperacional resolverIdentidadeVisita(Long visitaId) {
        Map<String, Object> row = consultarUmaLinha("""
                SELECT tv.id AS truck_visit_id, tv.agendamento_id, tv.motorista_id,
                       tv.transportadora_id, m.nome AS motorista_nome,
                       m.documento AS motorista_documento, t.nome AS transportadora_nome
                  FROM truck_visit tv
                  JOIN motorista m ON m.id = tv.motorista_id
                  JOIN transportadora t ON t.id = tv.transportadora_id
                 WHERE tv.id = :visitaId
                """, new MapSqlParameterSource("visitaId", visitaId), "Truck visit não encontrada.");
        Long agendamentoId = nullableLong(row, "agendamento_id");
        return new IdentidadeOperacional(
                agendamentoId != null ? "AGENDAMENTO:" + agendamentoId : "VISITA:" + visitaId,
                visitaId,
                agendamentoId,
                longValue(row, "motorista_id"),
                textValue(row, "motorista_nome"),
                textValue(row, "motorista_documento"),
                longValue(row, "transportadora_id"),
                textValue(row, "transportadora_nome"));
    }

    private IdentidadeOperacional resolverIdentidadeAgendamento(Long agendamentoId) {
        Map<String, Object> row = consultarUmaLinha("""
                SELECT a.id AS agendamento_id,
                       (SELECT tv.id FROM truck_visit tv
                         WHERE tv.agendamento_id = a.id
                         ORDER BY tv.created_at DESC LIMIT 1) AS truck_visit_id,
                       a.motorista_id, a.transportadora_id, m.nome AS motorista_nome,
                       m.documento AS motorista_documento, t.nome AS transportadora_nome
                  FROM agendamento a
                  JOIN motorista m ON m.id = a.motorista_id
                  JOIN transportadora t ON t.id = a.transportadora_id
                 WHERE a.id = :agendamentoId
                """, new MapSqlParameterSource("agendamentoId", agendamentoId), "Agendamento não encontrado.");
        return new IdentidadeOperacional(
                "AGENDAMENTO:" + agendamentoId,
                nullableLong(row, "truck_visit_id"),
                agendamentoId,
                longValue(row, "motorista_id"),
                textValue(row, "motorista_nome"),
                textValue(row, "motorista_documento"),
                longValue(row, "transportadora_id"),
                textValue(row, "transportadora_nome"));
    }

    private VerificacaoMotoristaDTO montarDto(IdentidadeOperacional identidade) {
        Map<String, Object> estado = consultarEstado(identidade.chaveOperacional());
        int tentativas = intValue(estado, "tentativas");
        int limite = intValue(estado, "limite_tentativas");
        return new VerificacaoMotoristaDTO(
                longValue(estado, "id"),
                identidade.chaveOperacional(),
                identidade.truckVisitId(),
                identidade.agendamentoId(),
                identidade.motoristaId(),
                identidade.motoristaNome(),
                identidade.transportadoraId(),
                identidade.transportadoraNome(),
                textValue(estado, "status"),
                textValue(estado, "metodo"),
                tentativas,
                limite,
                Math.max(0, limite - tentativas),
                dateTimeValue(estado, "bloqueado_ate"),
                dateTimeValue(estado, "verificado_em"),
                dateTimeValue(estado, "expira_em"),
                textValue(estado, "verificado_por"),
                textValue(estado, "override_por"),
                textValue(estado, "motivo_override"),
                textValue(estado, "ultimo_motivo"));
    }

    private Map<String, Object> consultarEstado(String chaveOperacional) {
        return consultarUmaLinha("""
                SELECT * FROM gate_driver_verification WHERE chave_operacional = :chave
                """, new MapSqlParameterSource("chave", chaveOperacional),
                "Estado da verificação do motorista não encontrado.");
    }

    private Map<String, Object> consultarUmaLinha(
            String sql,
            MapSqlParameterSource parameters,
            String mensagem) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, parameters);
        if (rows.isEmpty()) {
            throw new NotFoundException(mensagem);
        }
        return rows.get(0);
    }

    private String calcularHash(String valor, byte[] salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt);
            return Base64.getEncoder().encodeToString(
                    digest.digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível.", exception);
        }
    }

    private String normalizarEnum(String valor, Set<String> permitidos, String mensagem) {
        if (!StringUtils.hasText(valor)) {
            throw new BusinessException(mensagem);
        }
        String normalizado = valor.trim().toUpperCase(Locale.ROOT);
        if (!permitidos.contains(normalizado)) {
            throw new BusinessException(mensagem);
        }
        return normalizado;
    }

    private String normalizarSegredo(String tipo, String valor) {
        String normalizado = normalizarTexto(valor, "O valor da credencial deve ser informado.").trim();
        return "CREDENCIAL".equals(tipo) ? normalizado.toUpperCase(Locale.ROOT) : normalizado;
    }

    private String normalizarDocumento(String valor) {
        return valor == null ? "" : valor.replaceAll("\\D", "");
    }

    private String normalizarUsuario(String usuario) {
        return normalizarTexto(usuario, "O usuário responsável deve ser informado.");
    }

    private String normalizarTexto(String valor, String mensagem) {
        if (!StringUtils.hasText(valor)) {
            throw new BusinessException(mensagem);
        }
        return valor.trim();
    }

    private Long longValue(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value != null) {
            return Long.valueOf(value.toString());
        }
        throw new IllegalStateException("Campo obrigatório ausente: " + key);
    }

    private Long nullableLong(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value == null) {
            return null;
        }
        return value instanceof Number number ? number.longValue() : Long.valueOf(value.toString());
    }

    private int intValue(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value == null) {
            return 0;
        }
        return value instanceof Number number ? number.intValue() : Integer.parseInt(value.toString());
    }

    private String textValue(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value == null ? null : value.toString();
    }

    private LocalDateTime dateTimeValue(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        return null;
    }

    private record IdentidadeOperacional(
            String chaveOperacional,
            Long truckVisitId,
            Long agendamentoId,
            Long motoristaId,
            String motoristaNome,
            String documentoMotorista,
            Long transportadoraId,
            String transportadoraNome) {
    }
}
