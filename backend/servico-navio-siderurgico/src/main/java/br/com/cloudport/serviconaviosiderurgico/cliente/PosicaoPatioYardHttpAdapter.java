package br.com.cloudport.serviconaviosiderurgico.cliente;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Component
@ConditionalOnProperty(
        name = "cloudport.modulo.yard.integracao",
        havingValue = "http",
        matchIfMissing = true)
public class PosicaoPatioYardHttpAdapter implements PosicaoPatioYardCliente {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public PosicaoPatioYardHttpAdapter(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${cloudport.integracao.yard.base-url:http://localhost:8081}") String baseUrl) {
        this.restTemplate = restTemplateBuilder.build();
        this.baseUrl = removerBarraFinal(baseUrl);
    }

    @Override
    public List<PosicaoPatioYardDTO> listarPosicoes() {
        ResponseEntity<PosicaoPatioYardDTO[]> resposta = restTemplate.getForEntity(
                baseUrl + "/yard/patio/posicoes",
                PosicaoPatioYardDTO[].class);
        PosicaoPatioYardDTO[] corpo = resposta.getBody();
        if (corpo == null) {
            return List.of();
        }
        return Arrays.stream(corpo).filter(Objects::nonNull).toList();
    }

    private String removerBarraFinal(String valor) {
        if (!StringUtils.hasText(valor)) {
            return "http://localhost:8081";
        }
        return valor.endsWith("/") ? valor.substring(0, valor.length() - 1) : valor;
    }
}
