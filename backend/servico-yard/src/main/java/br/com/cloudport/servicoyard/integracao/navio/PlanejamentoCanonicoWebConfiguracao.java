package br.com.cloudport.servicoyard.integracao.navio;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class PlanejamentoCanonicoWebConfiguracao implements WebMvcConfigurer {
    private final PlanejamentoCanonicoInterceptor interceptor;

    public PlanejamentoCanonicoWebConfiguracao(PlanejamentoCanonicoInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor)
                .addPathPatterns("/api/estivagem-bulk/planos/**", "/api/vessel-planner/planos/**");
    }
}
