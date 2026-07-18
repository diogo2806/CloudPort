package br.com.cloudport.runtime;

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
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "br\\.com\\.cloudport\\.(servicogate\\.config\\.SecurityConfig|servicorail\\.configuracao\\.ConfiguracaoSeguranca|servicoyard\\.configuracao\\.ConfiguracaoSeguranca|serviconavio\\.configuracao\\.ConfiguracaoSeguranca|serviconaviosiderurgico\\.configuracao\\.ConfiguracaoSeguranca|visibilidade\\.config\\.SecurityConfig)")
        })
public class CloudPortRuntimeApplication {

    public static void main(String[] args) {
        SpringApplication.run(CloudPortRuntimeApplication.class, args);
    }
}
