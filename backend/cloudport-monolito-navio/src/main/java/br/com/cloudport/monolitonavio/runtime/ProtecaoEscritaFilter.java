package br.com.cloudport.monolitonavio.runtime;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class ProtecaoEscritaFilter extends OncePerRequestFilter {

    private final boolean escritaHabilitada;

    public ProtecaoEscritaFilter(
            @Value("${cloudport.runtime.writes-enabled:true}") boolean escritaHabilitada) {
        this.escritaHabilitada = escritaHabilitada;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!escritaHabilitada && metodoDeEscrita(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"codigo\":\"RUNTIME_SOMENTE_LEITURA\","
                    + "\"mensagem\":\"Comandos de escrita estão desabilitados neste runtime.\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean metodoDeEscrita(String metodo) {
        return "POST".equalsIgnoreCase(metodo)
                || "PUT".equalsIgnoreCase(metodo)
                || "PATCH".equalsIgnoreCase(metodo)
                || "DELETE".equalsIgnoreCase(metodo);
    }
}
