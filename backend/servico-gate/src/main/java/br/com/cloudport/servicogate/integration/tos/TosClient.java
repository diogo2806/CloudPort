package br.com.cloudport.servicogate.integration.tos;

import br.com.cloudport.servicogate.integration.tos.model.TosBookingResponse;
import br.com.cloudport.servicogate.integration.tos.model.TosContainerStatusResponse;
import br.com.cloudport.servicogate.integration.tos.model.TosCustomsReleaseResponse;
import br.com.cloudport.servicogate.contingencia.ContingenciaProperties;
import br.com.cloudport.servicogate.monitoring.IntegracaoDegradacaoHandler;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
public class TosClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(TosClient.class);

    private final WebClient webClient;
    private final TosProperties properties;
    private final Retry retry;
    private final CircuitBreaker circuitBreaker;
    private final IntegracaoDegradacaoHandler degradacaoHandler;
    private final ContingenciaProperties contingenciaProperties;

    public TosClient(WebClient tosWebClient,
                     TosProperties properties,
                     Retry tosRetry,
                     CircuitBreaker tosCircuitBreaker,
                     IntegracaoDegradacaoHandler degradacaoHandler,
                     ContingenciaProperties contingenciaProperties) {
        this.webClient = tosWebClient;
        this.properties = properties;
        this.retry = tosRetry;
        this.circuitBreaker = tosCircuitBreaker;
        this.degradacaoHandler = degradacaoHandler;
        this.contingenciaProperties = contingenciaProperties;
    }

    public TosBookingResponse buscarBooking(String bookingNumber) {
        Supplier<TosBookingResponse> supplier = () -> webClient.get()
                .uri(uriBuilder -> uriBuilder.path(properties.getApi().getBookingPath())
                        .build(bookingNumber))
                .retrieve()
                .bodyToMono(TosBookingResponse.class)
                .block(properties.getApi().getTimeout());
        return executarComRetry(supplier, "booking", bookingNumber);
    }

    public TosContainerStatusResponse buscarStatusContainer(String containerNumber) {
        Supplier<TosContainerStatusResponse> supplier = () -> webClient.get()
                .uri(uriBuilder -> uriBuilder.path(properties.getApi().getContainerStatusPath())
                        .build(containerNumber))
                .retrieve()
                .bodyToMono(TosContainerStatusResponse.class)
                .block(properties.getApi().getTimeout());
        return executarComRetry(supplier, "container-status", containerNumber);
    }

    public TosCustomsReleaseResponse buscarLiberacaoAduaneira(String containerNumber) {
        Supplier<TosCustomsReleaseResponse> supplier = () -> webClient.get()
                .uri(uriBuilder -> uriBuilder.path(properties.getApi().getCustomsReleasePath())
                        .build(containerNumber))
                .retrieve()
                .bodyToMono(TosCustomsReleaseResponse.class)
                .block(properties.getApi().getTimeout());
        return executarComRetry(supplier, "customs-release", containerNumber);
    }

    private <T> T executarComRetry(Supplier<T> supplier, String recurso, String identificador) {
        Supplier<T> decorated = Retry.decorateSupplier(retry, supplier);
        decorated = CircuitBreaker.decorateSupplier(circuitBreaker, decorated);
        try {
            T result = decorated.get();
            LOGGER.info("event=tos.call.success resource={} identifier={}", recurso, identificador);
            return result;
        } catch (CallNotPermittedException ex) {
            String orientacao = String.format("%s Utilize as rotas de contingência /gate/contingencia/agendar e /gate/contingencia/liberar enquanto o TOS estiver indisponível.",
                    contingenciaProperties.getOrientacaoOperador());
            degradacaoHandler.registrarDegradacao("tos", "circuit-breaker", orientacao);
            throw new TosIntegrationException("Circuit breaker do TOS aberto. " + orientacao, ex);
        } catch (Exception ex) {
            throw tratarExcecao(ex, recurso, identificador);
        }
    }

    private RuntimeException tratarExcecao(Exception ex, String recurso, String identificador) {
        if (ex instanceof WebClientResponseException) {
            WebClientResponseException responseException = (WebClientResponseException) ex;
            HttpStatus status = responseException.getStatusCode();
            String body = responseException.getResponseBodyAsString();
            LOGGER.error("event=tos.call.error resource={} identifier={} status={} body={}",
                    recurso, identificador, status.value(), body);
            String detalhes = Optional.ofNullable(body)
                    .filter(value -> !value.isBlank())
                    .orElse("Corpo vazio");
            return new TosIntegrationException(String.format("TOS respondeu %s para %s %s: %s",
                    status.getReasonPhrase(), recurso, identificador, detalhes));
        }
        if (ex instanceof WebClientRequestException) {
            LOGGER.error("event=tos.call.error resource={} identifier={} cause={}",
                    recurso, identificador, Objects.toString(ex.getMessage()));
            degradacaoHandler.registrarDegradacao("tos", "request-exception",
                    "Verificar conectividade com o TOS e acionar contingência se as chamadas continuarem falhando.");
            return new TosIntegrationException(String.format("Falha de comunicação com TOS ao acessar %s %s",
                    recurso, identificador), ex);
        }
        LOGGER.error("event=tos.call.error resource={} identifier={} cause={}",
                recurso, identificador, Objects.toString(ex.getMessage()));
        degradacaoHandler.registrarDegradacao("tos", "unexpected-error",
                "Investigar erro inesperado e seguir o playbook de contingência do TOS.");
        return new TosIntegrationException(String.format("Erro inesperado ao acessar TOS para %s %s",
                recurso, identificador), ex);
    }
}
