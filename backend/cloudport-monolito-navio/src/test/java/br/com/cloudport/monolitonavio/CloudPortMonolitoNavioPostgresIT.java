package br.com.cloudport.monolitonavio;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.cloudport.serviconavio.escala.repositorio.EscalaRepositorio;
import br.com.cloudport.serviconavio.estiva.repositorio.AtribuicaoEstivaRepositorio;
import br.com.cloudport.serviconavio.estiva.repositorio.PlanoEstivaRepositorio;
import br.com.cloudport.serviconavio.navio.repositorio.NavioRepositorio;
import br.com.cloudport.serviconaviosiderurgico.repositorio.EventoVisitaNavioRepositorio;
import br.com.cloudport.serviconaviosiderurgico.repositorio.ItemCargaSiderurgicaRepositorio;
import br.com.cloudport.serviconaviosiderurgico.repositorio.ItemOperacaoNavioRepositorio;
import br.com.cloudport.serviconaviosiderurgico.repositorio.NavioSiderurgicoRepositorio;
import br.com.cloudport.serviconaviosiderurgico.repositorio.OperacaoSiderurgicaRepositorio;
import br.com.cloudport.serviconaviosiderurgico.repositorio.PlanoEstivaNavioRepositorio;
import br.com.cloudport.serviconaviosiderurgico.repositorio.PosicaoEstivaNavioRepositorio;
import br.com.cloudport.serviconaviosiderurgico.repositorio.ReservaPosicaoPatioNavioRepositorio;
import br.com.cloudport.serviconaviosiderurgico.repositorio.VisitaNavioRepositorio;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(
        classes = CloudPortMonolitoNavioApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.jpa.open-in-view=false",
                "spring.flyway.enabled=false",
                "cloudport.modulo.navio.integracao=local",
                "cloudport.integracao.yard.reconciliacao-ms=3600000",
                "cloudport.integracao.navio.sincronizacao-ms=3600000",
                "cloudport.security.jwt.secret=cloudport-test-secret-with-at-least-32-characters",
                "cloudport.security.internal-service-key=cloudport-test-service-key"
        })
class CloudPortMonolitoNavioPostgresIT {

    private static final String SCHEMA_NAVIO = "cloudport_navio";
    private static final String SCHEMA_SIDERURGICO = "cloudport_siderurgico";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("cloudport_monolito_test")
            .withUsername("cloudport")
            .withPassword("cloudport");

    @DynamicPropertySource
    static void configurarPostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.datasource.hikari.connection-init-sql",
                () -> "SET search_path TO " + SCHEMA_NAVIO + ", " + SCHEMA_SIDERURGICO + ", public");
        registry.add("cloudport.monolito.schema.navio", () -> SCHEMA_NAVIO);
        registry.add("cloudport.monolito.schema.siderurgico", () -> SCHEMA_SIDERURGICO);
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private NavioRepositorio navioRepositorio;

    @Autowired
    private EscalaRepositorio escalaRepositorio;

    @Autowired
    private PlanoEstivaRepositorio planoEstivaRepositorio;

    @Autowired
    private AtribuicaoEstivaRepositorio atribuicaoEstivaRepositorio;

    @Autowired
    private NavioSiderurgicoRepositorio navioSiderurgicoRepositorio;

    @Autowired
    private OperacaoSiderurgicaRepositorio operacaoSiderurgicaRepositorio;

    @Autowired
    private ItemCargaSiderurgicaRepositorio itemCargaSiderurgicaRepositorio;

    @Autowired
    private VisitaNavioRepositorio visitaNavioRepositorio;

    @Autowired
    private ItemOperacaoNavioRepositorio itemOperacaoNavioRepositorio;

    @Autowired
    private PlanoEstivaNavioRepositorio planoEstivaNavioRepositorio;

    @Autowired
    private PosicaoEstivaNavioRepositorio posicaoEstivaNavioRepositorio;

    @Autowired
    private EventoVisitaNavioRepositorio eventoVisitaNavioRepositorio;

    @Autowired
    private ReservaPosicaoPatioNavioRepositorio reservaPosicaoPatioNavioRepositorio;

    @Test
    void deveAplicarMigracoesReaisNosDoisSchemas() {
        assertAll(
                () -> assertEquals(1, quantidadeSchemas(SCHEMA_NAVIO)),
                () -> assertEquals(1, quantidadeSchemas(SCHEMA_SIDERURGICO)),
                () -> assertTrue(quantidadeMigracoesAplicadas(SCHEMA_NAVIO) > 0),
                () -> assertTrue(quantidadeMigracoesAplicadas(SCHEMA_SIDERURGICO) > 0),
                () -> assertTrue(quantidadeTabelas(SCHEMA_NAVIO) > 0),
                () -> assertTrue(quantidadeTabelas(SCHEMA_SIDERURGICO) > 0)
        );
    }

    @Test
    void deveInicializarEConsultarTodosOsRepositoriosJpa() {
        assertAll(
                () -> assertEquals(0, navioRepositorio.count()),
                () -> assertEquals(0, escalaRepositorio.count()),
                () -> assertEquals(0, planoEstivaRepositorio.count()),
                () -> assertEquals(0, atribuicaoEstivaRepositorio.count()),
                () -> assertEquals(0, navioSiderurgicoRepositorio.count()),
                () -> assertEquals(0, operacaoSiderurgicaRepositorio.count()),
                () -> assertEquals(0, itemCargaSiderurgicaRepositorio.count()),
                () -> assertEquals(0, visitaNavioRepositorio.count()),
                () -> assertEquals(0, itemOperacaoNavioRepositorio.count()),
                () -> assertEquals(0, planoEstivaNavioRepositorio.count()),
                () -> assertEquals(0, posicaoEstivaNavioRepositorio.count()),
                () -> assertEquals(0, eventoVisitaNavioRepositorio.count()),
                () -> assertEquals(0, reservaPosicaoPatioNavioRepositorio.count())
        );
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

    private int quantidadeTabelas(String schema) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = ? AND table_type = 'BASE TABLE'",
                Integer.class,
                schema);
    }
}
