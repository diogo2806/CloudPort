package br.com.cloudport.serviconaviosiderurgico.observabilidade;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.io.IOException;
import java.security.Principal;
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
public class ObservabilidadeOperacionalFiltro extends OncePerRequestFilter {

    public static final String HEADER_CORRELATION_ID = "X-Correlation-Id";
    public static final String HEADER_TRACE_ID = "X-Trace-Id";
    public static final String HEADER_TRACEPARENT = "traceparent";

    private static final Logger LOGGER = LoggerFactory.getLogger(ObservabilidadeOperacionalFiltro.class);
    private static final Pattern TRACEPARENT_PATTERN = Pattern.compile(
            "^[0-9a-f]{2}-([0-9a-f]{32})-([0-9a-f]{16})-[0-9a-f]{2}$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern UUID_PATH_PATTERN = Pattern.compile(
            "(?i)[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}");
    private static final Pattern NUMERIC_PATH_PATTERN = Pattern.compile("(?<=/)\\d+(?=/|$)");
    private static final Pattern VISITA_PATTERN = Pattern.compile("/visitas-navio/(\\d+)");
    private static final Pattern ITEM_PATTERN = Pattern.compile("/itens/(\\d+)");
    private static final Pattern RESERVA_PATTERN = Pattern.compile("/reservas/(\\d+)");
    private static final Pattern ORDEM_PATTERN = Pattern.compile("/(?:ordens|work-instructions)/(\\d+)");
    private static final Pattern WORK_QUEUE_PATTERN = Pattern.compile("/work-queues/(\\d+)");
    private static final Pattern EQUIPAMENTO_PATTERN = Pattern.compile("/equipamentos/([^/?]+)");

    private final MeterRegistry meterRegistry;
    private final ObjectMapper objectMapper;

    public ObservabilidadeOperacionalFiltro(MeterRegistry meterRegistry, ObjectMapper objectMapper) {
        this.meterRegistry = meterRegistry;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Map<String, String> contextoAnterior = MDC.getCopyOfContextMap();
        String correlationId = valorSeguro(request.getHeader(HEADER_CORRELATION_ID), UUID.randomUUID().toString(), 100);
        TraceContext traceContext = resolverTraceContext(request);
        String modulo = resolverModulo(request.getRequestURI());
        String operacao = request.getMethod() + " " + normalizarRota(request.getRequestURI());
        Map<String, String> identificadores = extrairIdentificadores(request);
        String contextoMetrica = contextoMetrica(identificadores);
        long inicio = System.nanoTime();
        Throwable erro = null;

        preencherMdc(correlationId, traceContext, modulo, operacao, identificadores);
        response.setHeader(HEADER_CORRELATION_ID, correlationId);
        response.setHeader(HEADER_TRACE_ID, traceContext.traceId());
        response.setHeader(HEADER_TRACEPARENT, traceContext.traceparent());

        try {
            filterChain.doFilter(request, response);
        } catch (IOException | ServletException | RuntimeException ex) {
            erro = ex;
            throw ex;
        } finally {
            long duracaoMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicio);
            int status = erro == null ? response.getStatus() : Math.max(response.getStatus(), 500);
            String resultado = resultado(status);
            registrarMetricas(modulo, operacao, resultado, contextoMetrica, duracaoMs);
            registrarLog(request, correlationId, traceContext, modulo, operacao,
                    identificadores, status, resultado, duracaoMs, erro);
            restaurarMdc(contextoAnterior);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/assets/")
                || uri.startsWith("/static/")
                || "/favicon.ico".equals(uri);
    }

    private TraceContext resolverTraceContext(HttpServletRequest request) {
        String traceparentRecebido = request.getHeader(HEADER_TRACEPARENT);
        if (StringUtils.hasText(traceparentRecebido)) {
            Matcher matcher = TRACEPARENT_PATTERN.matcher(traceparentRecebido.trim());
            if (matcher.matches()) {
                String traceId = matcher.group(1).toLowerCase(Locale.ROOT);
                String spanId = novoSpanId();
                return new TraceContext(traceId, spanId, "00-" + traceId + "-" + spanId + "-01");
            }
        }

        String traceIdRecebido = request.getHeader(HEADER_TRACE_ID);
        String traceId = traceIdValido(traceIdRecebido) ? traceIdRecebido.toLowerCase(Locale.ROOT) : novoTraceId();
        String spanId = novoSpanId();
        return new TraceContext(traceId, spanId, "00-" + traceId + "-" + spanId + "-01");
    }

    private void preencherMdc(String correlationId,
                              TraceContext traceContext,
                              String modulo,
                              String operacao,
                              Map<String, String> identificadores) {
        MDC.put("correlationId", correlationId);
        MDC.put("traceId", traceContext.traceId());
        MDC.put("spanId", traceContext.spanId());
        MDC.put("traceparent", traceContext.traceparent());
        MDC.put("modulo", modulo);
        MDC.put("operacao", operacao);
        identificadores.forEach((chave, valor) -> {
            if (StringUtils.hasText(valor)) {
                MDC.put(chave, valor);
            }
        });
    }

    private void registrarMetricas(String modulo,
                                   String operacao,
                                   String resultado,
                                   String contexto,
                                   long duracaoMs) {
        meterRegistry.counter(
                "cloudport.operacao.total",
                "modulo", modulo,
                "operacao", operacao,
                "resultado", resultado,
                "contexto", contexto
        ).increment();
        Timer.builder("cloudport.operacao.duracao")
                .description("Duracao das operacoes HTTP do CloudPort")
                .tags(
                        "modulo", modulo,
                        "operacao", operacao,
                        "resultado", resultado,
                        "contexto", contexto
                )
                .register(meterRegistry)
                .record(duracaoMs, TimeUnit.MILLISECONDS);
    }

    private void registrarLog(HttpServletRequest request,
                              String correlationId,
                              TraceContext traceContext,
                              String modulo,
                              String operacao,
                              Map<String, String> identificadores,
                              int status,
                              String resultado,
                              long duracaoMs,
                              Throwable erro) {
        Map<String, Object> evento = new LinkedHashMap<>();
        evento.put("timestamp", Instant.now().toString());
        evento.put("evento", "cloudport.operacao.http");
        evento.put("correlationId", correlationId);
        evento.put("traceId", traceContext.traceId());
        evento.put("spanId", traceContext.spanId());
        evento.put("modulo", modulo);
        evento.put("operacao", operacao);
        evento.put("usuario", usuario(request));
        evento.putAll(identificadores);
        evento.put("statusHttp", status);
        evento.put("resultado", resultado);
        evento.put("duracaoMs", duracaoMs);
        if (erro != null) {
            evento.put("erroTipo", erro.getClass().getSimpleName());
            evento.put("erroMensagem", valorSeguro(erro.getMessage(), "erro sem mensagem", 500));
        }

        String json = serializar(evento);
        if (erro == null) {
            LOGGER.info(json);
        } else {
            LOGGER.error(json, erro);
        }
    }

    private Map<String, String> extrairIdentificadores(HttpServletRequest request) {
        String uri = request.getRequestURI();
        Map<String, String> identificadores = new LinkedHashMap<>();
        adicionar(identificadores, "visitaId", primeiro(uri, VISITA_PATTERN,
                request.getHeader("X-CloudPort-Visit-Id"), request.getParameter("visitaNavioId")));
        adicionar(identificadores, "itemId", primeiro(uri, ITEM_PATTERN,
                request.getHeader("X-CloudPort-Item-Id"), request.getParameter("itemId")));
        adicionar(identificadores, "reservaId", primeiro(uri, RESERVA_PATTERN,
                request.getHeader("X-CloudPort-Reservation-Id"), request.getParameter("reservaId")));
        adicionar(identificadores, "ordemId", primeiro(uri, ORDEM_PATTERN,
                request.getHeader("X-CloudPort-Order-Id"), request.getParameter("ordemId")));
        adicionar(identificadores, "workQueueId", primeiro(uri, WORK_QUEUE_PATTERN,
                request.getHeader("X-CloudPort-Work-Queue-Id"), request.getParameter("workQueueId")));
        adicionar(identificadores, "equipamentoId", primeiro(uri, EQUIPAMENTO_PATTERN,
                request.getHeader("X-CloudPort-Equipment-Id"), request.getParameter("equipamento")));
        return identificadores;
    }

    private String primeiro(String uri, Pattern pattern, String... alternativas) {
        Matcher matcher = pattern.matcher(uri);
        if (matcher.find()) {
            return valorSeguro(matcher.group(1), null, 120);
        }
        for (String alternativa : alternativas) {
            if (StringUtils.hasText(alternativa)) {
                return valorSeguro(alternativa, null, 120);
            }
        }
        return null;
    }

    private void adicionar(Map<String, String> identificadores, String chave, String valor) {
        if (StringUtils.hasText(valor)) {
            identificadores.put(chave, valor);
        }
    }

    private String contextoMetrica(Map<String, String> identificadores) {
        if (identificadores.containsKey("equipamentoId")) {
            return "equipamento";
        }
        if (identificadores.containsKey("workQueueId")) {
            return "fila";
        }
        if (identificadores.containsKey("ordemId")) {
            return "ordem";
        }
        if (identificadores.containsKey("reservaId")) {
            return "reserva";
        }
        if (identificadores.containsKey("itemId")) {
            return "item";
        }
        if (identificadores.containsKey("visitaId")) {
            return "visita";
        }
        return "geral";
    }

    private String resolverModulo(String uri) {
        if (uri.startsWith("/yard")) {
            return "yard";
        }
        if (uri.startsWith("/navios-siderurgicos") || uri.startsWith("/visitas-navio")) {
            return "navio-siderurgico";
        }
        if (uri.startsWith("/navios") || uri.startsWith("/escalas") || uri.startsWith("/planos-estiva")) {
            return "navio";
        }
        return "runtime";
    }

    private String normalizarRota(String uri) {
        String rota = UUID_PATH_PATTERN.matcher(uri).replaceAll("{id}");
        return NUMERIC_PATH_PATTERN.matcher(rota).replaceAll("{id}");
    }

    private String resultado(int status) {
        if (status >= 500) {
            return "erro_servidor";
        }
        if (status >= 400) {
            return "erro_cliente";
        }
        if (status >= 300) {
            return "redirecionamento";
        }
        return "sucesso";
    }

    private String usuario(HttpServletRequest request) {
        Principal principal = request.getUserPrincipal();
        return principal == null || !StringUtils.hasText(principal.getName()) ? "anonimo" : principal.getName();
    }

    private String serializar(Map<String, Object> evento) {
        try {
            return objectMapper.writeValueAsString(evento);
        } catch (JsonProcessingException ex) {
            return "{\"evento\":\"cloudport.operacao.http\",\"erro\":\"falha_serializacao_log\"}";
        }
    }

    private String valorSeguro(String valor, String padrao, int limite) {
        if (!StringUtils.hasText(valor)) {
            return padrao;
        }
        String seguro = valor.trim().replaceAll("[\\r\\n\\t]", "_");
        return seguro.length() <= limite ? seguro : seguro.substring(0, limite);
    }

    private boolean traceIdValido(String valor) {
        return StringUtils.hasText(valor) && valor.matches("(?i)[0-9a-f]{32}");
    }

    private String novoTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String novoSpanId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private void restaurarMdc(Map<String, String> contextoAnterior) {
        MDC.clear();
        if (contextoAnterior != null && !contextoAnterior.isEmpty()) {
            MDC.setContextMap(contextoAnterior);
        }
    }

    private record TraceContext(String traceId, String spanId, String traceparent) {
    }
}
