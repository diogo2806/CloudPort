package br.com.cloudport.serviconaviosiderurgico.configuracao;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Correlation-Id";
    public static final String ATRIBUTO = CorrelationIdFilter.class.getName() + ".correlationId";
    private static final Pattern VALOR_SEGURO = Pattern.compile("[A-Za-z0-9._:-]{1,100}");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String correlationId = normalizar(request.getHeader(HEADER));
        request.setAttribute(ATRIBUTO, correlationId);
        response.setHeader(HEADER, correlationId);
        MDC.put("correlationId", correlationId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("correlationId");
        }
    }

    public static String obter(HttpServletRequest request) {
        Object valor = request.getAttribute(ATRIBUTO);
        return valor == null ? null : valor.toString();
    }

    private String normalizar(String valor) {
        if (StringUtils.hasText(valor) && VALOR_SEGURO.matcher(valor.trim()).matches()) {
            return valor.trim();
        }
        return UUID.randomUUID().toString();
    }
}
