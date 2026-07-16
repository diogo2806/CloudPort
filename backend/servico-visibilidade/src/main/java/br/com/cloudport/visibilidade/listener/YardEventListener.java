package br.com.cloudport.visibilidade.listener;

import br.com.cloudport.visibilidade.config.RabbitMQConfig;
import br.com.cloudport.visibilidade.dto.evento.EventoMovimentoPatioMensagem;
import br.com.cloudport.visibilidade.dto.evento.EventoRecebido;
import br.com.cloudport.visibilidade.service.CapacidadeYardService;
import br.com.cloudport.visibilidade.service.EventoProcessadoService;
import br.com.cloudport.visibilidade.service.MovimentoConteinerService;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class YardEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(YardEventListener.class);
    private static final String CONSUMIDOR = "YARD";

    private final MovimentoConteinerService movimentoConteinerService;
    private final CapacidadeYardService capacidadeYardService;
    private final EventoProcessadoService eventoProcessadoService;

    public YardEventListener(MovimentoConteinerService movimentoConteinerService,
                             CapacidadeYardService capacidadeYardService,
                             EventoProcessadoService eventoProcessadoService) {
        this.movimentoConteinerService = movimentoConteinerService;
        this.capacidadeYardService = capacidadeYardService;
        this.eventoProcessadoService = eventoProcessadoService;
    }

    @RabbitListener(queues = RabbitMQConfig.VISIBILIDADE_YARD_QUEUE)
    public void handleYardEvent(Map<String, Object> event) {
        boolean processado = eventoProcessadoService.processarUmaVez(
                CONSUMIDOR, event, this::processarEvento);
        if (!processado) {
            LOGGER.info("Redelivery de evento do patio ignorado. identidade={}",
                    EventoRecebido.de(event).getIdentidade());
        }
    }

    private void processarEvento(EventoRecebido event) {
        String eventType = event.getTipo();

        if ("yard.container.stored".equals(eventType)) {
            processarArmazenagem(event);
            return;
        }
        if ("yard.capacity_updated".equals(eventType)) {
            processarCapacidade(event);
            return;
        }
        if ("yard.movimento.registrado".equals(eventType)
                || StringUtils.hasText(event.texto("codigoConteiner"))) {
            processarMovimentoOperacional(event);
            return;
        }

        LOGGER.debug("Evento de patio sem processador registrado. eventType={} identidade={}",
                eventType, event.getIdentidade());
    }

    private void processarArmazenagem(EventoRecebido event) {
        String containerId = event.primeiroTexto("containerId", "codigoConteiner");
        if (!StringUtils.hasText(containerId)) {
            LOGGER.warn("Evento yard.container.stored ignorado porque containerId nao foi informado.");
            return;
        }

        String zona = event.texto("zona");
        String posicao = event.texto("posicao");
        String equipamento = event.primeiroTexto("equipamentoId", "equipamento", "cheId");
        String responsavel = event.primeiroTexto("responsavel", "usuario", "operatorId");

        movimentoConteinerService.registrarArmazenagemYard(
                event.getIdentidade(), containerId, zona, posicao, equipamento, responsavel);
        LOGGER.info("Armazenagem de conteiner registrada. containerId={} zona={} posicao={}",
                containerId, zona, posicao);
    }

    private void processarMovimentoOperacional(EventoRecebido event) {
        EventoMovimentoPatioMensagem mensagem = new EventoMovimentoPatioMensagem();
        mensagem.setCodigoConteiner(event.primeiroTexto("codigoConteiner", "containerId"));
        mensagem.setTipoMovimento(event.texto("tipoMovimento"));
        mensagem.setDescricao(event.texto("descricao"));
        mensagem.setDestino(event.texto("destino"));
        mensagem.setLinha(event.inteiro("linha"));
        mensagem.setColuna(event.inteiro("coluna"));
        mensagem.setCamadaOperacional(event.texto("camadaOperacional"));
        mensagem.setRegistradoEm(dataHora(event, "registradoEm"));

        if (!StringUtils.hasText(mensagem.getCodigoConteiner())) {
            LOGGER.warn("Evento yard.movimento.registrado ignorado porque codigoConteiner nao foi informado.");
            return;
        }

        movimentoConteinerService.registrarMovimentoPatio(event.getIdentidade(), mensagem);
        LOGGER.info("Movimento real de patio registrado. containerId={} tipo={} linha={} coluna={} camada={}",
                mensagem.getCodigoConteiner(), mensagem.getTipoMovimento(), mensagem.getLinha(),
                mensagem.getColuna(), mensagem.getCamadaOperacional());
    }

    private void processarCapacidade(EventoRecebido event) {
        String zona = event.texto("zona");
        Integer ocupacaoAtual = event.inteiro("ocupacaoAtual");

        if (!StringUtils.hasText(zona) || ocupacaoAtual == null) {
            LOGGER.warn("Evento yard.capacity_updated ignorado por dados invalidos. zona={} ocupacaoAtual={}",
                    zona, event.valor("ocupacaoAtual"));
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

    private LocalDateTime dataHora(EventoRecebido event, String chave) {
        String valor = event.texto(chave);
        if (!StringUtils.hasText(valor)) {
            return null;
        }
        try {
            return LocalDateTime.parse(valor);
        } catch (DateTimeParseException ex) {
            LOGGER.warn("Data invalida recebida no evento de patio. campo={} valor={}", chave, valor);
            return null;
        }
    }
}
