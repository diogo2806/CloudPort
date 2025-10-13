package br.com.cloudport.servicogate.integration.hardware;

import br.com.cloudport.servicogate.dto.GateDecisionDTO;
import br.com.cloudport.servicogate.dto.GateFlowRequest;
import br.com.cloudport.servicogate.exception.BusinessException;
import br.com.cloudport.servicogate.exception.NotFoundException;
import br.com.cloudport.servicogate.model.enums.StatusGate;
import br.com.cloudport.servicogate.monitoring.GateMetrics;
import br.com.cloudport.servicogate.service.GateFlowService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class GateHardwareListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(GateHardwareListener.class);

    private final GateFlowService gateFlowService;
    private final GateHardwarePublisher publisher;
    private final GateMetrics gateMetrics;
    private final ObjectMapper objectMapper;

    public GateHardwareListener(GateFlowService gateFlowService,
                                GateHardwarePublisher publisher,
                                ObjectMapper objectMapper,
                                GateMetrics gateMetrics) {
        this.gateFlowService = gateFlowService;
        this.publisher = publisher;
        this.objectMapper = objectMapper;
        this.gateMetrics = gateMetrics;
    }

    @RabbitListener(queues = "${cloudport.gate.hardware.entrada-queue}")
    public void consumirEntrada(@Payload String payload) {
        processarMensagem(payload, true);
    }

    @RabbitListener(queues = "${cloudport.gate.hardware.saida-queue}")
    public void consumirSaida(@Payload String payload) {
        processarMensagem(payload, false);
    }

    private void processarMensagem(String payload, boolean entrada) {
        if (!StringUtils.hasText(payload)) {
            return;
        }
        boolean sucesso = false;
        try {
            HardwareEventMessage event = objectMapper.readValue(payload, HardwareEventMessage.class);
            GateFlowRequest request = event.toRequest();
            GateDecisionDTO decision = entrada
                    ? gateFlowService.registrarEntrada(request)
                    : gateFlowService.registrarSaida(request);
            publicarDecisao(decision, event, entrada);
            sucesso = true;
        } catch (BusinessException | NotFoundException ex) {
            LOGGER.warn("Falha ao processar evento de hardware: {}", ex.getMessage());
            publicarDecisao(GateDecisionDTO.negado(StatusGate.RETIDO, null, null, ex.getMessage()),
                    criarFallbackEvento(payload), entrada);
        } catch (Exception ex) {
            LOGGER.error("Erro inesperado ao processar evento do hardware", ex);
        } finally {
            gateMetrics.registrarConsumoFila(entrada ? "entrada" : "saida", sucesso);
        }
    }

    private void publicarDecisao(GateDecisionDTO decision, HardwareEventMessage event, boolean entrada) {
        if (entrada) {
            publisher.publicarDecisaoEntrada(decision, event);
        } else {
            publisher.publicarDecisaoSaida(decision, event);
        }
    }

    private HardwareEventMessage criarFallbackEvento(String payload) {
        HardwareEventMessage event = new HardwareEventMessage();
        event.setQrCode(payload);
        return event;
    }
}
