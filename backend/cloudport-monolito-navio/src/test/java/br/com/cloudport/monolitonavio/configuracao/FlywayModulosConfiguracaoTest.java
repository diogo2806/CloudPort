package br.com.cloudport.monolitonavio.configuracao;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.net.URL;
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
    void deveCarregarMigracoesPublicadasPelosArtefatosDosModulos() {
        ClassLoader classLoader = getClass().getClassLoader();
        URL migracaoNavio = classLoader.getResource(
                "cloudport/migrations/navio/V1__criar_tabelas_navio.sql");
        URL migracaoSiderurgica = classLoader.getResource(
                "cloudport/migrations/navio-siderurgico/V1__criar_tabelas_navio_siderurgico.sql");

        assertNotNull(migracaoNavio);
        assertNotNull(migracaoSiderurgica);
        assertTrue(migracaoNavio.toExternalForm().contains("servico-navio"));
        assertTrue(migracaoSiderurgica.toExternalForm().contains("servico-navio-siderurgico"));
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
