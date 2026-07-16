package br.com.cloudport.monolitonavio;

import br.com.cloudport.servicoautenticacao.ServicoAutenticacaoApplication;
import br.com.cloudport.servicogate.ServicoGateApplication;
import br.com.cloudport.serviconavio.ServicoNavioApplication;
import br.com.cloudport.serviconaviosiderurgico.ServicoNavioSiderurgicoApplication;
import br.com.cloudport.servicorail.ServicoRailApplication;
import br.com.cloudport.servicoyard.ServicoYardApplication;
import br.com.cloudport.visibilidade.VisibilidadeApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootConfiguration
@EnableAutoConfiguration
@EntityScan(basePackages = {
        "br.com.cloudport.serviconavio",
        "br.com.cloudport.serviconaviosiderurgico",
        "br.com.cloudport.servicoyard",
        "br.com.cloudport.servicogate",
        "br.com.cloudport.servicorail",
        "br.com.cloudport.servicoautenticacao",
        "br.com.cloudport.visibilidade"
})
@EnableJpaRepositories(basePackages = {
        "br.com.cloudport.serviconavio",
        "br.com.cloudport.serviconaviosiderurgico",
        "br.com.cloudport.servicoyard",
        "br.com.cloudport.servicogate",
        "br.com.cloudport.servicorail",
        "br.com.cloudport.servicoautenticacao",
        "br.com.cloudport.visibilidade"
})
@ComponentScan(
        basePackages = {
                "br.com.cloudport.monolitonavio",
                "br.com.cloudport.serviconavio",
                "br.com.cloudport.serviconaviosiderurgico",
                "br.com.cloudport.servicoyard",
                "br.com.cloudport.servicogate",
                "br.com.cloudport.servicorail",
                "br.com.cloudport.servicoautenticacao",
                "br.com.cloudport.visibilidade"
        },
        nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                        ServicoNavioApplication.class,
                        ServicoNavioSiderurgicoApplication.class,
                        ServicoYardApplication.class,
                        ServicoGateApplication.class,
                        ServicoRailApplication.class,
                        ServicoAutenticacaoApplication.class,
                        VisibilidadeApplication.class
                }),
                @ComponentScan.Filter(
                        type = FilterType.REGEX,
                        pattern = "br\\.com\\.cloudport\\..*\\.(ConfiguracaoSeguranca|SecurityConfig|SecurityConfigurations|SecurityFilter|TratadorExcecoes|GlobalExceptionHandler|VisibilidadeExceptionHandler|OpenApiConfig|RabbitConfiguracao|ModoEscritaNavioFiltro|ModoEscritaSiderurgicoFiltro|InternalServiceAuthenticationFilter|ObservabilidadeYardFiltro|ObservabilidadeOperacionalFiltro)")
        })
public class CloudPortMonolitoNavioApplication {

    public static void main(String[] args) {
        SpringApplication.run(CloudPortMonolitoNavioApplication.class, args);
    }
}
