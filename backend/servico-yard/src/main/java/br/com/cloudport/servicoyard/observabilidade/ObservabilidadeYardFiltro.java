package br.com.cloudport.servicoyard.observabilidade;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class ObservabilidadeYardFiltro extends OncePerRequestFilter {

    public static final String HEADER_CORRELATION_ID = "X-Correlation-Id";
    public static final String HEADER_TRACE_ID = "X-Trace-Id";
    public static final String HEADER_TRACEPARENT = "traceparent";

    private static final Logger LOGGER = LoggerFactory.getLogger(ObservabilidadeYardFiltro.class);
    private static final Pattern TRACEPARENT_PATTERN = Pattern.compile(
            "^[0-9a-f]{2}-([0-9a-f]{32})-([0-9a-f]{16})-[0-9a-f]{2}$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern NUMERIC_PATH_PATTERN = Pattern.compile("(?<=/)\\d+(?=/|$)");
    private static final Pattern VISITA_PATTERN = Pattern.compile("(?:visita-navio/|visitaNavioId=)(\\d+)");
    private static final Pattern ITEM_PATTERN = Pattern.compile("(?:itens/|itemOperacaoNavioId=)(\\d+)");
    private static final Pattern RESERVA_PATTERN = Pattern.compile("/reservas/(\\d+)");
    private static final Pattern ORDEM_PATTERN = Pattern.compile("/(?:ordens|work-instructions)/(\\d+)");
    private static final Pattern FILA_PATTERN = Pattern.compile("/work-queues/(\\d+)");
    private static final Pattern EQUIPAMENTO_PATTERN = Pattern.compile("/equipamentos/([^/?]+)");

    private final MeterRegistry meterRegistry;
    private final ObjectMapper objectMapper;

    public ObservabilidadeYardFiltro(MeterRegistry meterRegistry, ObjectMapper objectMapper) {
        this.meterRegistry = meterRegistry;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Map<String, String> contextoAnterior = MDC.getCopyOfContextMap();
        String correlationId = sanitizar(request.getHeader(HEADER_CORRELATION_ID), UUID.randomUUID().toString());
        TraceContext trace = resolverTrace(request);
        String operacao = request.getMethod() + " " + NUMERIC_PATH_PATTERN.matcher(request.getRequestURI()).replaceAll("{id}");
        Map<String, String> ids = extrairIds(request);
        long inicio = System.nanoTime();
        Throwable erro = null;

        MDC.put("correlationId", correlationId);
        MDC.put("traceId", trace.traceId());
        MDC.put("spanId", trace.spanId());
        MDC.put("traceparent", trace.traceparent());
        MDC.put("modulo", "yard");
        MDC.put("operacao", operacao);
        ids.forEach(MDC::put);

        response.setHeader(HEADER_CORRELATION_ID, correlationId);
        response.setHeader(HEADER_TRACE_ID, trace.traceId());
        response.setHeader(HEADER_TRACEPARENT, trace.traceparent());

        try {
            filterChain.doFilter(request, response);
        } catch (IOException | ServletException | RuntimeException ex) {
            erro = ex;
            throw ex;
        } finally {
            long duracaoMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicio);
            int status = erro == null ? response.getStatus() : Math.max(500, response.getStatus());
            String resultado = status >= 500 ? "erro_servidor" : status >= 400 ? "erro_cliente" : "sucesso";
            String contexto = contextoMetrica(ids);

            meterRegistry.counter(
                    "cloudport.operacao.total",
                    "modulo", "yard",
                    "operacao", operacao,
                    "resultado", resultado,
                    "contexto", contexto
            ).increment();
            Timer.builder("cloudport.operacao.duracao")
                    .tags("modulo", "yard", "operacao", operacao, "resultado", resultado, "contexto", contexto)
                    .register(meterRegistry)
                    .record(duracaoMs, TimeUnit.MILLISECONDS);

            registrarLog(request, correlationId, trace, operacao, ids, status, resultado, duracaoMs, erro);
            restaurarMdc(contextoAnterior);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/swagger-ui")
                || uri.startsWith("/v3/api-docs")
                || uri.startsWith("/webjars/")
                || "/favicon.ico".equals(uri);
    }

    private TraceContext resolverTrace(HttpServletRequest request) {
        String traceparent = request.getHeader(HEADER_TRACEPARENT);
        if (StringUtils.hasText(traceparent)) {
            Matcher matcher = TRACEPARENT_PATTERN.matcher(traceparent.trim());
            if (matcher.matches()) {
                String traceId = matcher.group(1).toLowerCase(Locale.ROOT);
                String spanId = novoSpanId();
                return new TraceContext(traceId, spanId, "00-" + traceId + "-" + spanId + "-01");
            }
        }
        String recebido = request.getHeader(HEADER_TRACE_ID);
        String traceId = StringUtils.hasText(recebido) && recebido.matches("(?i)[0-9a-f]{32}")
                ? recebido.toLowerCase(Locale.ROOT)
                : UUID.randomUUID().toString().replace("-", "");
        String spanId = novoSpanId();
        return new TraceContext(traceId, spanId, "00-" + traceId + "-" + spanId + "-01");
    }

    private Map<String, String> extrairIds(HttpServletRequest request) {
        String alvo = request.getRequestURI() + "?" + request.getQueryString();
        Map<String, String> ids = new LinkedHashMap<>();
        adicionar(ids, "visitaId", buscar(alvo, VISITA_PATTERN,
                request.getHeader("X-CloudPort-Visit-Id"), request.getParameter("visitaNavioId")));
        adicionar(ids, "itemId", buscar(alvo, ITEM_PATTERN,
                request.getHeader("X-CloudPort-Item-Id"), request.getParameter("itemOperacaoNavioId")));
        adicionar(ids, "reservaId", buscar(alvo, RESERVA_PATTERN,
                request.getHeader("X-CloudPort-Reservation-Id"), request.getParameter("reservaId")));
        adicionar(ids, "ordemId", buscar(alvo, ORDEM_PATTERN,
                request.getHeader("X-CloudPort-Order-Id"), request.getParameter("ordemId")));
        adicionar(ids, "workQueueId", buscar(alvo, FILA_PATTERN,
                request.getHeader("X-CloudPort-Work-Queue-Id"), request.getParameter("workQueueId")));
        adicionar(ids, "equipamentoId", buscar(alvo, EQUIPAMENTO_PATTERN,
                request.getHeader("X-CloudPort-Equipment-Id"), request.getParameter("equipamento")));
        return ids;
    }

    private String buscar(String alvo, Pattern pattern, String... alternativas) {
        Matcher matcher = pattern.matcher(alvo);
        if (matcher.find()) {
            return sanitizar(matcher.group(1), null);
        }
        for (String alternativa : alternativas) {
            if (StringUtils.hasText(alternativa)) {
                return sanitizar(alternativa, null);
            }
        }
        return null;
    }

    private void adicionar(Map<String, String> ids, String chave, String valor) {
        if (StringUtils.hasText(valor)) {
            ids.put(chave, valor);
        }
    }

    private String contextoMetrica(Map<String, String> ids) {
        if (ids.containsKey("equipamentoId")) {
            return "equipamento";
        }
        if (ids.containsKey("workQueueId")) {
            return "fila";
        }
        if (ids.containsKey("ordemId")) {
            return "ordem";
        }
        if (ids.containsKey("reservaId")) {
            return "reserva";
        }
        if (ids.containsKey("itemId")) {
            return "item";
        }
        if (ids.containsKey("visitaId")) {
            return "visita";
        }
        return "geral";
    }

    private void registrarLog(HttpServletRequest request,
                              String correlationId,
                              TraceContext trace,
                              String operacao,
                              Map<String, String> ids,
                              int status,
                              String resultado,
                              long duracaoMs,
                              Throwable erro) {
        Map<String, Object> evento = new LinkedHashMap<>();
        evento.put("timestamp", Instant.now().toString());
        evento.put("evento", "cloudport.operacao.http");
        evento.put("correlationId", correlationId);
        evento.put("traceId", trace.traceId());
        evento.put("spanId", trace.spanId());
        evento.put("modulo", "yard");
        evento.put("operacao", operacao);
        evento.put("usuario", request.getUserPrincipal() == null ? "anonimo" : request.getUserPrincipal().getName());
        evento.putAll(ids);
        evento.put("statusHttp", status);
        evento.put("resultado", resultado);
        evento.put("duracaoMs", duracaoMs);
        if (erro != null) {
            evento.put("erroTipo", erro.getClass().getSimpleName());
            evento.put("erroMensagem", sanitizar(erro.getMessage(), "erro sem mensagem"));
        }
        String json = serializar(evento);
        if (erro == null) {
            LOGGER.info(json);
        } else {
            LOGGER.error(json, erro);
        }
    }

    private String serializar(Map<String, Object> evento) {
        try {
            return objectMapper.writeValueAsString(evento);
        } catch (JsonProcessingException ex) {
            return "{\"evento\":\"cloudport.operacao.http\",\"erro\":\"falha_serializacao_log\"}";
        }
    }

    private String sanitizar(String valor, String padrao) {
        if (!StringUtils.hasText(valor)) {
            return padrao;
        }
        String seguro = valor.trim().replaceAll("[\\r\\n\\t]", "_");
        return seguro.length() <= 120 ? seguro : seguro.substring(0, 120);
    }

    private String novoSpanId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private void restaurarMdc(Map<String, String> anterior) {
        MDC.clear();
        if (anterior != null && !anterior.isEmpty()) {
            MDC.setContextMap(anterior);
        }
    }

    private record TraceContext(String traceId, String spanId, String traceparent) {
    }
}
