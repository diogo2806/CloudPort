package br.com.cloudport.serviconaviosiderurgico.configuracao;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Component
public class YardRestTemplateCustomizer implements RestTemplateCustomizer {

    private static final String HEADER_SERVICE_KEY = "X-CloudPort-Service-Key";
    private final String serviceKey;

    public YardRestTemplateCustomizer(
            @Value("${cloudport.security.internal-service-key:}") String serviceKey) {
        this.serviceKey = serviceKey;
    }

    @Override
    public void customize(RestTemplate restTemplate) {
        if (!StringUtils.hasText(serviceKey)) {
            return;
        }
        restTemplate.getInterceptors().add((request, body, execution) -> {
            request.getHeaders().set(HEADER_SERVICE_KEY, serviceKey);
            return execution.execute(request, body);
        });
    }
}
