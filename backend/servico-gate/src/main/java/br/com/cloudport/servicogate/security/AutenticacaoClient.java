package br.com.cloudport.servicogate.security;

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

    public AutenticacaoClient(RestTemplate restTemplate,
                              @Value("${cloudport.security.autenticacao.base-url}") String autenticacaoBaseUrl) {
        this.restTemplate = restTemplate;
        this.autenticacaoBaseUrl = autenticacaoBaseUrl;
    }

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
}
