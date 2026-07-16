package br.com.cloudport.monolitonavio.observabilidade;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
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

    public static final String HEADER_CORRELATION_ID = "X-Correlation-Id";
    private static final String MDC_CORRELATION_ID = "correlationId";
    private static final String MDC_TRACE_ID = "traceId";

    private final MeterRegistry meterRegistry;

    public CorrelationIdFilter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String correlationId = resolverCorrelationId(request.getHeader(HEADER_CORRELATION_ID));
        String traceId = resolverTraceId(request.getHeader("traceparent"), correlationId);
        long inicio = System.nanoTime();
        MDC.put(MDC_CORRELATION_ID, correlationId);
        MDC.put(MDC_TRACE_ID, traceId);
        response.setHeader(HEADER_CORRELATION_ID, correlationId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            long duracao = System.nanoTime() - inicio;
            Timer.builder("cloudport.http.server.requests")
                    .description("Tempo das requisições atendidas pelo monólito modular")
                    .tag("method", request.getMethod())
                    .tag("status", String.valueOf(response.getStatus()))
                    .register(meterRegistry)
                    .record(duracao, TimeUnit.NANOSECONDS);
            MDC.remove(MDC_CORRELATION_ID);
            MDC.remove(MDC_TRACE_ID);
        }
    }

    private String resolverCorrelationId(String recebido) {
        if (!StringUtils.hasText(recebido)) {
            return UUID.randomUUID().toString();
        }
        String normalizado = recebido.trim();
        return normalizado.length() <= 128
                ? normalizado
                : normalizado.substring(0, 128);
    }

    private String resolverTraceId(String traceparent, String fallback) {
        if (StringUtils.hasText(traceparent)) {
            String[] partes = traceparent.trim().split("-");
            if (partes.length >= 4 && partes[1].matches("[0-9a-fA-F]{32}")) {
                return partes[1].toLowerCase();
            }
        }
        return fallback.replace("-", "");
    }
}
