package br.com.cloudport.visibilidade.listener;

import br.com.cloudport.visibilidade.config.RabbitMQConfig;
import br.com.cloudport.visibilidade.dto.evento.EventoMovimentacaoTremConcluidaMensagem;
import br.com.cloudport.visibilidade.dto.evento.EventoRecebido;
import br.com.cloudport.visibilidade.service.EventoProcessadoService;
import br.com.cloudport.visibilidade.service.MovimentoConteinerService;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class RailEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(RailEventListener.class);
    private static final String CONSUMIDOR = "RAIL";

    private final MovimentoConteinerService movimentoConteinerService;
    private final EventoProcessadoService eventoProcessadoService;

    public RailEventListener(MovimentoConteinerService movimentoConteinerService,
                             EventoProcessadoService eventoProcessadoService) {
        this.movimentoConteinerService = movimentoConteinerService;
        this.eventoProcessadoService = eventoProcessadoService;
    }

    @RabbitListener(queues = RabbitMQConfig.VISIBILIDADE_RAIL_QUEUE)
    public void handleRailEvent(Map<String, Object> event) {
        boolean processado = eventoProcessadoService.processarUmaVez(
                CONSUMIDOR, event, this::processarEvento);
        if (!processado) {
            LOGGER.info("Redelivery de evento ferroviario ignorado. identidade={}",
                    EventoRecebido.de(event).getIdentidade());
        }
    }

    private void processarEvento(EventoRecebido event) {
        String eventType = event.getTipo();

        if ("rail.container.moved".equals(eventType)) {
            processarEventoLegado(event);
            return;
        }
        if ("rail.movimentacao.concluida".equals(eventType)
                || StringUtils.hasText(event.texto("codigoConteiner"))) {
            processarEventoOperacional(event);
            return;
        }

        LOGGER.debug("Evento ferroviario sem processador registrado. eventType={} identidade={}",
                eventType, event.getIdentidade());
    }

    private void processarEventoLegado(EventoRecebido event) {
        String containerId = event.primeiroTexto("containerId", "codigoConteiner");
        if (!StringUtils.hasText(containerId)) {
            LOGGER.warn("Evento rail.container.moved ignorado porque containerId nao foi informado.");
            return;
        }

        String origem = event.texto("origem");
        String destino = event.texto("destino");
        String equipamento = event.primeiroTexto("equipamentoId", "equipamento", "locomotivaId");
        String responsavel = event.primeiroTexto("responsavel", "usuario", "operatorId");

        movimentoConteinerService.registrarMovimentoRail(
                event.getIdentidade(), containerId, origem, destino, equipamento, responsavel);
        LOGGER.info("Movimento ferroviario legado registrado. containerId={} origem={} destino={}",
                containerId, origem, destino);
    }

    private void processarEventoOperacional(EventoRecebido event) {
        EventoMovimentacaoTremConcluidaMensagem mensagem = new EventoMovimentacaoTremConcluidaMensagem();
        mensagem.setIdVisitaTrem(event.longo("idVisitaTrem"));
        mensagem.setIdOrdemMovimentacao(event.longo("idOrdemMovimentacao"));
        mensagem.setCodigoConteiner(event.primeiroTexto("codigoConteiner", "containerId"));
        mensagem.setTipoMovimentacao(event.texto("tipoMovimentacao"));
        mensagem.setConcluidoEm(dataHora(event, "concluidoEm"));
        mensagem.setStatusEvento(event.texto("statusEvento"));

        if (!StringUtils.hasText(mensagem.getCodigoConteiner())) {
            LOGGER.warn("Evento rail.movimentacao.concluida ignorado porque codigoConteiner nao foi informado.");
            return;
        }

        movimentoConteinerService.registrarMovimentoFerroviario(
                event.getIdentidade(), mensagem);
        LOGGER.info("Movimento ferroviario real registrado. containerId={} visita={} ordem={} tipo={}",
                mensagem.getCodigoConteiner(), mensagem.getIdVisitaTrem(),
                mensagem.getIdOrdemMovimentacao(), mensagem.getTipoMovimentacao());
    }

    private OffsetDateTime dataHora(EventoRecebido event, String chave) {
        String valor = event.texto(chave);
        if (!StringUtils.hasText(valor)) {
            return null;
        }
        try {
            return OffsetDateTime.parse(valor);
        } catch (DateTimeParseException ex) {
            LOGGER.warn("Data invalida recebida no evento ferroviario. campo={} valor={}", chave, valor);
            return null;
        }
    }
}
