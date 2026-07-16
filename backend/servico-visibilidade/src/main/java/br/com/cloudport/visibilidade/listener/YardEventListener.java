package br.com.cloudport.visibilidade.listener;

import br.com.cloudport.visibilidade.config.RabbitMQConfig;
import br.com.cloudport.visibilidade.service.CapacidadeYardService;
import br.com.cloudport.visibilidade.service.MovimentoConteinerService;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class YardEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(YardEventListener.class);

    private final MovimentoConteinerService movimentoConteinerService;
    private final CapacidadeYardService capacidadeYardService;

    public YardEventListener(MovimentoConteinerService movimentoConteinerService,
                             CapacidadeYardService capacidadeYardService) {
        this.movimentoConteinerService = movimentoConteinerService;
        this.capacidadeYardService = capacidadeYardService;
    }

    @RabbitListener(queues = RabbitMQConfig.VISIBILIDADE_YARD_QUEUE)
    public void handleYardEvent(Map<String, Object> event) {
        String eventType = texto(event, "eventType");
        if (!StringUtils.hasText(eventType)) {
            LOGGER.warn("Evento de patio ignorado porque eventType nao foi informado.");
            return;
        }

        switch (eventType) {
            case "yard.container.stored":
                processarArmazenagem(event);
                break;
            case "yard.capacity_updated":
                processarCapacidade(event);
                break;
            default:
                LOGGER.debug("Evento de patio sem processador registrado. eventType={}", eventType);
        }
    }

    private void processarArmazenagem(Map<String, Object> event) {
        String containerId = texto(event, "containerId");
        if (!StringUtils.hasText(containerId)) {
            LOGGER.warn("Evento yard.container.stored ignorado porque containerId nao foi informado.");
            return;
        }

        String zona = texto(event, "zona");
        String posicao = texto(event, "posicao");
        String equipamento = primeiroTexto(event, "equipamentoId", "equipamento", "cheId");
        String responsavel = primeiroTexto(event, "responsavel", "usuario", "operatorId");

        movimentoConteinerService.registrarArmazenagemYard(
                containerId, zona, posicao, equipamento, responsavel);
        LOGGER.info("Armazenagem de conteiner registrada. containerId={} zona={} posicao={}",
                containerId, zona, posicao);
    }

    private void processarCapacidade(Map<String, Object> event) {
        String zona = texto(event, "zona");
        Integer ocupacaoAtual = inteiro(event, "ocupacaoAtual");

        if (!StringUtils.hasText(zona) || ocupacaoAtual == null) {
            LOGGER.warn("Evento yard.capacity_updated ignorado por dados invalidos. zona={} ocupacaoAtual={}",
                    zona, event == null ? null : event.get("ocupacaoAtual"));
            return;
        }

        if (ocupacaoAtual < 0) {
            LOGGER.warn("Evento yard.capacity_updated ignorado porque ocupacaoAtual e negativa. zona={} ocupacaoAtual={}",
                    zona, ocupacaoAtual);
            return;
        }

        capacidadeYardService.atualizarOcupacao(zona, ocupacaoAtual);
        LOGGER.info("Ocupacao do patio atualizada. zona={} ocupacaoAtual={}", zona, ocupacaoAtual);
    }

    private Integer inteiro(Map<String, Object> event, String chave) {
        if (event == null) {
            return null;
        }
        Object valor = event.get(chave);
        if (valor instanceof Number) {
            return ((Number) valor).intValue();
        }
        if (valor == null) {
            return null;
        }
        try {
            return Integer.valueOf(String.valueOf(valor).trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String primeiroTexto(Map<String, Object> event, String... chaves) {
        for (String chave : chaves) {
            String valor = texto(event, chave);
            if (StringUtils.hasText(valor)) {
                return valor;
            }
        }
        return null;
    }

    private String texto(Map<String, Object> event, String chave) {
        if (event == null) {
            return null;
        }
        Object valor = event.get(chave);
        return valor == null ? null : String.valueOf(valor).trim();
    }
}
