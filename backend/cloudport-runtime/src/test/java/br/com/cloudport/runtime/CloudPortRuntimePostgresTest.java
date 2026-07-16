package br.com.cloudport.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.cloudport.runtime.integracao.OrdemPatioYardLocalAdapter;
import br.com.cloudport.runtime.integracao.PosicaoPatioYardLocalAdapter;
import br.com.cloudport.serviconaviosiderurgico.cliente.OrdemPatioYardCliente;
import br.com.cloudport.serviconaviosiderurgico.cliente.PosicaoPatioYardCliente;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(
        classes = CloudPortRuntimeApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.jpa.hibernate.ddl-auto=none",
                "spring.flyway.enabled=false",
                "spring.rabbitmq.listener.simple.auto-startup=false",
                "spring.rabbitmq.listener.direct.auto-startup=false",
                "cloudport.runtime.jobs-enabled=false",
                "cloudport.runtime.writes-enabled=true",
                "cloudport.modulo.navio.integracao=local",
                "cloudport.modulo.yard.integracao=local",
                "cloudport.security.jwt.secret=cloudport-runtime-test-secret-with-32-bytes",
                "cloudport.security.internal-service-key=cloudport-runtime-test-service-key",
                "api.security.token.secret=cloudport-runtime-test-secret-with-32-bytes",
                "jwt.secret=cloudport-runtime-test-secret-with-32-bytes",
                "app.security.cors.allowed-origins=http://localhost:4200",
                "visibilidade.dashboard.refresh-ms=3600000",
                "visibilidade.alertas.refresh-ms=3600000"
        })
class CloudPortRuntimePostgresTest {

    private static final List<String> SCHEMAS = List.of(
            "cloudport_autenticacao",
            "cloudport_gate",
            "cloudport_rail",
            "cloudport_visibilidade",
            "cloudport_yard",
            "cloudport_navio",
            "cloudport_siderurgico");

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("cloudport_runtime_test")
            .withUsername("cloudport")
            .withPassword("cloudport");

    @DynamicPropertySource
    static void configurarPostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.datasource.hikari.connection-init-sql", () ->
                "SET search_path TO " + String.join(", ", SCHEMAS) + ", public");
    }

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ApplicationContext applicationContext;
    @Autowired private OrdemPatioYardCliente ordemPatioYardCliente;
    @Autowired private PosicaoPatioYardCliente posicaoPatioYardCliente;

    @Test
    void deveInicializarTodosOsModulosComHistoricosFlywayIndependentes() {
        for (String schema : SCHEMAS) {
            assertEquals(1, quantidadeSchemas(schema));
            assertTrue(quantidadeMigracoesAplicadas(schema) > 0, "Sem migracoes no schema " + schema);
        }
        assertEquals(1, applicationContext.getBeansOfType(SecurityFilterChain.class).size());
    }

    @Test
    void deveUsarIntegracaoLocalEntreNavioEPatio() {
        assertInstanceOf(OrdemPatioYardLocalAdapter.class, ordemPatioYardCliente);
        assertInstanceOf(PosicaoPatioYardLocalAdapter.class, posicaoPatioYardCliente);
    }

    private int quantidadeSchemas(String schema) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name = ?",
                Integer.class,
                schema);
    }

    private int quantidadeMigracoesAplicadas(String schema) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + schema + ".flyway_schema_history WHERE success",
                Integer.class);
    }
}
