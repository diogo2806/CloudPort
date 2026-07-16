package br.com.cloudport.serviconaviosiderurgico.configuracao;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class YardRestTemplateCustomizerContractTest {

    @AfterEach
    void limparMdc() {
        MDC.clear();
    }

    @Test
    void deveEnviarCredencialCorrelacaoETraceNoContratoLegadoComYard() {
        RestTemplate restTemplate = new RestTemplate();
        new YardRestTemplateCustomizer("service-key-test").customize(restTemplate);
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        MDC.put("correlationId", "corr-yard-001");
        MDC.put("traceId", "0123456789abcdef0123456789abcdef");
        MDC.put("spanId", "0123456789abcdef");

        server.expect(requestTo("http://yard.test/yard/patio/work-queues?visitaNavioId=42"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(YardRestTemplateCustomizer.HEADER_SERVICE_KEY, "service-key-test"))
                .andExpect(header(YardRestTemplateCustomizer.HEADER_CORRELATION_ID, "corr-yard-001"))
                .andExpect(header(YardRestTemplateCustomizer.HEADER_TRACE_ID,
                        "0123456789abcdef0123456789abcdef"))
                .andExpect(header(YardRestTemplateCustomizer.HEADER_TRACEPARENT,
                        "00-0123456789abcdef0123456789abcdef-0123456789abcdef-01"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        restTemplate.getForObject(
                "http://yard.test/yard/patio/work-queues?visitaNavioId=42",
                String.class
        );

        server.verify();
    }
}
