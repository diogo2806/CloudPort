package br.com.cloudport.servicogate.config;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.util.StringUtils;

@Configuration
public class GateJdbcTemplateConfig {

    @Bean
    @Primary
    public NamedParameterJdbcTemplate gateNamedParameterJdbcTemplate(JdbcOperations jdbcOperations) {
        return new GateSqlNamedParameterJdbcTemplate(jdbcOperations);
    }

    static final class GateSqlNamedParameterJdbcTemplate extends NamedParameterJdbcTemplate {

        private static final Pattern AGRUPAMENTO_PISTAS_INCOMPLETO = Pattern.compile(
                "GROUP\\s+BY\\s+gl\\.id\\s+ORDER\\s+BY\\s+gc\\.id\\s*,\\s*gl\\.codigo",
                Pattern.CASE_INSENSITIVE);

        GateSqlNamedParameterJdbcTemplate(JdbcOperations jdbcOperations) {
            super(jdbcOperations);
        }

        @Override
        public List<Map<String, Object>> queryForList(String sql, SqlParameterSource parameterSource) {
            return super.queryForList(corrigirAgrupamentoPistas(sql), parameterSource);
        }

        static String corrigirAgrupamentoPistas(String sql) {
            if (!StringUtils.hasText(sql)
                    || !sql.contains("FROM gate_lane gl")
                    || !sql.contains("JOIN gate_configuracao gc")) {
                return sql;
            }
            return AGRUPAMENTO_PISTAS_INCOMPLETO.matcher(sql)
                    .replaceFirst("GROUP BY gl.id, gc.id ORDER BY gc.id, gl.codigo");
        }
    }
}
