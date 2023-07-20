package br.com.cloudport.servicoautenticacao.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOriginPatterns("https://4200-diogo2806-cloudport-5rk6q3wf87j.ws-us102.gitpod.io","http://localhost:4200")
            .allowedMethods("*")
            .allowedHeaders("*")
            .allowCredentials(true);
    }
    
}
