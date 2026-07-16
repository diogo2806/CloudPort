package br.com.cloudport.monolitonavio;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.cloudport.monolitonavio.integracao.AutenticacaoLocalAdapter;
import br.com.cloudport.monolitonavio.integracao.CadastroNavioLocalAdapter;
import br.com.cloudport.monolitonavio.integracao.OrdemPatioLocalAdapter;
import br.com.cloudport.monolitonavio.integracao.PosicaoPatioLocalAdapter;
import br.com.cloudport.monolitonavio.integracao.StatusPatioLocalAdapter;
import br.com.cloudport.servicogate.integration.yard.ClienteStatusPatio;
import br.com.cloudport.servicogate.integration.yard.ClienteStatusPatioHttpAdapter;
import br.com.cloudport.servicogate.security.AutenticacaoClient;
import br.com.cloudport.servicogate.security.AutenticacaoHttpAdapter;
import br.com.cloudport.serviconaviosiderurgico.cliente.NavioCadastroCliente;
import br.com.cloudport.serviconaviosiderurgico.cliente.OrdemPatioYardCliente;
import br.com.cloudport.serviconaviosiderurgico.cliente.OrdemPatioYardHttpAdapter;
import br.com.cloudport.serviconaviosiderurgico.cliente.PosicaoPatioYardCliente;
import br.com.cloudport.serviconaviosiderurgico.cliente.PosicaoPatioYardHttpAdapter;
import br.com.cloudport.serviconaviosiderurgico.porta.CadastroNavioPorta;
import br.com.cloudport.serviconaviosiderurgico.servico.ExecucaoUnicaServico;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.RestController;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(
        classes = CloudPortMonolitoNavioApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.jpa.properties.hibernate.hbm2ddl.jdbc_metadata_extraction_strategy=individually",
                "spring.jpa.open-in-view=false",
                "spring.flyway.enabled=false",
                "cloudport.modulo.navio.integracao=local",
                "cloudport.modulo.yard.integracao=local",
                "cloudport.modulo.autenticacao.integracao=local",
                "cloudport.runtime.writes-enabled=true",
                "cloudport.runtime.jobs-enabled=false",
                "cloudport.runtime.consumers-enabled=false",
                "spring.rabbitmq.listener.simple.auto-startup=false",
                "spring.rabbitmq.listener.direct.auto-startup=false",
                "cloudport.security.jwt.secret=cloudport-test-secret-with-at-least-32-characters",
                "jwt.secret=cloudport-test-secret-with-at-least-32-characters",
                "api.security.token.secret=cloudport-test-secret-with-at-least-32-characters",
                "cloudport.security.internal-service-key=cloudport-test-service-key"
        })
class CloudPortMonolitoNavioPostgresTest {

    private static final String SCHEMA_NAVIO = "cloudport_navio";
    private static final String SCHEMA_SIDERURGICO = "cloudport_siderurgico";
    private static final String SCHEMA_YARD = "cloudport_yard";
    private static final String SCHEMA_GATE = "cloudport_gate";
    private static final String SCHEMA_RAIL = "cloudport_rail";
    private static final String SCHEMA_AUTENTICACAO = "cloudport_autenticacao";
    private static final String SCHEMA_VISIBILIDADE = "cloudport_visibilidade";
    private static final List<String> SCHEMAS = List.of(
            SCHEMA_NAVIO,
            SCHEMA_SIDERURGICO,
            SCHEMA_YARD,
            SCHEMA_GATE,
            SCHEMA_RAIL,
            SCHEMA_AUTENTICACAO,
            SCHEMA_VISIBILIDADE);

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
                () -> "SET search_path TO " + String.join(", ", SCHEMAS) + ", public");
        registry.add("cloudport.monolito.schema.navio", () -> SCHEMA_NAVIO);
        registry.add("cloudport.monolito.schema.siderurgico", () -> SCHEMA_SIDERURGICO);
        registry.add("cloudport.monolito.schema.yard", () -> SCHEMA_YARD);
        registry.add("cloudport.monolito.schema.gate", () -> SCHEMA_GATE);
        registry.add("cloudport.monolito.schema.rail", () -> SCHEMA_RAIL);
        registry.add("cloudport.monolito.schema.autenticacao", () -> SCHEMA_AUTENTICACAO);
        registry.add("cloudport.monolito.schema.visibilidade", () -> SCHEMA_VISIBILIDADE);
    }

    @Autowired private ApplicationContext applicationContext;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private CadastroNavioPorta cadastroNavioPorta;
    @Autowired private OrdemPatioYardCliente ordemPatioYardCliente;
    @Autowired private PosicaoPatioYardCliente posicaoPatioYardCliente;
    @Autowired private ClienteStatusPatio clienteStatusPatio;
    @Autowired private AutenticacaoClient autenticacaoClient;
    @Autowired private ExecucaoUnicaServico execucaoUnicaServico;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired @Qualifier("flywayNavio") private Flyway flywayNavio;
    @Autowired @Qualifier("flywayNavioSiderurgico") private Flyway flywayNavioSiderurgico;
    @Autowired @Qualifier("flywayYard") private Flyway flywayYard;
    @Autowired @Qualifier("flywayGate") private Flyway flywayGate;
    @Autowired @Qualifier("flywayRail") private Flyway flywayRail;
    @Autowired @Qualifier("flywayAutenticacao") private Flyway flywayAutenticacao;
    @Autowired @Qualifier("flywayVisibilidade") private Flyway flywayVisibilidade;

    @Test
    void deveCriarUmSchemaEHistoricoPorModulo() {
        SCHEMAS.forEach(schema -> assertEquals(1, quantidadeSchemas(schema), schema));
        assertAll(
                () -> assertTrue(quantidadeMigracoesAplicadas(SCHEMA_NAVIO) > 0),
                () -> assertTrue(quantidadeMigracoesAplicadas(SCHEMA_SIDERURGICO) > 0),
                () -> assertTrue(quantidadeTabelas(SCHEMA_NAVIO) > 0),
                () -> assertTrue(quantidadeTabelas(SCHEMA_SIDERURGICO) > 0),
                () -> assertTrue(quantidadeTabelas(SCHEMA_YARD) > 0),
                () -> assertTrue(quantidadeTabelas(SCHEMA_GATE) > 0),
                () -> assertTrue(quantidadeTabelas(SCHEMA_RAIL) > 0),
                () -> assertTrue(quantidadeTabelas(SCHEMA_AUTENTICACAO) > 0));
    }

    @Test
    void deveValidarCompatibilidadeDosHistoricosFlywaySemPendencias() {
        List<Flyway> flyways = List.of(
                flywayNavio,
                flywayNavioSiderurgico,
                flywayYard,
                flywayGate,
                flywayRail,
                flywayAutenticacao,
                flywayVisibilidade);
        flyways.forEach(Flyway::validate);
        flyways.forEach(flyway -> assertEquals(0, flyway.info().pending().length));
    }

    @Test
    void deveCarregarSegurancaUnicaEPortasLocais() {
        assertAll(
                () -> assertInstanceOf(CadastroNavioLocalAdapter.class, cadastroNavioPorta),
                () -> assertInstanceOf(OrdemPatioLocalAdapter.class, ordemPatioYardCliente),
                () -> assertInstanceOf(PosicaoPatioLocalAdapter.class, posicaoPatioYardCliente),
                () -> assertInstanceOf(StatusPatioLocalAdapter.class, clienteStatusPatio),
                () -> assertInstanceOf(AutenticacaoLocalAdapter.class, autenticacaoClient),
                () -> assertTrue(applicationContext.getBeansOfType(NavioCadastroCliente.class).isEmpty()),
                () -> assertTrue(applicationContext.getBeansOfType(OrdemPatioYardHttpAdapter.class).isEmpty()),
                () -> assertTrue(applicationContext.getBeansOfType(PosicaoPatioYardHttpAdapter.class).isEmpty()),
                () -> assertTrue(applicationContext.getBeansOfType(ClienteStatusPatioHttpAdapter.class).isEmpty()),
                () -> assertTrue(applicationContext.getBeansOfType(AutenticacaoHttpAdapter.class).isEmpty()),
                () -> assertEquals(1, applicationContext.getBeansOfType(SecurityFilterChain.class).size()),
                () -> assertTrue(applicationContext.getBeansWithAnnotation(RestController.class).size() > 20));
    }

    @Test
    void deveImpedirExecucaoDuplicadaDoMesmoJobEntreInstancias() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CountDownLatch primeiraExecucaoIniciada = new CountDownLatch(1);
        CountDownLatch liberarPrimeiraExecucao = new CountDownLatch(1);
        TransactionTemplate transacao = new TransactionTemplate(transactionManager);
        try {
            Future<Boolean> primeiraExecucao = executor.submit(() -> Boolean.TRUE.equals(transacao.execute(status ->
                    execucaoUnicaServico.executarSeDisponivel("teste:job-unico", () -> {
                        primeiraExecucaoIniciada.countDown();
                        aguardar(liberarPrimeiraExecucao);
                    }))));
            assertTrue(primeiraExecucaoIniciada.await(10, SECONDS));
            Boolean segundaExecucao = transacao.execute(status ->
                    execucaoUnicaServico.executarSeDisponivel("teste:job-unico", () -> { }));
            assertFalse(Boolean.TRUE.equals(segundaExecucao));
            liberarPrimeiraExecucao.countDown();
            assertTrue(primeiraExecucao.get(10, SECONDS));
        } finally {
            liberarPrimeiraExecucao.countDown();
            executor.shutdownNow();
        }
    }

    private void aguardar(CountDownLatch latch) {
        try {
            if (!latch.await(10, SECONDS)) {
                throw new IllegalStateException("Tempo excedido aguardando liberação do bloqueio.");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Thread interrompida durante o teste de execução única.", ex);
        }
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
