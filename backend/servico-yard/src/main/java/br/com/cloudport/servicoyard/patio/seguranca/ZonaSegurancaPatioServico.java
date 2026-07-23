package br.com.cloudport.servicoyard.patio.seguranca;

import br.com.cloudport.servicoyard.patio.seguranca.ZonaSegurancaPatioDtos.Criar;
import br.com.cloudport.servicoyard.patio.seguranca.ZonaSegurancaPatioDtos.Liberar;
import br.com.cloudport.servicoyard.patio.seguranca.ZonaSegurancaPatioDtos.Prorrogar;
import br.com.cloudport.servicoyard.patio.seguranca.ZonaSegurancaPatioDtos.Resposta;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ZonaSegurancaPatioServico {

    private final NamedParameterJdbcTemplate jdbc;

    public ZonaSegurancaPatioServico(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public List<Resposta> listar() {
        expirarVencidas();
        return jdbc.query(sqlBase() + " ORDER BY z.criado_em DESC, z.id DESC", new MapSqlParameterSource(), this::mapear);
    }

    @Transactional
    public Resposta criar(Criar dto) {
        validarPeriodo(dto.inicio(), dto.fim());
        KeyHolder keyHolder = new GeneratedKeyHolder();
        try {
            jdbc.update("""
                    INSERT INTO zona_seguranca_patio (
                        chave_idempotencia, nome, geometria, posicoes, inicio, fim,
                        responsavel, equipe, motivo, estado, bloqueia_origem,
                        bloqueia_destino, bloqueia_rota
                    ) VALUES (
                        :chave, :nome, :geometria, :posicoes, :inicio, :fim,
                        :responsavel, :equipe, :motivo, 'RASCUNHO', :bloqueiaOrigem,
                        :bloqueiaDestino, :bloqueiaRota
                    )
                    """, parametros(dto), keyHolder, new String[]{"id"});
        } catch (DuplicateKeyException excecao) {
            return buscarPorChave(dto.chaveIdempotencia());
        }
        Long id = Objects.requireNonNull(keyHolder.getKey()).longValue();
        registrarEvento(id, 1L, "CRIADA", dto.operador(), dto.correlationId(), dto.motivo());
        return buscar(id);
    }

    @Transactional
    public Resposta ativar(Long id, String operador, String correlationId) {
        Resposta atual = buscar(id);
        if ("ATIVA".equals(atual.estado())) {
            return atual;
        }
        if ("LIBERADA".equals(atual.estado()) || atual.fim().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A zona não pode ser ativada no estado atual.");
        }
        long versao = atual.versao() + 1;
        jdbc.update("""
                UPDATE zona_seguranca_patio
                   SET estado = 'ATIVA', ativada_em = COALESCE(ativada_em, CURRENT_TIMESTAMP),
                       versao = :versao, atualizado_em = CURRENT_TIMESTAMP
                 WHERE id = :id
                """, new MapSqlParameterSource().addValue("id", id).addValue("versao", versao));
        reavaliarOrdens(id);
        registrarEvento(id, versao, "ATIVADA", operador, correlationId, atual.motivo());
        return buscar(id);
    }

    @Transactional
    public Resposta prorrogar(Long id, Prorrogar dto) {
        Resposta atual = buscar(id);
        if (!"ATIVA".equals(atual.estado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Somente zonas ativas podem ser prorrogadas.");
        }
        if (!dto.novoFim().isAfter(atual.fim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O novo fim deve ser posterior ao fim atual.");
        }
        long versao = atual.versao() + 1;
        jdbc.update("""
                UPDATE zona_seguranca_patio
                   SET fim = :fim, motivo = :motivo, versao = :versao, atualizado_em = CURRENT_TIMESTAMP
                 WHERE id = :id
                """, new MapSqlParameterSource().addValue("id", id).addValue("fim", dto.novoFim())
                .addValue("motivo", dto.motivo().trim()).addValue("versao", versao));
        registrarEvento(id, versao, "PRORROGADA", dto.operador(), dto.correlationId(), dto.motivo());
        return buscar(id);
    }

    @Transactional
    public Resposta liberar(Long id, Liberar dto) {
        Resposta atual = buscar(id);
        if ("LIBERADA".equals(atual.estado())) {
            return atual;
        }
        long versao = atual.versao() + 1;
        jdbc.update("""
                UPDATE zona_seguranca_patio
                   SET estado = 'LIBERADA', liberada_em = CURRENT_TIMESTAMP,
                       liberada_por = :operador, motivo_liberacao = :motivo,
                       versao = :versao, atualizado_em = CURRENT_TIMESTAMP
                 WHERE id = :id
                """, new MapSqlParameterSource().addValue("id", id)
                .addValue("operador", dto.operador().trim()).addValue("motivo", dto.motivo().trim())
                .addValue("versao", versao));
        jdbc.update("""
                UPDATE zona_seguranca_patio_conflito
                   SET estado = 'RESOLVIDO', resolvido_em = CURRENT_TIMESTAMP
                 WHERE zona_id = :id AND estado = 'ABERTO'
                """, new MapSqlParameterSource("id", id));
        registrarEvento(id, versao, "LIBERADA", dto.operador(), dto.correlationId(), dto.motivo());
        return buscar(id);
    }

    @Transactional(readOnly = true)
    public void validarMovimento(String origem, String destino, List<String> rota) {
        List<String> pontos = new java.util.ArrayList<>();
        if (origem != null) pontos.add(origem);
        if (destino != null) pontos.add(destino);
        if (rota != null) pontos.addAll(rota);
        for (String ponto : pontos) {
            Integer quantidade = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM zona_seguranca_patio z
                     WHERE z.estado = 'ATIVA'
                       AND CURRENT_TIMESTAMP BETWEEN z.inicio AND z.fim
                       AND ('|' || UPPER(z.posicoes) || '|') LIKE ('%|' || UPPER(:ponto) || '|%')
                    """, new MapSqlParameterSource("ponto", normalizar(ponto)), Integer.class);
            if (quantidade != null && quantidade > 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Movimento bloqueado por zona temporária de segurança ativa na posição " + ponto + ".");
            }
        }
    }

    private void reavaliarOrdens(Long zonaId) {
        jdbc.update("""
                INSERT INTO zona_seguranca_patio_conflito (zona_id, ordem_trabalho_patio_id, posicao)
                SELECT z.id, o.id, o.destino
                  FROM zona_seguranca_patio z
                  JOIN ordem_trabalho_patio o
                    ON ('|' || UPPER(z.posicoes) || '|') LIKE ('%|' || UPPER(o.destino) || '|%')
                 WHERE z.id = :id
                   AND o.status_ordem IN ('PENDENTE','SUSPENSA','EM_EXECUCAO')
                ON CONFLICT (zona_id, ordem_trabalho_patio_id, posicao) DO NOTHING
                """, new MapSqlParameterSource("id", zonaId));
        jdbc.update("""
                UPDATE ordem_trabalho_patio o
                   SET status_ordem = 'BLOQUEADA', atualizado_em = CURRENT_TIMESTAMP
                  FROM zona_seguranca_patio z
                 WHERE z.id = :id
                   AND ('|' || UPPER(z.posicoes) || '|') LIKE ('%|' || UPPER(o.destino) || '|%')
                   AND o.status_ordem IN ('PENDENTE','SUSPENSA')
                """, new MapSqlParameterSource("id", zonaId));
    }

    private void expirarVencidas() {
        jdbc.update("""
                UPDATE zona_seguranca_patio
                   SET estado = 'EXPIRADA', atualizado_em = CURRENT_TIMESTAMP, versao = versao + 1
                 WHERE estado = 'ATIVA' AND fim < CURRENT_TIMESTAMP
                """, new MapSqlParameterSource());
    }

    private Resposta buscar(Long id) {
        return jdbc.query(sqlBase() + " WHERE z.id = :id", new MapSqlParameterSource("id", id), this::mapear)
                .stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Zona de segurança não encontrada."));
    }

    private Resposta buscarPorChave(String chave) {
        return jdbc.query(sqlBase() + " WHERE z.chave_idempotencia = :chave",
                        new MapSqlParameterSource("chave", chave.trim()), this::mapear)
                .stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Zona de segurança não encontrada."));
    }

    private String sqlBase() {
        return """
                SELECT z.*,
                       (SELECT COUNT(*) FROM zona_seguranca_patio_conflito c
                         WHERE c.zona_id = z.id AND c.estado = 'ABERTO') AS conflitos_ativos
                  FROM zona_seguranca_patio z
                """;
    }

    private MapSqlParameterSource parametros(Criar dto) {
        return new MapSqlParameterSource()
                .addValue("chave", dto.chaveIdempotencia().trim())
                .addValue("nome", dto.nome().trim())
                .addValue("geometria", dto.geometria())
                .addValue("posicoes", serializar(dto.posicoes()))
                .addValue("inicio", dto.inicio()).addValue("fim", dto.fim())
                .addValue("responsavel", dto.responsavel().trim())
                .addValue("equipe", String.join("|", dto.equipe()))
                .addValue("motivo", dto.motivo().trim())
                .addValue("bloqueiaOrigem", dto.bloqueiaOrigem() == null || dto.bloqueiaOrigem())
                .addValue("bloqueiaDestino", dto.bloqueiaDestino() == null || dto.bloqueiaDestino())
                .addValue("bloqueiaRota", dto.bloqueiaRota() == null || dto.bloqueiaRota());
    }

    private Resposta mapear(ResultSet rs, int rowNum) throws SQLException {
        return new Resposta(rs.getLong("id"), rs.getString("chave_idempotencia"), rs.getString("nome"),
                rs.getString("geometria"), desserializar(rs.getString("posicoes")), data(rs.getTimestamp("inicio")),
                data(rs.getTimestamp("fim")), rs.getString("responsavel"), desserializar(rs.getString("equipe")),
                rs.getString("motivo"), rs.getString("estado"), rs.getBoolean("bloqueia_origem"),
                rs.getBoolean("bloqueia_destino"), rs.getBoolean("bloqueia_rota"), rs.getLong("versao"),
                rs.getInt("conflitos_ativos"), data(rs.getTimestamp("ativada_em")), data(rs.getTimestamp("liberada_em")),
                rs.getString("liberada_por"), rs.getString("motivo_liberacao"));
    }

    private void registrarEvento(Long id, long versao, String tipo, String operador, String correlationId, String payload) {
        jdbc.update("""
                INSERT INTO zona_seguranca_patio_evento
                    (zona_id, versao, tipo, payload, operador, correlation_id)
                VALUES (:id, :versao, :tipo, :payload, :operador, :correlationId)
                ON CONFLICT (zona_id, versao) DO NOTHING
                """, new MapSqlParameterSource().addValue("id", id).addValue("versao", versao)
                .addValue("tipo", tipo).addValue("payload", payload)
                .addValue("operador", operador == null || operador.isBlank() ? "sistema" : operador.trim())
                .addValue("correlationId", correlationId));
    }

    private void validarPeriodo(LocalDateTime inicio, LocalDateTime fim) {
        if (!fim.isAfter(inicio)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O fim deve ser posterior ao início.");
        }
    }

    private String serializar(List<String> valores) {
        return valores.stream().map(this::normalizar).distinct().reduce((a, b) -> a + "|" + b).orElse("");
    }

    private List<String> desserializar(String valor) {
        return valor == null || valor.isBlank() ? List.of() : Arrays.stream(valor.split("\\|"))
                .filter(item -> !item.isBlank()).toList();
    }

    private String normalizar(String valor) {
        return valor.trim().toUpperCase(Locale.ROOT);
    }

    private LocalDateTime data(Timestamp valor) {
        return valor == null ? null : valor.toLocalDateTime();
    }
}