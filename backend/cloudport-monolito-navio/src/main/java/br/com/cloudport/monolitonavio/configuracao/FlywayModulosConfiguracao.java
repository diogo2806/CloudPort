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

    public FlywayModulosConfiguracao(
            DataSource dataSource,
            @Value("${cloudport.monolito.schema.navio:cloudport_navio}") String schemaNavio,
            @Value("${cloudport.monolito.schema.siderurgico:cloudport_siderurgico}") String schemaSiderurgico) {
        this.dataSource = dataSource;
        this.schemaNavio = validarSchema(schemaNavio, "Navio");
        this.schemaSiderurgico = validarSchema(schemaSiderurgico, "Navio Siderurgico");
    }

    @Bean(name = "flywayNavio", initMethod = "migrate")
    public Flyway flywayNavio() {
        return criarFlyway("classpath:cloudport/migrations/navio", schemaNavio);
    }

    @Bean(name = "flywayNavioSiderurgico", initMethod = "migrate")
    public Flyway flywayNavioSiderurgico() {
        return criarFlyway("classpath:cloudport/migrations/navio-siderurgico", schemaSiderurgico);
    }

    @Bean
    public static EntityManagerFactoryDependsOnPostProcessor entityManagerFactoryDependsOnFlywayModulos() {
        return new EntityManagerFactoryDependsOnPostProcessor("flywayNavio", "flywayNavioSiderurgico");
    }

    private Flyway criarFlyway(String localMigracoes, String schema) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations(localMigracoes)
                .schemas(schema)
                .defaultSchema(schema)
                .createSchemas(true)
                .load();
    }

    private String validarSchema(String schema, String modulo) {
        if (schema == null || !NOME_SCHEMA_VALIDO.matcher(schema).matches()) {
            throw new IllegalArgumentException("Nome de schema invalido para o modulo " + modulo + ".");
        }
        return schema;
    }
}
