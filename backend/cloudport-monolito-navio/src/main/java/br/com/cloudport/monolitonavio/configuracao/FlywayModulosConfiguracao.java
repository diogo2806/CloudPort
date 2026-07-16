package br.com.cloudport.monolitonavio.configuracao;

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
    private final String schemaNavio;
    private final String schemaSiderurgico;
    private final String schemaYard;
    private final String schemaGate;
    private final String schemaRail;
    private final String schemaAutenticacao;
    private final String schemaVisibilidade;

    public FlywayModulosConfiguracao(
            DataSource dataSource,
            @Value("${cloudport.monolito.schema.navio:cloudport_navio}") String schemaNavio,
            @Value("${cloudport.monolito.schema.siderurgico:cloudport_siderurgico}") String schemaSiderurgico,
            @Value("${cloudport.monolito.schema.yard:cloudport_yard}") String schemaYard,
            @Value("${cloudport.monolito.schema.gate:cloudport_gate}") String schemaGate,
            @Value("${cloudport.monolito.schema.rail:cloudport_rail}") String schemaRail,
            @Value("${cloudport.monolito.schema.autenticacao:cloudport_autenticacao}") String schemaAutenticacao,
            @Value("${cloudport.monolito.schema.visibilidade:cloudport_visibilidade}") String schemaVisibilidade) {
        this.dataSource = dataSource;
        this.schemaNavio = validarSchema(schemaNavio, "Navio");
        this.schemaSiderurgico = validarSchema(schemaSiderurgico, "Navio Siderúrgico");
        this.schemaYard = validarSchema(schemaYard, "Yard");
        this.schemaGate = validarSchema(schemaGate, "Gate");
        this.schemaRail = validarSchema(schemaRail, "Rail");
        this.schemaAutenticacao = validarSchema(schemaAutenticacao, "Autenticação");
        this.schemaVisibilidade = validarSchema(schemaVisibilidade, "Visibilidade");
    }

    @Bean(name = "flywayNavio", initMethod = "migrate")
    public Flyway flywayNavio() {
        return criarFlyway("classpath:cloudport/migrations/navio", schemaNavio);
    }

    @Bean(name = "flywayNavioSiderurgico", initMethod = "migrate")
    public Flyway flywayNavioSiderurgico() {
        return criarFlyway("classpath:cloudport/migrations/navio-siderurgico", schemaSiderurgico);
    }

    @Bean(name = "flywayYard", initMethod = "migrate")
    public Flyway flywayYard() {
        return criarFlyway("classpath:cloudport/migrations/yard", schemaYard);
    }

    @Bean(name = "flywayGate", initMethod = "migrate")
    public Flyway flywayGate() {
        return criarFlyway("classpath:cloudport/migrations/gate", schemaGate);
    }

    @Bean(name = "flywayRail", initMethod = "migrate")
    public Flyway flywayRail() {
        return criarFlyway("classpath:cloudport/migrations/rail", schemaRail);
    }

    @Bean(name = "flywayAutenticacao", initMethod = "migrate")
    public Flyway flywayAutenticacao() {
        return criarFlyway("classpath:cloudport/migrations/autenticacao", schemaAutenticacao);
    }

    @Bean(name = "flywayVisibilidade", initMethod = "migrate")
    public Flyway flywayVisibilidade() {
        return criarFlyway("classpath:cloudport/migrations/visibilidade", schemaVisibilidade);
    }

    @Bean
    public static EntityManagerFactoryDependsOnPostProcessor entityManagerFactoryDependsOnFlywayModulos() {
        return new EntityManagerFactoryDependsOnPostProcessor(
                "flywayNavio",
                "flywayNavioSiderurgico",
                "flywayYard",
                "flywayGate",
                "flywayRail",
                "flywayAutenticacao",
                "flywayVisibilidade");
    }

    private Flyway criarFlyway(String localMigracoes, String schema) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations(localMigracoes)
                .schemas(schema)
                .defaultSchema(schema)
                .table("flyway_schema_history")
                .validateOnMigrate(true)
                .cleanDisabled(true)
                .createSchemas(true)
                .load();
    }

    private String validarSchema(String schema, String modulo) {
        if (schema == null || !NOME_SCHEMA_VALIDO.matcher(schema).matches()) {
            throw new IllegalArgumentException("Nome de schema inválido para o módulo " + modulo + ".");
        }
        return schema;
    }
}
