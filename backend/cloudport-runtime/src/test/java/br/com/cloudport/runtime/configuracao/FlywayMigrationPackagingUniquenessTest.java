package br.com.cloudport.runtime.configuracao;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class FlywayMigrationPackagingUniquenessTest {

    private static final Pattern MIGRATION_FILENAME_PATTERN = Pattern.compile("^V([^_]+)__.+\\.sql$");

    @Test
    void deveEmpacotarVersoesFlywayUnicasPorModulo() throws IOException {
        Path migrationsRoot = Path.of("target", "classes", "cloudport", "migrations");
        assertThat(migrationsRoot).isDirectory();

        Map<String, List<String>> migrationsByModuleAndVersion = new TreeMap<>();

        try (Stream<Path> migrationFiles = Files.walk(migrationsRoot)) {
            migrationFiles
                    .filter(Files::isRegularFile)
                    .forEach(path -> adicionarMigrationPorModuloEVersao(
                            migrationsRoot,
                            migrationsByModuleAndVersion,
                            path));
        }

        List<String> duplicatedVersions = migrationsByModuleAndVersion.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .map(entry -> entry.getKey() + "=" + String.join(", ", entry.getValue()))
                .collect(Collectors.toList());

        assertThat(duplicatedVersions)
                .as("Versoes Flyway duplicadas no artefato do runtime")
                .isEmpty();
    }

    private static void adicionarMigrationPorModuloEVersao(
            Path migrationsRoot,
            Map<String, List<String>> migrationsByModuleAndVersion,
            Path migrationPath) {
        String fileName = migrationPath.getFileName().toString();
        Matcher matcher = MIGRATION_FILENAME_PATTERN.matcher(fileName);
        if (!matcher.matches()) {
            return;
        }

        Path relativePath = migrationsRoot.relativize(migrationPath);
        if (relativePath.getNameCount() < 4) {
            return;
        }

        String module = relativePath.getName(0).toString();
        String moduleAndVersion = module + ":" + matcher.group(1);

        migrationsByModuleAndVersion
                .computeIfAbsent(moduleAndVersion, ignored -> new ArrayList<>())
                .add(fileName);
    }
}
