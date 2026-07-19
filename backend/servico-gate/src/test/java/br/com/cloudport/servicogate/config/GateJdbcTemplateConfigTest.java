package br.com.cloudport.servicogate.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GateJdbcTemplateConfigTest {

    @Test
    void deveAdicionarGateAoAgrupamentoDasPistas() {
        String sql = """
                SELECT gl.id,
                       COUNT(tv.id) AS fila_atual
                  FROM gate_lane gl
                  JOIN gate_configuracao gc ON gc.id = gl.gate_id
                  LEFT JOIN truck_visit tv ON tv.lane_id = gl.id
                 WHERE gc.facility_id = :facilityId
                 GROUP BY gl.id
                 ORDER BY gc.id, gl.codigo
                """;

        String corrigido = GateJdbcTemplateConfig.GateSqlNamedParameterJdbcTemplate
                .corrigirAgrupamentoPistas(sql);

        assertThat(corrigido)
                .contains("GROUP BY gl.id, gc.id")
                .contains("ORDER BY gc.id, gl.codigo")
                .doesNotContain("GROUP BY gl.id\n");
    }

    @Test
    void naoDeveAlterarConsultasQueNaoSaoDePistas() {
        String sql = "SELECT id FROM gate_configuracao ORDER BY id";

        String corrigido = GateJdbcTemplateConfig.GateSqlNamedParameterJdbcTemplate
                .corrigirAgrupamentoPistas(sql);

        assertThat(corrigido).isEqualTo(sql);
    }
}
