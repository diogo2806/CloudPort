package br.com.cloudport.servicoyard.patio.dispatch;

import br.com.cloudport.servicoyard.patio.dispatch.DispatchDtos.Configuracao;
import br.com.cloudport.servicoyard.patio.dispatch.DispatchDtos.ConfiguracaoRequest;
import br.com.cloudport.servicoyard.patio.dispatch.DispatchEnums.StatusConfiguracao;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.WorkQueuePatio;
import br.com.cloudport.servicoyard.patio.modelo.TipoEquipamento;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
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
public class ConfiguracaoDispatchServico {

    private final NamedParameterJdbcTemplate jdbc;

    public ConfiguracaoDispatchServico(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public List<Configuracao> listar() {
        return jdbc.query("""
                SELECT *
                FROM configuracao_dispatch
                ORDER BY tipo_equipamento, tipo_escopo, valor_escopo, versao DESC
                """, Collections.emptyMap(), this::mapear);
    }

    @Transactional(readOnly = true)
    public Configuracao buscar(Long id) {
        return jdbc.query("SELECT * FROM configuracao_dispatch WHERE id = :id",
                new MapSqlParameterSource("id", id), this::mapear).stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Configuracao de dispatch nao encontrada."));
    }

    @Transactional(readOnly = true)
    public Configuracao resolver(WorkQueuePatio fila, TipoEquipamento tipoEquipamento) {
        MapSqlParameterSource parametros = new MapSqlParameterSource()
                .addValue("tipoEquipamento", tipoEquipamento.name())
                .addValue("fila", String.valueOf(fila.getId()))
                .addValue("pool", normalizar(fila.getPoolOperacional()))
                .addValue("pow", normalizar(fila.getPow()))
                .addValue("bloco", normalizar(fila.getBlocoZona()));
        return jdbc.query("""
                SELECT *
                FROM configuracao_dispatch
                WHERE tipo_equipamento = :tipoEquipamento
                  AND status = 'ATIVA'
                  AND vigente_de <= CURRENT_TIMESTAMP
                  AND (vigente_ate IS NULL OR vigente_ate > CURRENT_TIMESTAMP)
                  AND (
                    (tipo_escopo = 'FILA' AND valor_escopo = :fila)
                    OR (tipo_escopo = 'POOL' AND valor_escopo = :pool)
                    OR (tipo_escopo = 'POW' AND valor_escopo = :pow)
                    OR (tipo_escopo = 'BLOCO' AND valor_escopo = :bloco)
                    OR (tipo_escopo = 'PATIO' AND valor_escopo = 'PADRAO')
                    OR (tipo_escopo = 'TERMINAL' AND valor_escopo = 'PADRAO')
                  )
                ORDER BY CASE tipo_escopo
                    WHEN 'FILA' THEN 1
                    WHEN 'POOL' THEN 2
                    WHEN 'POW' THEN 3
                    WHEN 'BLOCO' THEN 4
                    WHEN 'PATIO' THEN 5
                    ELSE 6
                END, versao DESC
                LIMIT 1
                """, parametros, this::mapear).stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "Nao existe configuracao de dispatch ativa para a familia " + tipoEquipamento + "."));
    }

    @Transactional
    public Configuracao criar(ConfiguracaoRequest request, String usuario) {
        validar(request);
        String tipoEscopo = request.getTipoEscopo().name();
        String valorEscopo = normalizarObrigatorio(request.getValorEscopo(), "valorEscopo");
        String tipoEquipamento = request.getTipoEquipamento().name();
        Long versao = jdbc.queryForObject("""
                SELECT COALESCE(MAX(versao), 0) + 1
                FROM configuracao_dispatch
                WHERE tipo_escopo = :tipoEscopo
                  AND valor_escopo = :valorEscopo
                  AND tipo_equipamento = :tipoEquipamento
                """, new MapSqlParameterSource()
                .addValue("tipoEscopo", tipoEscopo)
                .addValue("valorEscopo", valorEscopo)
                .addValue("tipoEquipamento", tipoEquipamento), Long.class);
        MapSqlParameterSource parametros = parametros(request, usuario)
                .addValue("valorEscopo", valorEscopo)
                .addValue("versao", versao)
                .addValue("status", StatusConfiguracao.RASCUNHO.name());
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update("""
                INSERT INTO configuracao_dispatch (
                    tipo_escopo, valor_escopo, tipo_equipamento, versao, status, modo_dispatch,
                    peso_prioridade, peso_distancia, peso_atraso, peso_congestionamento,
                    velocidade_media_kmh, tempo_coleta_segundos, tempo_entrega_segundos,
                    tolerancia_telemetria_segundos, capacidade_simultanea, limite_regional_che,
                    selecionar_auxiliar, permitir_override, vigente_de, vigente_ate,
                    motivo, criado_por, criado_em
                ) VALUES (
                    :tipoEscopo, :valorEscopo, :tipoEquipamento, :versao, :status, :modo,
                    :pesoPrioridade, :pesoDistancia, :pesoAtraso, :pesoCongestionamento,
                    :velocidadeMedia, :tempoColeta, :tempoEntrega,
                    :toleranciaTelemetria, :capacidadeSimultanea, :limiteRegionalChe,
                    :selecionarAuxiliar, :permitirOverride, :vigenteDe, :vigenteAte,
                    :motivo, :usuario, CURRENT_TIMESTAMP
                )
                """, parametros, keyHolder, new String[]{"id"});
        Long id = Objects.requireNonNull(keyHolder.getKey()).longValue();
        registrarHistorico(id, "CRIADA", versao, request.getMotivo(), usuario,
                "Configuracao criada em estado RASCUNHO.");
        return buscar(id);
    }

    @Transactional
    public Configuracao ativar(Long id, String usuario) {
        Configuracao configuracao = buscar(id);
        if (StatusConfiguracao.ATIVA.name().equals(configuracao.status())) {
            return configuracao;
        }
        MapSqlParameterSource parametros = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("tipoEscopo", configuracao.tipoEscopo().name())
                .addValue("valorEscopo", configuracao.valorEscopo())
                .addValue("tipoEquipamento", configuracao.tipoEquipamento().name());
        List<Long> inativadas = jdbc.queryForList("""
                SELECT id FROM configuracao_dispatch
                WHERE tipo_escopo = :tipoEscopo
                  AND valor_escopo = :valorEscopo
                  AND tipo_equipamento = :tipoEquipamento
                  AND status = 'ATIVA'
                  AND id <> :id
                """, parametros, Long.class);
        jdbc.update("""
                UPDATE configuracao_dispatch
                SET status = 'INATIVA'
                WHERE tipo_escopo = :tipoEscopo
                  AND valor_escopo = :valorEscopo
                  AND tipo_equipamento = :tipoEquipamento
                  AND status = 'ATIVA'
                  AND id <> :id
                """, parametros);
        inativadas.forEach(configuracaoId -> registrarHistorico(configuracaoId, "INATIVADA",
                buscar(configuracaoId).versao(), "Substituida pela configuracao " + id + ".", usuario,
                "Ativacao de nova versao no mesmo escopo."));
        jdbc.update("""
                UPDATE configuracao_dispatch
                SET status = 'ATIVA', ativado_em = CURRENT_TIMESTAMP
                WHERE id = :id
                """, new MapSqlParameterSource("id", id));
        registrarHistorico(id, "ATIVADA", configuracao.versao(), configuracao.motivo(), usuario,
                "Configuracao ativada em runtime.");
        return buscar(id);
    }

    @Transactional
    public Configuracao rollback(Long id, String usuario, String motivo) {
        Configuracao origem = buscar(id);
        Long versao = jdbc.queryForObject("""
                SELECT COALESCE(MAX(versao), 0) + 1
                FROM configuracao_dispatch
                WHERE tipo_escopo = :tipoEscopo
                  AND valor_escopo = :valorEscopo
                  AND tipo_equipamento = :tipoEquipamento
                """, new MapSqlParameterSource()
                .addValue("tipoEscopo", origem.tipoEscopo().name())
                .addValue("valorEscopo", origem.valorEscopo())
                .addValue("tipoEquipamento", origem.tipoEquipamento().name()), Long.class);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update("""
                INSERT INTO configuracao_dispatch (
                    tipo_escopo, valor_escopo, tipo_equipamento, versao, status, modo_dispatch,
                    peso_prioridade, peso_distancia, peso_atraso, peso_congestionamento,
                    velocidade_media_kmh, tempo_coleta_segundos, tempo_entrega_segundos,
                    tolerancia_telemetria_segundos, capacidade_simultanea, limite_regional_che,
                    selecionar_auxiliar, permitir_override, vigente_de, vigente_ate,
                    motivo, criado_por, criado_em, rollback_configuracao_id
                )
                SELECT
                    tipo_escopo, valor_escopo, tipo_equipamento, :versao, 'RASCUNHO', modo_dispatch,
                    peso_prioridade, peso_distancia, peso_atraso, peso_congestionamento,
                    velocidade_media_kmh, tempo_coleta_segundos, tempo_entrega_segundos,
                    tolerancia_telemetria_segundos, capacidade_simultanea, limite_regional_che,
                    selecionar_auxiliar, permitir_override, CURRENT_TIMESTAMP, NULL,
                    :motivo, :usuario, CURRENT_TIMESTAMP, id
                FROM configuracao_dispatch WHERE id = :id
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("versao", versao)
                .addValue("motivo", normalizarObrigatorio(motivo, "motivo"))
                .addValue("usuario", usuario(usuario)), keyHolder, new String[]{"id"});
        Long novaId = Objects.requireNonNull(keyHolder.getKey()).longValue();
        registrarHistorico(novaId, "ROLLBACK", versao, motivo, usuario,
                "Nova versao criada a partir da configuracao " + id + ".");
        return ativar(novaId, usuario);
    }

    private MapSqlParameterSource parametros(ConfiguracaoRequest request, String usuario) {
        return new MapSqlParameterSource()
                .addValue("tipoEscopo", request.getTipoEscopo().name())
                .addValue("tipoEquipamento", request.getTipoEquipamento().name())
                .addValue("modo", request.getModo().name())
                .addValue("pesoPrioridade", valor(request.getPesoPrioridade(), 10.0))
                .addValue("pesoDistancia", valor(request.getPesoDistancia(), 1.0))
                .addValue("pesoAtraso", valor(request.getPesoAtraso(), 3.0))
                .addValue("pesoCongestionamento", valor(request.getPesoCongestionamento(), 2.0))
                .addValue("velocidadeMedia", valor(request.getVelocidadeMediaKmh(), 20.0))
                .addValue("tempoColeta", valor(request.getTempoColetaSegundos(), 60))
                .addValue("tempoEntrega", valor(request.getTempoEntregaSegundos(), 60))
                .addValue("toleranciaTelemetria", valor(request.getToleranciaTelemetriaSegundos(), 120))
                .addValue("capacidadeSimultanea", valor(request.getCapacidadeSimultanea(), 1))
                .addValue("limiteRegionalChe", valor(request.getLimiteRegionalChe(), 8))
                .addValue("selecionarAuxiliar", Boolean.TRUE.equals(request.getSelecionarAuxiliar()))
                .addValue("permitirOverride", request.getPermitirOverride() == null || request.getPermitirOverride())
                .addValue("vigenteDe", request.getVigenteDe() == null ? LocalDateTime.now() : request.getVigenteDe())
                .addValue("vigenteAte", request.getVigenteAte())
                .addValue("motivo", request.getMotivo().trim())
                .addValue("usuario", usuario(usuario));
    }

    private void validar(ConfiguracaoRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A configuracao deve ser informada.");
        }
        if (request.getVigenteAte() != null && request.getVigenteDe() != null
                && !request.getVigenteAte().isAfter(request.getVigenteDe())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A vigencia final deve ser posterior a vigencia inicial.");
        }
    }

    private void registrarHistorico(Long configuracaoId, String acao, Long versao,
                                     String motivo, String operador, String detalhes) {
        jdbc.update("""
                INSERT INTO historico_configuracao_dispatch (
                    configuracao_id, acao, versao, motivo, operador, detalhes, ocorrido_em
                ) VALUES (
                    :configuracaoId, :acao, :versao, :motivo, :operador, :detalhes, CURRENT_TIMESTAMP
                )
                """, new MapSqlParameterSource()
                .addValue("configuracaoId", configuracaoId)
                .addValue("acao", acao)
                .addValue("versao", versao)
                .addValue("motivo", StringUtils.hasText(motivo) ? motivo.trim() : "Alteracao operacional.")
                .addValue("operador", usuario(operador))
                .addValue("detalhes", detalhes));
    }

    private Configuracao mapear(ResultSet rs, int rowNum) throws SQLException {
        return new Configuracao(
                rs.getLong("id"),
                DispatchEnums.TipoEscopo.valueOf(rs.getString("tipo_escopo")),
                rs.getString("valor_escopo"),
                TipoEquipamento.valueOf(rs.getString("tipo_equipamento")),
                rs.getLong("versao"),
                rs.getString("status"),
                DispatchEnums.ModoDispatch.valueOf(rs.getString("modo_dispatch")),
                rs.getDouble("peso_prioridade"),
                rs.getDouble("peso_distancia"),
                rs.getDouble("peso_atraso"),
                rs.getDouble("peso_congestionamento"),
                rs.getDouble("velocidade_media_kmh"),
                rs.getInt("tempo_coleta_segundos"),
                rs.getInt("tempo_entrega_segundos"),
                rs.getInt("tolerancia_telemetria_segundos"),
                rs.getInt("capacidade_simultanea"),
                rs.getInt("limite_regional_che"),
                rs.getBoolean("selecionar_auxiliar"),
                rs.getBoolean("permitir_override"),
                data(rs.getTimestamp("vigente_de")),
                data(rs.getTimestamp("vigente_ate")),
                rs.getString("motivo"),
                rs.getString("criado_por"),
                data(rs.getTimestamp("criado_em")),
                data(rs.getTimestamp("ativado_em")));
    }

    private LocalDateTime data(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private String normalizar(String valor) {
        return StringUtils.hasText(valor) ? valor.trim().toUpperCase(Locale.ROOT) : "";
    }

    private String normalizarObrigatorio(String valor, String campo) {
        if (!StringUtils.hasText(valor)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, campo + " deve ser informado.");
        }
        return valor.trim().toUpperCase(Locale.ROOT);
    }

    private String usuario(String valor) {
        return StringUtils.hasText(valor) ? valor.trim() : "sistema";
    }

    private double valor(Double valor, double padrao) {
        return valor == null ? padrao : valor;
    }

    private int valor(Integer valor, int padrao) {
        return valor == null ? padrao : valor;
    }
}
