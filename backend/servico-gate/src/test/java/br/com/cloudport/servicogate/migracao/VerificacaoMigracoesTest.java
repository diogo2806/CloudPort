package br.com.cloudport.servicogate.migracao;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

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
                .withDatabaseName("cloudport")
                .withUsername("postgres")
                .withPassword("postgres")) {

            postgres.start();

            Flyway flyway = Flyway.configure()
                    .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                    .schemas("cloudport")
                    .defaultSchema("cloudport")
                    .locations("classpath:db/migration")
                    .load();

            assertDoesNotThrow(flyway::migrate);
        }
    }
}
