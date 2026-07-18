package br.com.cloudport.runtime.operacao;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class ProtecaoEscritaRuntimeFiltro extends OncePerRequestFilter {

    static final String HEADER_CORRELATION_ID = "X-Correlation-Id";
    private static final Set<String> METODOS_SEGUROS = Set.of("GET", "HEAD", "OPTIONS");

    private final boolean escritaHabilitada;
    private final ObjectMapper objectMapper;

    public ProtecaoEscritaRuntimeFiltro(
            @Value("${cloudport.runtime.writes-enabled:false}") boolean escritaHabilitada,
            ObjectMapper objectMapper) {
        this.escritaHabilitada = escritaHabilitada;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (escritaHabilitada || METODOS_SEGUROS.contains(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String correlationId = correlationId(request);
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader(HEADER_CORRELATION_ID, correlationId);

        Map<String, Object> erro = new LinkedHashMap<>();
        erro.put("codigo", "RUNTIME_SOMENTE_LEITURA");
        erro.put("mensagem", "Este runtime está em observação ou rollback e não aceita comandos de escrita.");
        erro.put("status", HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        erro.put("caminho", request.getRequestURI());
        erro.put("timestamp", Instant.now().toString());
        erro.put("correlationId", correlationId);
        objectMapper.writeValue(response.getWriter(), erro);
    }

    private String correlationId(HttpServletRequest request) {
        String recebido = request.getHeader(HEADER_CORRELATION_ID);
        if (!StringUtils.hasText(recebido)) {
            return UUID.randomUUID().toString();
        }
        String seguro = recebido.trim().replaceAll("[\\r\\n\\t]", "_");
        return seguro.length() <= 120 ? seguro : seguro.substring(0, 120);
    }
}
