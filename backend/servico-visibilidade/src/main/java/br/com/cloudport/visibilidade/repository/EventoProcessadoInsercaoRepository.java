package br.com.cloudport.visibilidade.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class EventoProcessadoInsercaoRepository {

    private static final String INSERT_SE_AUSENTE =
            "INSERT INTO visibilidade_evento_processado "
                    + "(identidade_evento, tipo_evento, hash_payload, processado_em) "
                    + "VALUES (?, ?, ?, CURRENT_TIMESTAMP) "
                    + "ON CONFLICT (identidade_evento) DO NOTHING";

    private final JdbcTemplate jdbcTemplate;

    public EventoProcessadoInsercaoRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int inserirSeAusente(String identidadeEvento,
                                String tipoEvento,
                                String hashPayload) {
        return jdbcTemplate.update(
                INSERT_SE_AUSENTE,
                identidadeEvento,
                tipoEvento,
                hashPayload);
    }
}
