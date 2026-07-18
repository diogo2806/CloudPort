package br.com.cloudport.runtime.configuracao;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

class FlywayMigrationExecutionTest {

    private static final Path MIGRATIONS_ROOT = Path.of(
            "target", "classes", "cloudport", "migrations");

    @Test
    void deveExecutarTodasAsMigrationsEmPostgresqlLimpo() throws IOException {
        assertThat(MIGRATIONS_ROOT).isDirectory();

        List<Path> moduleDirectories;
        try (Stream<Path> directories = Files.list(MIGRATIONS_ROOT)) {
            moduleDirectories = directories
                    .filter(Files::isDirectory)
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .collect(Collectors.toList());
        }

        assertThat(moduleDirectories)
                .as("Modulos com migrations empacotadas")
                .isNotEmpty();

        List<String> failures = new ArrayList<>();

        try (PostgreSQLContainer<?> postgresql = new PostgreSQLContainer<>("postgres:14-alpine")) {
            postgresql.start();

            for (Path moduleDirectory : moduleDirectories) {
                validateModule(postgresql, moduleDirectory, failures);
            }
        }

        assertThat(failures)
                .as("Falhas ao executar migrations por modulo")
                .isEmpty();
    }

    private static void validateModule(
            PostgreSQLContainer<?> postgresql,
            Path moduleDirectory,
            List<String> failures) {
        String module = moduleDirectory.getFileName().toString();
        Path migrationDirectory = moduleDirectory.resolve("db").resolve("migration");

        if (!Files.isDirectory(migrationDirectory)) {
            failures.add(module + ": diretorio db/migration nao encontrado");
            return;
        }

        String schema = "audit_" + module.replace('-', '_');

        try {
            Flyway flyway = Flyway.configure()
                    .dataSource(
                            postgresql.getJdbcUrl(),
                            postgresql.getUsername(),
                            postgresql.getPassword())
                    .locations("filesystem:" + migrationDirectory.toAbsolutePath())
                    .schemas(schema)
                    .defaultSchema(schema)
                    .createSchemas(true)
                    .validateOnMigrate(true)
                    .load();

            flyway.migrate();
            flyway.validate();
        } catch (Exception exception) {
            failures.add(module + ": " + rootMessage(exception));
        }
    }

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getClass().getSimpleName() + ": " + current.getMessage();
    }
}
