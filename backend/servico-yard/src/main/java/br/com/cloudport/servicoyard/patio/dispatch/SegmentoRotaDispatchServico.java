package br.com.cloudport.servicoyard.patio.dispatch;

import br.com.cloudport.servicoyard.patio.dispatch.SegmentoRotaDispatchDto.Requisicao;
import br.com.cloudport.servicoyard.patio.dispatch.SegmentoRotaDispatchDto.Resposta;
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
public class SegmentoRotaDispatchServico {

    private final NamedParameterJdbcTemplate jdbc;

    public SegmentoRotaDispatchServico(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public List<Resposta> listar() {
        return jdbc.query("""
                SELECT * FROM segmento_rota_dispatch
                ORDER BY origem, destino, versao DESC
                """, Collections.emptyMap(), this::mapear);
    }

    @Transactional
    public Resposta criarVersao(Requisicao request, String usuario) {
        if (request.getVigenteAte() != null && request.getVigenteDe() != null
                && !request.getVigenteAte().isAfter(request.getVigenteDe())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A vigencia final da rota deve ser posterior a inicial.");
        }
        String origem = normalizar(request.getOrigem());
        String destino = normalizar(request.getDestino());
        Long versao = jdbc.queryForObject("""
                SELECT COALESCE(MAX(versao), 0) + 1
                FROM segmento_rota_dispatch
                WHERE origem = :origem AND destino = :destino
                """, new MapSqlParameterSource()
                .addValue("origem", origem)
                .addValue("destino", destino), Long.class);
        jdbc.update("""
                UPDATE segmento_rota_dispatch
                SET ativo = FALSE, atualizado_em = CURRENT_TIMESTAMP, atualizado_por = :usuario
                WHERE origem = :origem AND destino = :destino AND ativo = TRUE
                """, new MapSqlParameterSource()
                .addValue("origem", origem)
                .addValue("destino", destino)
                .addValue("usuario", usuario(usuario)));
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update("""
                INSERT INTO segmento_rota_dispatch (
                    origem, destino, distancia_metros, sentido,
                    congestionamento_percentual, bloqueado, motivo_interdicao,
                    limite_regional_che, ativo, versao, vigente_de, vigente_ate,
                    atualizado_por, atualizado_em
                ) VALUES (
                    :origem, :destino, :distancia, :sentido,
                    :congestionamento, :bloqueado, :motivo,
                    :limite, TRUE, :versao, :vigenteDe, :vigenteAte,
                    :usuario, CURRENT_TIMESTAMP
                )
                """, new MapSqlParameterSource()
                .addValue("origem", origem)
                .addValue("destino", destino)
                .addValue("distancia", request.getDistanciaMetros())
                .addValue("sentido", limpar(request.getSentido()))
                .addValue("congestionamento", request.getCongestionamentoPercentual() == null
                        ? 0.0 : request.getCongestionamentoPercentual())
                .addValue("bloqueado", Boolean.TRUE.equals(request.getBloqueado()))
                .addValue("motivo", limpar(request.getMotivoInterdicao()))
                .addValue("limite", request.getLimiteRegionalChe())
                .addValue("versao", versao)
                .addValue("vigenteDe", request.getVigenteDe() == null
                        ? LocalDateTime.now() : request.getVigenteDe())
                .addValue("vigenteAte", request.getVigenteAte())
                .addValue("usuario", usuario(usuario)), keyHolder, new String[]{"id"});
        return buscar(Objects.requireNonNull(keyHolder.getKey()).longValue());
    }

    private Resposta buscar(Long id) {
        return jdbc.query("SELECT * FROM segmento_rota_dispatch WHERE id = :id",
                new MapSqlParameterSource("id", id), this::mapear).stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Segmento de rota nao encontrado."));
    }

    private Resposta mapear(ResultSet rs, int rowNum) throws SQLException {
        return new Resposta(
                rs.getLong("id"),
                rs.getString("origem"),
                rs.getString("destino"),
                rs.getDouble("distancia_metros"),
                rs.getString("sentido"),
                rs.getDouble("congestionamento_percentual"),
                rs.getBoolean("bloqueado"),
                rs.getString("motivo_interdicao"),
                (Integer) rs.getObject("limite_regional_che"),
                rs.getBoolean("ativo"),
                rs.getLong("versao"),
                data(rs.getTimestamp("vigente_de")),
                data(rs.getTimestamp("vigente_ate")),
                rs.getString("atualizado_por"),
                data(rs.getTimestamp("atualizado_em")));
    }

    private LocalDateTime data(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private String normalizar(String valor) {
        if (!StringUtils.hasText(valor)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Origem e destino da rota devem ser informados.");
        }
        return valor.trim().toUpperCase(Locale.ROOT);
    }

    private String limpar(String valor) {
        return StringUtils.hasText(valor) ? valor.trim() : null;
    }

    private String usuario(String valor) {
        return StringUtils.hasText(valor) ? valor.trim() : "sistema";
    }
}
