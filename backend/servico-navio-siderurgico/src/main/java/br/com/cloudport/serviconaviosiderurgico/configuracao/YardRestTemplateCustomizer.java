package br.com.cloudport.serviconaviosiderurgico.configuracao;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Component
public class YardRestTemplateCustomizer implements RestTemplateCustomizer {

    static final String HEADER_SERVICE_KEY = "X-CloudPort-Service-Key";
    static final String HEADER_CORRELATION_ID = "X-Correlation-Id";
    static final String HEADER_TRACE_ID = "X-Trace-Id";
    static final String HEADER_TRACEPARENT = "traceparent";

    private final String serviceKey;

    public YardRestTemplateCustomizer(
            @Value("${cloudport.security.internal-service-key:}") String serviceKey) {
        this.serviceKey = serviceKey;
    }

    @Override
    public void customize(RestTemplate restTemplate) {
        restTemplate.getInterceptors().add((request, body, execution) -> {
            HttpHeaders headers = request.getHeaders();
            definirSePresente(headers, HEADER_SERVICE_KEY, serviceKey);
            definirSePresente(headers, HEADER_CORRELATION_ID, MDC.get("correlationId"));
            definirSePresente(headers, HEADER_TRACE_ID, MDC.get("traceId"));

            String traceParent = MDC.get("traceparent");
            if (!StringUtils.hasText(traceParent)) {
                String traceId = MDC.get("traceId");
                String spanId = MDC.get("spanId");
                if (StringUtils.hasText(traceId) && StringUtils.hasText(spanId)) {
                    traceParent = "00-" + traceId + "-" + spanId + "-01";
                }
            }
            definirSePresente(headers, HEADER_TRACEPARENT, traceParent);
            return execution.execute(request, body);
        });
    }

    private void definirSePresente(HttpHeaders headers, String nome, String valor) {
        if (StringUtils.hasText(valor) && !headers.containsKey(nome)) {
            headers.set(nome, valor);
        }
    }
}
