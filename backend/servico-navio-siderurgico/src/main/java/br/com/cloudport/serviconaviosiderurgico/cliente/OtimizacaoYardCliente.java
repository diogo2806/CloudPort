package br.com.cloudport.serviconaviosiderurgico.cliente;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Component
public class OtimizacaoYardCliente {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public OtimizacaoYardCliente(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${cloudport.integracao.yard.base-url:http://localhost:8081}") String baseUrl
    ) {
        this.restTemplate = restTemplateBuilder.build();
        this.baseUrl = removerBarraFinal(baseUrl);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> otimizar(Map<String, Object> requisicao) {
        Map<String, Object> resposta = restTemplate.postForObject(
                baseUrl + "/api/scheduler/gerar-plano",
                requisicao,
                Map.class
        );
        if (resposta == null) {
            throw new IllegalStateException("O scheduler do Yard nao retornou o plano otimizado.");
        }
        return Map.copyOf(resposta);
    }

    private String removerBarraFinal(String valor) {
        if (!StringUtils.hasText(valor)) {
            return "http://localhost:8081";
        }
        return valor.endsWith("/") ? valor.substring(0, valor.length() - 1) : valor;
    }
}
