package br.com.cloudport.runtime;

import br.com.cloudport.runtime.configuracao.RedisEnvironmentInitializer;
import br.com.cloudport.servicoautenticacao.ServicoAutenticacaoApplication;
import br.com.cloudport.servicoautenticacao.config.SecurityConfigurations;
import br.com.cloudport.servicoautenticacao.config.SecurityFilter;
import br.com.cloudport.servicocargageral.ServicoCargaGeralApplication;
import br.com.cloudport.servicogate.ServicoGateApplication;
import br.com.cloudport.servicorail.ServicoRailApplication;
import br.com.cloudport.serviconavio.ServicoNavioApplication;
import br.com.cloudport.serviconaviosiderurgico.ServicoNavioSiderurgicoApplication;
import br.com.cloudport.servicoyard.ServicoYardApplication;
import br.com.cloudport.visibilidade.VisibilidadeApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootConfiguration
@EnableAutoConfiguration
@EnableScheduling
@EnableAsync
@EnableCaching
@EntityScan(basePackages = {
        "br.com.cloudport.servicoautenticacao",
        "br.com.cloudport.servicocargageral",
        "br.com.cloudport.servicogate",
        "br.com.cloudport.servicorail",
        "br.com.cloudport.servicoyard",
        "br.com.cloudport.serviconavio",
        "br.com.cloudport.serviconaviosiderurgico",
        "br.com.cloudport.visibilidade"
})
@EnableJpaRepositories(basePackages = {
        "br.com.cloudport.servicoautenticacao",
        "br.com.cloudport.servicocargageral",
        "br.com.cloudport.servicogate",
        "br.com.cloudport.servicorail",
        "br.com.cloudport.servicoyard",
        "br.com.cloudport.serviconavio",
        "br.com.cloudport.serviconaviosiderurgico",
        "br.com.cloudport.visibilidade"
})
@ComponentScan(
        basePackages = {
                "br.com.cloudport.runtime",
                "br.com.cloudport.servicoautenticacao",
                "br.com.cloudport.servicocargageral",
                "br.com.cloudport.servicogate",
                "br.com.cloudport.servicorail",
                "br.com.cloudport.servicoyard",
                "br.com.cloudport.serviconavio",
                "br.com.cloudport.serviconaviosiderurgico",
                "br.com.cloudport.visibilidade"
        },
        nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ServicoAutenticacaoApplication.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ServicoCargaGeralApplication.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ServicoGateApplication.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ServicoRailApplication.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ServicoYardApplication.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ServicoNavioApplication.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ServicoNavioSiderurgicoApplication.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = VisibilidadeApplication.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfigurations.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityFilter.class),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "br\\.com\\.cloudport\\.(servicocargageral\\.configuracao\\.ConfiguracaoSeguranca|servicogate\\.config\\.SecurityConfig|servicorail\\.configuracao\\.ConfiguracaoSeguranca|servicoyard\\.configuracao\\.ConfiguracaoSeguranca|serviconavio\\.configuracao\\.ConfiguracaoSeguranca|serviconaviosiderurgico\\.configuracao\\.ConfiguracaoSeguranca|visibilidade\\.config\\.SecurityConfig)")
        })
public class CloudPortRuntimeApplication {

    private static final String RABBIT_ENABLED_PROPERTY = "cloudport.messaging.rabbit.enabled";
    private static final String RABBIT_CONSUMERS_ENABLED_PROPERTY = "cloudport.runtime.consumers-enabled";
    private static final String SPRING_AUTOCONFIGURE_EXCLUDE = "spring.autoconfigure.exclude";

    static {
        configurarRabbitMqOpcional();
    }

    public static void main(String[] args) {
        criarAplicacao().run(args);
    }

    static SpringApplication criarAplicacao() {
        SpringApplication application = new SpringApplication(CloudPortRuntimeApplication.class);
        application.addInitializers(new RedisEnvironmentInitializer());
        return application;
    }

    private static void configurarRabbitMqOpcional() {
        String habilitado = System.getProperty(RABBIT_ENABLED_PROPERTY);
        if (habilitado == null || habilitado.isBlank()) {
            String variavelAmbiente = System.getenv("RABBITMQ_ENABLED");
            habilitado = variavelAmbiente == null || variavelAmbiente.isBlank()
                    ? Boolean.FALSE.toString()
                    : Boolean.toString(Boolean.parseBoolean(variavelAmbiente));
        }

        definirPropriedadePadrao(RABBIT_ENABLED_PROPERTY, habilitado);
        definirPropriedadePadrao(RABBIT_CONSUMERS_ENABLED_PROPERTY, habilitado);
        definirPropriedadePadrao("spring.rabbitmq.dynamic", habilitado);
        definirPropriedadePadrao("spring.rabbitmq.listener.simple.auto-startup", habilitado);
        definirPropriedadePadrao("spring.rabbitmq.listener.direct.auto-startup", habilitado);
        definirPropriedadePadrao("management.health.rabbit.enabled", habilitado);

        if (!Boolean.parseBoolean(habilitado)) {
            adicionarExclusaoAutoConfiguracao(RabbitAutoConfiguration.class.getName());
        }
    }

    private static void adicionarExclusaoAutoConfiguracao(String classe) {
        String exclusoes = System.getProperty(SPRING_AUTOCONFIGURE_EXCLUDE);
        if (exclusoes == null || exclusoes.isBlank()) {
            exclusoes = System.getenv("SPRING_AUTOCONFIGURE_EXCLUDE");
        }

        if (exclusoes == null || exclusoes.isBlank()) {
            System.setProperty(SPRING_AUTOCONFIGURE_EXCLUDE, classe);
            return;
        }

        String exclusoesNormalizadas = "," + exclusoes.replace(" ", "") + ",";
        if (!exclusoesNormalizadas.contains("," + classe + ",")) {
            System.setProperty(SPRING_AUTOCONFIGURE_EXCLUDE, exclusoes + "," + classe);
        }
    }

    private static void definirPropriedadePadrao(String nome, String valor) {
        System.getProperties().putIfAbsent(nome, valor);
    }
}
