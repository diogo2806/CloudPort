package br.com.cloudport.servicogate.infra;

import static org.junit.jupiter.api.Assertions.assertTrue;

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

class FlywayMigrationVersionUniquenessTest {

    private static final Pattern MIGRATION_FILENAME_PATTERN = Pattern.compile("^V([^_]+)__.+\\.sql$");

    @Test
    void devePossuirVersoesFlywayUnicas() throws IOException {
        Path migrationsDirectory = Path.of("src", "main", "resources", "db", "migration");
        Map<String, List<String>> migrationsByVersion = new TreeMap<>();

        try (Stream<Path> migrationFiles = Files.list(migrationsDirectory)) {
            migrationFiles
                .filter(Files::isRegularFile)
                .map(path -> path.getFileName().toString())
                .forEach(fileName -> adicionarMigrationPorVersao(migrationsByVersion, fileName));
        }

        List<String> duplicatedVersions = migrationsByVersion.entrySet().stream()
            .filter(entry -> entry.getValue().size() > 1)
            .map(entry -> entry.getKey() + "=" + String.join(", ", entry.getValue()))
            .collect(Collectors.toList());

        assertTrue(
            duplicatedVersions.isEmpty(),
            () -> "Versões Flyway duplicadas: " + String.join("; ", duplicatedVersions)
        );
    }

    private static void adicionarMigrationPorVersao(
        Map<String, List<String>> migrationsByVersion,
        String fileName
    ) {
        Matcher matcher = MIGRATION_FILENAME_PATTERN.matcher(fileName);
        if (!matcher.matches()) {
            return;
        }

        migrationsByVersion
            .computeIfAbsent(matcher.group(1), ignored -> new ArrayList<>())
            .add(fileName);
    }
}
