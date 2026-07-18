package br.com.cloudport.servicorail.migracao;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public class VerificacaoMigracoesTest {

    @Test
    void deveExecutarMigracoesFlywaySemErros() {
        Assumptions.assumeTrue(
                DockerClientFactory.instance().isDockerAvailable(),
                "Docker não está disponível para executar os testes de migração");

        DockerImageName imagemPostgres = DockerImageName.parse("postgres:14-alpine");
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(imagemPostgres)
                .withDatabaseName("servico_rail")
                .withUsername("postgres")
                .withPassword("postgres")) {

            postgres.start();

            Flyway flyway = Flyway.configure()
                    .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                    .schemas("public")
                    .defaultSchema("public")
                    .locations("classpath:db/migration")
                    .load();

            assertDoesNotThrow(() -> {
                flyway.migrate();
                verificarEstruturaFerroviaria(postgres);
            });
        }
    }

    private void verificarEstruturaFerroviaria(PostgreSQLContainer<?> postgres) throws SQLException {
        try (Connection conexao = DriverManager.getConnection(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword())) {
            assertTrue(colunaExiste(conexao, "visita_trem", "versao"));
            assertTrue(colunaExiste(conexao, "visita_trem", "posicao_ferroviaria_atual"));
            assertTrue(colunaExiste(conexao, "visita_trem_vagao", "capacidade_conteineres"));
            assertTrue(colunaExiste(conexao, "visita_trem_descarga", "identificador_vagao"));
            assertTrue(colunaExiste(conexao, "visita_trem_carga", "identificador_vagao"));
            assertTrue(colunaExiste(conexao, "ordem_movimentacao", "identificador_vagao"));
            assertTrue(colunaExiste(conexao, "ordem_movimentacao", "posicao_vagao_no_trem"));
            assertTrue(tabelaExiste(conexao, "replanejamento_conteiner_ferroviario"));
            assertTrue(tabelaExiste(conexao, "movimento_ferroviario_interno"));
            assertTrue(tabelaExiste(conexao, "reserva_recurso_ferroviario"));
        }
    }

    private boolean colunaExiste(Connection conexao, String tabela, String coluna) throws SQLException {
        String sql = "SELECT 1 FROM information_schema.columns "
                + "WHERE table_schema = 'public' AND table_name = ? AND column_name = ?";
        try (PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setString(1, tabela);
            comando.setString(2, coluna);
            try (ResultSet resultado = comando.executeQuery()) {
                return resultado.next();
            }
        }
    }

    private boolean tabelaExiste(Connection conexao, String tabela) throws SQLException {
        String sql = "SELECT 1 FROM information_schema.tables "
                + "WHERE table_schema = 'public' AND table_name = ?";
        try (PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setString(1, tabela);
            try (ResultSet resultado = comando.executeQuery()) {
                return resultado.next();
            }
        }
    }
}
