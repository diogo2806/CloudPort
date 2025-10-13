package br.com.cloudport.servicogate.security;

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

@Component
public class AutenticacaoClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutenticacaoClient.class);

    private final RestTemplate restTemplate;
    private final String autenticacaoBaseUrl;
    private final IntegracaoDegradacaoHandler degradacaoHandler;
    private final String fallbackOrientacao;

    public AutenticacaoClient(RestTemplate restTemplate,
                              @Value("${cloudport.security.autenticacao.base-url}") String autenticacaoBaseUrl,
                              IntegracaoDegradacaoHandler degradacaoHandler,
                              @Value("${cloudport.security.autenticacao.fallback-orientacao:Validar credenciais manualmente com a equipe de segurança e registrar acessos temporários.}")
                                      String fallbackOrientacao) {
        this.restTemplate = restTemplate;
        this.autenticacaoBaseUrl = autenticacaoBaseUrl;
        this.degradacaoHandler = degradacaoHandler;
        this.fallbackOrientacao = fallbackOrientacao;
    }

    @CircuitBreaker(name = "autenticacao", fallbackMethod = "fallbackBuscarUsuario")
    public Optional<UserInfoResponse> buscarUsuario(String login, String authorizationHeader) {
        if (!StringUtils.hasText(autenticacaoBaseUrl) || !StringUtils.hasText(login)) {
            return Optional.empty();
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            if (StringUtils.hasText(authorizationHeader)) {
                headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader);
            }
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            URI uri = URI.create(String.format("%s/auth/usuarios/%s", autenticacaoBaseUrl, login));
            ResponseEntity<UserInfoResponse> response = restTemplate.exchange(uri, HttpMethod.GET, entity, UserInfoResponse.class);
            return Optional.ofNullable(response.getBody());
        } catch (RestClientException ex) {
            LOGGER.debug("Falha ao buscar usuário {} no serviço de autenticação", login, ex);
            return Optional.empty();
        }
    }

    @SuppressWarnings("unused")
    private Optional<UserInfoResponse> fallbackBuscarUsuario(String login, String authorizationHeader, Throwable throwable) {
        degradacaoHandler.registrarDegradacao("autenticacao", "circuit-breaker", fallbackOrientacao);
        LOGGER.warn("event=autenticacao.fallback login={} orientacao=\"{}\" causa={}",
                login, fallbackOrientacao, throwable != null ? throwable.getMessage() : "desconhecida");
        return Optional.empty();
    }
}
