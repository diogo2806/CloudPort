package br.com.cloudport.runtime.configuracao;

import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.orm.jpa.EntityManagerFactoryDependsOnPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayModulosConfiguracao {

    private static final Pattern NOME_SCHEMA_VALIDO = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    private final DataSource dataSource;
    private final String schemaAutenticacao;
    private final String schemaCargaGeral;
    private final String schemaGate;
    private final String schemaRail;
    private final String schemaVisibilidade;
    private final String schemaYard;
    private final String schemaNavio;
    private final String schemaSiderurgico;

    public FlywayModulosConfiguracao(
            DataSource dataSource,
            @Value("${cloudport.runtime.schema.autenticacao:cloudport_autenticacao}") String schemaAutenticacao,
            @Value("${cloudport.runtime.schema.carga-geral:cloudport_carga_geral}") String schemaCargaGeral,
            @Value("${cloudport.runtime.schema.gate:cloudport_gate}") String schemaGate,
            @Value("${cloudport.runtime.schema.rail:cloudport_rail}") String schemaRail,
            @Value("${cloudport.runtime.schema.visibilidade:cloudport_visibilidade}") String schemaVisibilidade,
            @Value("${cloudport.runtime.schema.yard:cloudport_yard}") String schemaYard,
            @Value("${cloudport.runtime.schema.navio:cloudport_navio}") String schemaNavio,
            @Value("${cloudport.runtime.schema.siderurgico:cloudport_siderurgico}") String schemaSiderurgico) {
        this.dataSource = dataSource;
        this.schemaAutenticacao = validarSchema(schemaAutenticacao, "Autenticacao");
        this.schemaCargaGeral = validarSchema(schemaCargaGeral, "Carga Geral");
        this.schemaGate = validarSchema(schemaGate, "Gate");
        this.schemaRail = validarSchema(schemaRail, "Rail");
        this.schemaVisibilidade = validarSchema(schemaVisibilidade, "Visibilidade");
        this.schemaYard = validarSchema(schemaYard, "Yard");
        this.schemaNavio = validarSchema(schemaNavio, "Navio");
        this.schemaSiderurgico = validarSchema(schemaSiderurgico, "Navio Siderurgico");
    }

    @Bean(name = "flywayAutenticacao", initMethod = "migrate")
    public Flyway flywayAutenticacao() {
        return criarFlyway("classpath:cloudport/migrations/autenticacao/db/migration", schemaAutenticacao);
    }

    @Bean(name = "flywayCargaGeral", initMethod = "migrate")
    public Flyway flywayCargaGeral() {
        return criarFlyway("classpath:cloudport/migrations/carga-geral/db/migration", schemaCargaGeral);
    }

    @Bean(name = "flywayGate", initMethod = "migrate")
    public Flyway flywayGate() {
        return criarFlyway("classpath:cloudport/migrations/gate/db/migration", schemaGate, true);
    }

    @Bean(name = "flywayRail", initMethod = "migrate")
    public Flyway flywayRail() {
        return criarFlyway("classpath:cloudport/migrations/rail/db/migration", schemaRail);
    }

    @Bean(name = "flywayVisibilidade", initMethod = "migrate")
    public Flyway flywayVisibilidade() {
        return criarFlyway("classpath:cloudport/migrations/visibilidade/db/migration", schemaVisibilidade);
    }

    @Bean(name = "flywayYard", initMethod = "migrate")
    public Flyway flywayYard() {
        return criarFlyway("classpath:cloudport/migrations/yard/db/migration", schemaYard);
    }

    @Bean(name = "flywayNavio", initMethod = "migrate")
    public Flyway flywayNavio() {
        return criarFlyway("classpath:cloudport/migrations/navio/db/migration", schemaNavio);
    }

    @Bean(name = "flywayNavioSiderurgico", initMethod = "migrate")
    public Flyway flywayNavioSiderurgico() {
        return criarFlyway("classpath:cloudport/migrations/navio-siderurgico/db/migration", schemaSiderurgico);
    }

    @Bean
    public static EntityManagerFactoryDependsOnPostProcessor entityManagerFactoryDependsOnFlywayModulos() {
        return new EntityManagerFactoryDependsOnPostProcessor(
                "flywayAutenticacao",
                "flywayCargaGeral",
                "flywayGate",
                "flywayRail",
                "flywayVisibilidade",
                "flywayYard",
                "flywayNavio",
                "flywayNavioSiderurgico");
    }

    private Flyway criarFlyway(String localMigracoes, String schema) {
        return criarFlyway(localMigracoes, schema, false);
    }

    private Flyway criarFlyway(String localMigracoes, String schema, boolean executarForaDeOrdem) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations(localMigracoes)
                .schemas(schema)
                .defaultSchema(schema)
                .createSchemas(true)
                .validateOnMigrate(true)
                .outOfOrder(executarForaDeOrdem)
                .load();
    }

    private String validarSchema(String schema, String modulo) {
        if (schema == null || !NOME_SCHEMA_VALIDO.matcher(schema).matches()) {
            throw new IllegalArgumentException("Nome de schema invalido para o modulo " + modulo + ".");
        }
        return schema;
    }
}
