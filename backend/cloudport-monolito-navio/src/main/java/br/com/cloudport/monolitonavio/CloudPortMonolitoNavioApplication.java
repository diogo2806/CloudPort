package br.com.cloudport.monolitonavio;

import br.com.cloudport.serviconavio.ServicoNavioApplication;
import br.com.cloudport.serviconaviosiderurgico.ServicoNavioSiderurgicoApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootConfiguration
@EnableAutoConfiguration
@EnableScheduling
@EntityScan(basePackages = {
        "br.com.cloudport.serviconavio",
        "br.com.cloudport.serviconaviosiderurgico"
})
@EnableJpaRepositories(basePackages = {
        "br.com.cloudport.serviconavio",
        "br.com.cloudport.serviconaviosiderurgico"
})
@ComponentScan(
        basePackages = {
                "br.com.cloudport.monolitonavio",
                "br.com.cloudport.serviconavio",
                "br.com.cloudport.serviconaviosiderurgico"
        },
        nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ServicoNavioApplication.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ServicoNavioSiderurgicoApplication.class),
                @ComponentScan.Filter(
                        type = FilterType.ASSIGNABLE_TYPE,
                        classes = {
                                br.com.cloudport.serviconavio.configuracao.ConfiguracaoSeguranca.class,
                                br.com.cloudport.serviconaviosiderurgico.configuracao.ConfiguracaoSeguranca.class
                        })
        })
public class CloudPortMonolitoNavioApplication {

    public static void main(String[] args) {
        SpringApplication.run(CloudPortMonolitoNavioApplication.class, args);
    }
}
