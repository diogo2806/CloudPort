package br.com.cloudport.serviconavio.configuracao;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(name = "cloudport.runtime.writes-enabled", havingValue = "false")
public class ModoEscritaNavioFiltro extends OncePerRequestFilter {

    private static final Set<String> METODOS_SOMENTE_LEITURA = Set.of("GET", "HEAD", "OPTIONS");
    private static final byte[] RESPOSTA = ("{\"codigo\":\"RUNTIME_LEGADO_SOMENTE_LEITURA\","
            + "\"mensagem\":\"O deployment legado de Navio esta em modo somente leitura durante o corte.\","
            + "\"detalhes\":\"Direcione comandos de escrita para o runtime monolitico.\"}")
            .getBytes(StandardCharsets.UTF_8);

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return METODOS_SOMENTE_LEITURA.contains(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", "30");
        response.getOutputStream().write(RESPOSTA);
    }
}
