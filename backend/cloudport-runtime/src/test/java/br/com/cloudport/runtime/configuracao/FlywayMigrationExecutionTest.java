package br.com.cloudport.runtime.configuracao;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

class FlywayMigrationExecutionTest {

    private static final Path MIGRATIONS_ROOT = Path.of(
            "target", "classes", "cloudport", "migrations");
    private static final Path REPORT_PATH = Path.of("target", "flyway-audit-report.txt");
    private static final Pattern VERSIONED_MIGRATION = Pattern.compile("^V([^_]+)__.+\\.sql$");

    @Test
    void deveAuditarTodasAsMigrationsEmPostgresqlLimpo() throws IOException {
        assertThat(MIGRATIONS_ROOT).isDirectory();

        List<Path> moduleDirectories = listModuleDirectories();
        assertThat(moduleDirectories)
                .as("Modulos com migrations empacotadas")
                .isNotEmpty();

        List<String> report = new ArrayList<>();
        List<String> failures = new ArrayList<>();
        Set<String> modulesWithDuplicateVersions = auditInventory(
                moduleDirectories,
                report,
                failures);

        try (PostgreSQLContainer<?> postgresql = new PostgreSQLContainer<>("postgres:14-alpine")) {
            postgresql.start();

            for (Path moduleDirectory : moduleDirectories) {
                String module = moduleDirectory.getFileName().toString();
                if (modulesWithDuplicateVersions.contains(module)) {
                    report.add("EXECUCAO IGNORADA " + module + ": versoes duplicadas");
                    continue;
                }
                validateModule(postgresql, moduleDirectory, report, failures);
            }
        } catch (Exception exception) {
            failures.add("postgresql: " + rootMessage(exception));
        }

        report.add("");
        report.add("RESULTADO");
        if (failures.isEmpty()) {
            report.add("SUCESSO: todas as migrations foram validadas.");
        } else {
            report.addAll(failures.stream()
                    .map(failure -> "FALHA: " + failure)
                    .collect(Collectors.toList()));
        }

        Files.write(REPORT_PATH, report, UTF_8);

        assertThat(failures)
                .as("Falhas na auditoria integral das migrations. Relatorio: " + REPORT_PATH)
                .isEmpty();
    }

    private static List<Path> listModuleDirectories() throws IOException {
        try (Stream<Path> directories = Files.list(MIGRATIONS_ROOT)) {
            return directories
                    .filter(Files::isDirectory)
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .collect(Collectors.toList());
        }
    }

    private static Set<String> auditInventory(
            List<Path> moduleDirectories,
            List<String> report,
            List<String> failures) throws IOException {
        Set<String> modulesWithDuplicateVersions = new HashSet<>();
        report.add("AUDITORIA INTEGRAL DE MIGRATIONS FLYWAY");
        report.add("");

        for (Path moduleDirectory : moduleDirectories) {
            String module = moduleDirectory.getFileName().toString();
            Path migrationDirectory = moduleDirectory.resolve("db").resolve("migration");
            report.add("MODULO " + module);

            if (!Files.isDirectory(migrationDirectory)) {
                failures.add(module + ": diretorio db/migration nao encontrado");
                report.add("  DIRETORIO AUSENTE");
                report.add("");
                continue;
            }

            List<Path> migrationFiles;
            try (Stream<Path> files = Files.list(migrationDirectory)) {
                migrationFiles = files
                        .filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().endsWith(".sql"))
                        .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                        .collect(Collectors.toList());
            }

            Map<String, List<String>> filesByVersion = new TreeMap<>();
            for (Path migrationFile : migrationFiles) {
                String fileName = migrationFile.getFileName().toString();
                report.add("  " + fileName);
                Matcher matcher = VERSIONED_MIGRATION.matcher(fileName);
                if (matcher.matches()) {
                    filesByVersion
                            .computeIfAbsent(matcher.group(1), ignored -> new ArrayList<>())
                            .add(fileName);
                }
            }

            filesByVersion.forEach((version, files) -> {
                if (files.size() > 1) {
                    modulesWithDuplicateVersions.add(module);
                    failures.add(module + ": versao V" + version + " duplicada: "
                            + String.join(", ", files));
                }
            });
            report.add("");
        }

        return modulesWithDuplicateVersions;
    }

    private static void validateModule(
            PostgreSQLContainer<?> postgresql,
            Path moduleDirectory,
            List<String> report,
            List<String> failures) {
        String module = moduleDirectory.getFileName().toString();
        Path migrationDirectory = moduleDirectory.resolve("db").resolve("migration");
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
            report.add("EXECUCAO SUCESSO " + module);
        } catch (Exception exception) {
            String failure = module + ": " + rootMessage(exception);
            failures.add(failure);
            report.add("EXECUCAO FALHA " + failure);
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
