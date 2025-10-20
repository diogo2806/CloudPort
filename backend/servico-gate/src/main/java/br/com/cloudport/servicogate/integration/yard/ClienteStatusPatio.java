package br.com.cloudport.servicogate.integration.yard;

import br.com.cloudport.servicogate.integration.yard.dto.StatusPatioResposta;
import br.com.cloudport.servicogate.monitoring.IntegracaoDegradacaoHandler;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.net.URI;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class ClienteStatusPatio {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClienteStatusPatio.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String statusPath;
    private final IntegracaoDegradacaoHandler degradacaoHandler;
    private final String fallbackOrientacao;

    public ClienteStatusPatio(RestTemplate restTemplate,
                              @Value("${cloudport.integracoes.yard.base-url:http://localhost:8083}") String baseUrl,
                              @Value("${cloudport.integracoes.yard.status-path:/yard/status}") String statusPath,
                              IntegracaoDegradacaoHandler degradacaoHandler,
                              @Value("${cloudport.integracoes.yard.fallback-orientacao:Consultar equipe de pátio para orientações manuais.}")
                                      String fallbackOrientacao) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.statusPath = statusPath;
        this.degradacaoHandler = degradacaoHandler;
        this.fallbackOrientacao = fallbackOrientacao;
    }

    @CircuitBreaker(name = "yardStatus", fallbackMethod = "fallbackConsultarStatus")
    public Optional<StatusPatioResposta> consultarStatus(String authorizationHeader) {
        if (!StringUtils.hasText(baseUrl)) {
            return Optional.empty();
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            if (StringUtils.hasText(authorizationHeader)) {
                headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader);
            }
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
                    .path(statusPath)
                    .build(true)
                    .toUri();
            ResponseEntity<StatusPatioResposta> resposta = restTemplate.exchange(uri, HttpMethod.GET, entity, StatusPatioResposta.class);
            return Optional.ofNullable(resposta.getBody());
        } catch (RestClientException ex) {
            LOGGER.debug("Falha ao consultar status do pátio", ex);
            return Optional.empty();
        }
    }

    @SuppressWarnings("unused")
    private Optional<StatusPatioResposta> fallbackConsultarStatus(String authorizationHeader, Throwable throwable) {
        degradacaoHandler.registrarDegradacao("servico-yard", "circuit-breaker", fallbackOrientacao);
        LOGGER.warn("event=yard.status.fallback orientacao=\"{}\" causa={}", fallbackOrientacao,
                throwable != null ? throwable.getMessage() : "indefinida");
        return Optional.empty();
    }
}
