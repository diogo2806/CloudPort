package br.com.cloudport.monolitonavio.configuracao;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

class FlywayModulosConfiguracaoTest {

    @Test
    void deveConfigurarHistoricosSeparadosPorSchema() {
        FlywayModulosConfiguracao configuracao = new FlywayModulosConfiguracao(
                mock(DataSource.class),
                "cloudport_navio",
                "cloudport_siderurgico");

        Flyway flywayNavio = configuracao.flywayNavio();
        Flyway flywaySiderurgico = configuracao.flywayNavioSiderurgico();

        assertEquals("cloudport_navio", flywayNavio.getConfiguration().getDefaultSchema());
        assertArrayEquals(new String[]{"cloudport_navio"}, flywayNavio.getConfiguration().getSchemas());
        assertEquals("cloudport_siderurgico", flywaySiderurgico.getConfiguration().getDefaultSchema());
        assertArrayEquals(new String[]{"cloudport_siderurgico"}, flywaySiderurgico.getConfiguration().getSchemas());
    }

    @Test
    void deveEmpacotarMigracoesDosDoisModulos() {
        ClassLoader classLoader = getClass().getClassLoader();

        assertNotNull(classLoader.getResource("db/migration/navio/V1__criar_tabelas_navio.sql"));
        assertNotNull(classLoader.getResource(
                "db/migration/navio-siderurgico/V1__criar_tabelas_navio_siderurgico.sql"));
    }

    @Test
    void deveRecusarNomeDeSchemaInseguro() {
        DataSource dataSource = mock(DataSource.class);

        assertThrows(IllegalArgumentException.class, () -> new FlywayModulosConfiguracao(
                dataSource,
                "cloudport_navio;drop schema public",
                "cloudport_siderurgico"));
    }
}
