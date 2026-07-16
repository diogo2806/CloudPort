package br.com.cloudport.serviconaviosiderurgico.configuracao;

import br.com.cloudport.contracts.api.ErroApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
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
        this.clientes = carregarClientes(clientesConfigurados);
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/public/v1/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (clientes.isEmpty()) {
            escreverErro(response, request, HttpStatus.SERVICE_UNAVAILABLE,
                    "CLIENTES_PUBLICOS_NAO_CONFIGURADOS",
                    "Nenhum cliente foi configurado para a API publica.");
            return;
        }

        String clientId = request.getHeader(HEADER_CLIENT_ID);
        String clientSecret = request.getHeader(HEADER_CLIENT_SECRET);
        String segredoEsperado = StringUtils.hasText(clientId) ? clientes.get(clientId.trim()) : null;
        if (!StringUtils.hasText(clientSecret) || segredoEsperado == null || !segredoValido(segredoEsperado, clientSecret)) {
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
        request.setAttribute(ATRIBUTO_CLIENT_ID, clientIdNormalizado);
        SecurityContextHolder.getContext().setAuthentication(autenticacao);
        filterChain.doFilter(request, response);
    }

    private Map<String, String> carregarClientes(String configuracao) {
        Map<String, String> resultado = new LinkedHashMap<>();
        if (!StringUtils.hasText(configuracao)) {
            return Map.of();
        }
        Arrays.stream(configuracao.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .forEach(entrada -> {
                    int separador = entrada.indexOf(':');
                    if (separador <= 0 || separador == entrada.length() - 1) {
                        throw new IllegalStateException(
                                "cloudport.security.public-api.clients deve usar o formato cliente:segredo."
                        );
                    }
                    String id = entrada.substring(0, separador).trim();
                    String segredo = entrada.substring(separador + 1).trim();
                    if (id.isBlank() || segredo.length() < 16) {
                        throw new IllegalStateException("Cliente publico invalido ou segredo com menos de 16 caracteres.");
                    }
                    resultado.put(id, segredo);
                });
        return Map.copyOf(resultado);
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
