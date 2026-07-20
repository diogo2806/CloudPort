package br.com.cloudport.servicoyard.patio.dispatch;

import br.com.cloudport.servicoyard.patio.dispatch.DispatchDtos.Etapa;
import br.com.cloudport.servicoyard.patio.dispatch.DispatchDtos.EtapaRequest;
import br.com.cloudport.servicoyard.patio.dispatch.DispatchEnums.StatusEtapa;
import br.com.cloudport.servicoyard.patio.dispatch.DispatchEnums.TipoEtapa;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.OrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.StatusConfirmacaoVmt;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EtapaWorkInstructionServico {

    private static final List<TipoEtapa> ORDEM_ETAPAS = List.of(
            TipoEtapa.DESLOCAMENTO_ORIGEM,
            TipoEtapa.CHEGADA_ORIGEM,
            TipoEtapa.COLETA,
            TipoEtapa.TRANSPORTE,
            TipoEtapa.ENTREGA,
            TipoEtapa.CONFIRMACAO_FISICA);

    private final NamedParameterJdbcTemplate jdbc;

    public EtapaWorkInstructionServico(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public List<Etapa> inicializar(Long ordemId, Long decisaoId, String operador) {
        for (int indice = 0; indice < ORDEM_ETAPAS.size(); indice++) {
            TipoEtapa tipo = ORDEM_ETAPAS.get(indice);
            jdbc.update("""
                    INSERT INTO etapa_work_instruction (
                        ordem_trabalho_patio_id, decisao_dispatch_id, tipo_etapa,
                        ordem_etapa, status, atualizado_em
                    ) VALUES (
                        :ordemId, :decisaoId, :tipoEtapa, :ordemEtapa, 'PENDENTE', CURRENT_TIMESTAMP
                    )
                    ON CONFLICT (ordem_trabalho_patio_id, tipo_etapa) DO UPDATE SET
                        decisao_dispatch_id = EXCLUDED.decisao_dispatch_id,
                        atualizado_em = CURRENT_TIMESTAMP
                    """, new MapSqlParameterSource()
                    .addValue("ordemId", ordemId)
                    .addValue("decisaoId", decisaoId)
                    .addValue("tipoEtapa", tipo.name())
                    .addValue("ordemEtapa", indice + 1));
        }
        iniciarPrimeiraPendente(ordemId, operador, "dispatch-inicial");
        return listar(ordemId);
    }

    @Transactional(readOnly = true)
    public List<Etapa> listar(Long ordemId) {
        return jdbc.query("""
                SELECT *
                FROM etapa_work_instruction
                WHERE ordem_trabalho_patio_id = :ordemId
                ORDER BY ordem_etapa
                """, new MapSqlParameterSource("ordemId", ordemId), this::mapear);
    }

    @Transactional
    public Etapa avancar(Long ordemId, TipoEtapa tipo, EtapaRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O comando da etapa deve ser informado.");
        }
        Etapa etapa = buscar(ordemId, tipo);
        buscarPorChave(request.getChaveIdempotencia()).ifPresent(existente -> {
            if (!Objects.equals(existente.id(), etapa.id())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "A chave idempotente ja foi usada em outra etapa.");
            }
        });
        validarTransicao(etapa, request.getStatusDestino());
        if (request.getStatusDestino() == StatusEtapa.EM_EXECUCAO) {
            validarAnterioresConcluidas(ordemId, etapa.ordem());
        }
        LocalDateTime agora = LocalDateTime.now();
        jdbc.update("""
                UPDATE etapa_work_instruction
                SET status = :status,
                    iniciado_em = CASE
                        WHEN :status = 'EM_EXECUCAO' AND iniciado_em IS NULL THEN :agora
                        ELSE iniciado_em
                    END,
                    concluido_em = CASE
                        WHEN :status IN ('CONCLUIDA', 'FALHA', 'IGNORADA') THEN :agora
                        ELSE concluido_em
                    END,
                    operador = :operador,
                    evidencia = :evidencia,
                    chave_idempotencia = :chave,
                    atualizado_em = :agora
                WHERE id = :id
                """, new MapSqlParameterSource()
                .addValue("status", request.getStatusDestino().name())
                .addValue("agora", agora)
                .addValue("operador", limpar(request.getOperador()))
                .addValue("evidencia", limpar(request.getEvidencia()))
                .addValue("chave", limpar(request.getChaveIdempotencia()))
                .addValue("id", etapa.id()));
        if (request.getStatusDestino() == StatusEtapa.CONCLUIDA) {
            iniciarProxima(ordemId, etapa.ordem(), request.getOperador(), request.getChaveIdempotencia());
        }
        return buscar(ordemId, tipo);
    }

    @Transactional
    public List<Etapa> sincronizarComVmt(OrdemTrabalhoPatio ordem) {
        if (listar(ordem.getId()).isEmpty()) {
            return List.of();
        }
        StatusConfirmacaoVmt status = ordem.getStatusConfirmacaoVmt() == null
                ? StatusConfirmacaoVmt.PENDENTE : ordem.getStatusConfirmacaoVmt();
        switch (status) {
            case ACEITA -> atualizarPorOrdem(ordem.getId(), 1, StatusEtapa.EM_EXECUCAO,
                    ordem.getVmtAceitoEm(), "integracao-vmt", "VMT aceite");
            case EM_EXECUCAO -> {
                concluirAte(ordem.getId(), 3, ordem.getVmtIniciadoEm(), "integracao-vmt", "VMT inicio");
                atualizarPorOrdem(ordem.getId(), 4, StatusEtapa.EM_EXECUCAO,
                        ordem.getVmtIniciadoEm(), "integracao-vmt", "Transporte iniciado");
            }
            case CONCLUIDA -> concluirAte(ordem.getId(), 6, ordem.getVmtConcluidoEm(),
                    "integracao-vmt", "VMT conclusao e confirmacao fisica");
            case FALHA -> falharAtual(ordem.getId(), ordem.getVmtFalhaEm(), ordem.getResultadoVmt());
            default -> {
            }
        }
        return listar(ordem.getId());
    }

    private void concluirAte(Long ordemId, int ordemLimite, LocalDateTime momento,
                             String operador, String evidencia) {
        jdbc.update("""
                UPDATE etapa_work_instruction
                SET status = 'CONCLUIDA',
                    iniciado_em = COALESCE(iniciado_em, :momento),
                    concluido_em = COALESCE(concluido_em, :momento),
                    operador = COALESCE(operador, :operador),
                    evidencia = COALESCE(evidencia, :evidencia),
                    atualizado_em = CURRENT_TIMESTAMP
                WHERE ordem_trabalho_patio_id = :ordemId
                  AND ordem_etapa <= :ordemLimite
                  AND status NOT IN ('CONCLUIDA', 'IGNORADA')
                """, parametrosSincronizacao(ordemId, momento, operador, evidencia)
                .addValue("ordemLimite", ordemLimite));
    }

    private void atualizarPorOrdem(Long ordemId, int ordemEtapa, StatusEtapa status,
                                    LocalDateTime momento, String operador, String evidencia) {
        jdbc.update("""
                UPDATE etapa_work_instruction
                SET status = :status,
                    iniciado_em = COALESCE(iniciado_em, :momento),
                    concluido_em = CASE
                        WHEN :status IN ('CONCLUIDA', 'FALHA', 'IGNORADA')
                            THEN COALESCE(concluido_em, :momento)
                        ELSE concluido_em
                    END,
                    operador = COALESCE(operador, :operador),
                    evidencia = COALESCE(evidencia, :evidencia),
                    atualizado_em = CURRENT_TIMESTAMP
                WHERE ordem_trabalho_patio_id = :ordemId
                  AND ordem_etapa = :ordemEtapa
                  AND status NOT IN ('CONCLUIDA', 'IGNORADA')
                """, parametrosSincronizacao(ordemId, momento, operador, evidencia)
                .addValue("status", status.name())
                .addValue("ordemEtapa", ordemEtapa));
    }

    private void falharAtual(Long ordemId, LocalDateTime momento, String evidencia) {
        int atual = jdbc.queryForObject("""
                SELECT COALESCE(MIN(ordem_etapa), 1)
                FROM etapa_work_instruction
                WHERE ordem_trabalho_patio_id = :ordemId
                  AND status IN ('EM_EXECUCAO', 'PENDENTE')
                """, new MapSqlParameterSource("ordemId", ordemId), Integer.class);
        atualizarPorOrdem(ordemId, atual, StatusEtapa.FALHA, momento,
                "integracao-vmt", StringUtils.hasText(evidencia) ? evidencia : "Falha VMT");
    }

    private MapSqlParameterSource parametrosSincronizacao(Long ordemId, LocalDateTime momento,
                                                           String operador, String evidencia) {
        return new MapSqlParameterSource()
                .addValue("ordemId", ordemId)
                .addValue("momento", momento == null ? LocalDateTime.now() : momento)
                .addValue("operador", operador)
                .addValue("evidencia", evidencia);
    }

    private void iniciarPrimeiraPendente(Long ordemId, String operador, String chave) {
        jdbc.update("""
                UPDATE etapa_work_instruction
                SET status = 'EM_EXECUCAO', iniciado_em = CURRENT_TIMESTAMP,
                    operador = :operador, chave_idempotencia = :chave,
                    atualizado_em = CURRENT_TIMESTAMP
                WHERE ordem_trabalho_patio_id = :ordemId
                  AND ordem_etapa = 1
                  AND status = 'PENDENTE'
                """, new MapSqlParameterSource()
                .addValue("ordemId", ordemId)
                .addValue("operador", limpar(operador))
                .addValue("chave", chave + "-" + ordemId));
    }

    private void iniciarProxima(Long ordemId, int ordemAtual, String operador, String chaveBase) {
        jdbc.update("""
                UPDATE etapa_work_instruction
                SET status = 'EM_EXECUCAO', iniciado_em = CURRENT_TIMESTAMP,
                    operador = :operador, chave_idempotencia = :chave,
                    atualizado_em = CURRENT_TIMESTAMP
                WHERE ordem_trabalho_patio_id = :ordemId
                  AND ordem_etapa = :proxima
                  AND status = 'PENDENTE'
                """, new MapSqlParameterSource()
                .addValue("ordemId", ordemId)
                .addValue("proxima", ordemAtual + 1)
                .addValue("operador", limpar(operador))
                .addValue("chave", limpar(chaveBase) + "-NEXT-" + (ordemAtual + 1)));
    }

    private void validarAnterioresConcluidas(Long ordemId, int ordemEtapa) {
        Integer pendentes = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM etapa_work_instruction
                WHERE ordem_trabalho_patio_id = :ordemId
                  AND ordem_etapa < :ordemEtapa
                  AND status NOT IN ('CONCLUIDA', 'IGNORADA')
                """, new MapSqlParameterSource()
                .addValue("ordemId", ordemId)
                .addValue("ordemEtapa", ordemEtapa), Integer.class);
        if (pendentes != null && pendentes > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A etapa nao pode iniciar antes da conclusao das etapas anteriores.");
        }
    }

    private void validarTransicao(Etapa etapa, StatusEtapa destino) {
        boolean permitida = switch (etapa.status()) {
            case PENDENTE -> destino == StatusEtapa.EM_EXECUCAO
                    || destino == StatusEtapa.IGNORADA;
            case EM_EXECUCAO -> destino == StatusEtapa.CONCLUIDA
                    || destino == StatusEtapa.FALHA;
            case FALHA -> destino == StatusEtapa.EM_EXECUCAO;
            case CONCLUIDA, IGNORADA -> false;
        };
        if (!permitida) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Transicao de etapa de " + etapa.status() + " para " + destino + " nao permitida.");
        }
    }

    private Etapa buscar(Long ordemId, TipoEtapa tipo) {
        return jdbc.query("""
                SELECT * FROM etapa_work_instruction
                WHERE ordem_trabalho_patio_id = :ordemId AND tipo_etapa = :tipo
                """, new MapSqlParameterSource()
                .addValue("ordemId", ordemId)
                .addValue("tipo", tipo.name()), this::mapear).stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Etapa da work instruction nao encontrada."));
    }

    private java.util.Optional<Etapa> buscarPorChave(String chave) {
        if (!StringUtils.hasText(chave)) {
            return java.util.Optional.empty();
        }
        return jdbc.query("""
                SELECT * FROM etapa_work_instruction WHERE chave_idempotencia = :chave
                """, new MapSqlParameterSource("chave", chave.trim()), this::mapear)
                .stream().findFirst();
    }

    private Etapa mapear(ResultSet rs, int rowNum) throws SQLException {
        return new Etapa(
                rs.getLong("id"),
                rs.getLong("ordem_trabalho_patio_id"),
                TipoEtapa.valueOf(rs.getString("tipo_etapa")),
                rs.getInt("ordem_etapa"),
                StatusEtapa.valueOf(rs.getString("status")),
                data(rs.getTimestamp("iniciado_em")),
                data(rs.getTimestamp("concluido_em")),
                rs.getString("operador"),
                rs.getString("evidencia"),
                rs.getString("chave_idempotencia"));
    }

    private LocalDateTime data(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private String limpar(String valor) {
        return StringUtils.hasText(valor) ? valor.trim() : null;
    }
}
