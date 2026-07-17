package br.com.cloudport.servicogate.integration.alerta;

import java.time.Duration;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class WebhookAlertaOperacionalGateway implements AlertaOperacionalGateway {

    private final WebClient webClient;
    private final String webhookUrl;
    private final String bearerToken;
    private final Duration timeout;

    public WebhookAlertaOperacionalGateway(
            WebClient.Builder webClientBuilder,
            @Value("${cloudport.gate.alertas.webhook-url:}") String webhookUrl,
            @Value("${cloudport.gate.alertas.bearer-token:}") String bearerToken,
            @Value("${cloudport.gate.alertas.timeout:PT5S}") Duration timeout) {
        this.webClient = webClientBuilder.build();
        this.webhookUrl = webhookUrl;
        this.bearerToken = bearerToken;
        this.timeout = timeout;
    }

    @Override
    public ConfirmacaoEntregaAlerta enviar(AlertaReconciliacaoBarcode alerta) {
        if (!StringUtils.hasText(webhookUrl)) {
            throw new IllegalStateException("O webhook de alertas operacionais não está configurado.");
        }

        WebClient.RequestBodySpec requisicao = webClient.post()
                .uri(webhookUrl)
                .header("Idempotency-Key", alerta.getChaveIdempotencia())
                .header("X-CloudPort-Alert-Id", String.valueOf(alerta.getReconciliacaoId()));
        if (StringUtils.hasText(bearerToken)) {
            requisicao.header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken.trim());
        }

        ResponseEntity<Void> resposta = requisicao
                .bodyValue(alerta)
                .retrieve()
                .toBodilessEntity()
                .block(timeout);
        if (resposta == null || !resposta.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("O provedor não confirmou a entrega do alerta operacional.");
        }

        String identificadorExterno = resposta.getHeaders().getFirst("X-Delivery-Id");
        if (!StringUtils.hasText(identificadorExterno)) {
            identificadorExterno = resposta.getHeaders().getFirst("X-Request-Id");
        }
        if (!StringUtils.hasText(identificadorExterno)) {
            identificadorExterno = alerta.getChaveIdempotencia();
        }
        return new ConfirmacaoEntregaAlerta("WEBHOOK", identificadorExterno, LocalDateTime.now());
    }
}
