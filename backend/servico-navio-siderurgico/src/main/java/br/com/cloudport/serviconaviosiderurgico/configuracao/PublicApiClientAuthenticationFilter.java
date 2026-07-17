package br.com.cloudport.serviconaviosiderurgico.configuracao;

import br.com.cloudport.contracts.api.ErroApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class PublicApiClientAuthenticationFilter extends OncePerRequestFilter {

    public static final String HEADER_CLIENT_ID = "X-CloudPort-Client-Id";
    public static final String HEADER_CLIENT_SECRET = "X-CloudPort-Client-Secret";
    public static final String ATRIBUTO_CLIENT_ID = PublicApiClientAuthenticationFilter.class.getName() + ".clientId";

    private final Map<String, String> clientes;
    private final ObjectMapper objectMapper;

    public PublicApiClientAuthenticationFilter(
            @Value("${cloudport.security.public-api.clients:}") String clientesConfigurados,
            ObjectMapper objectMapper) {
        this.clientes = CredenciaisSegurancaValidator.carregarClientesPublicos(clientesConfigurados);
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return CorsUtils.isPreFlightRequest(request)
                || !request.getRequestURI().startsWith("/api/public/v1/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        SecurityContextHolder.clearContext();
        try {
            String clientId = request.getHeader(HEADER_CLIENT_ID);
            String clientSecret = request.getHeader(HEADER_CLIENT_SECRET);
            String segredoEsperado = StringUtils.hasText(clientId) ? clientes.get(clientId.trim()) : null;
            if (!StringUtils.hasText(clientSecret)
                    || segredoEsperado == null
                    || !segredoValido(segredoEsperado, clientSecret)) {
                escreverErro(response, request, HttpStatus.UNAUTHORIZED,
                        "CLIENTE_PUBLICO_INVALIDO",
                        "Credenciais do cliente ou aplicacao invalidas.");
                return;
            }

            String clientIdNormalizado = clientId.trim();
            UsernamePasswordAuthenticationToken autenticacao = new UsernamePasswordAuthenticationToken(
                    "client:" + clientIdNormalizado,
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_INTEGRACAO_EXTERNA"))
            );
            SecurityContext contexto = SecurityContextHolder.createEmptyContext();
            contexto.setAuthentication(autenticacao);
            request.setAttribute(ATRIBUTO_CLIENT_ID, clientIdNormalizado);
            SecurityContextHolder.setContext(contexto);
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private boolean segredoValido(String esperado, String informado) {
        return MessageDigest.isEqual(
                esperado.getBytes(StandardCharsets.UTF_8),
                informado.getBytes(StandardCharsets.UTF_8)
        );
    }

    private void escreverErro(HttpServletResponse response,
                              HttpServletRequest request,
                              HttpStatus status,
                              String codigo,
                              String mensagem) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        ErroApi erro = new ErroApi(
                codigo,
                mensagem,
                Map.of(),
                CorrelationIdFilter.obter(request),
                Instant.now()
        );
        objectMapper.writeValue(response.getOutputStream(), erro);
    }
}
