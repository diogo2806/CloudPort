package br.com.cloudport.visibilidade.listener;

import br.com.cloudport.visibilidade.config.RabbitMQConfig;
import br.com.cloudport.visibilidade.dto.evento.EventoMovimentacaoTremConcluidaMensagem;
import br.com.cloudport.visibilidade.service.MovimentoConteinerService;
import br.com.cloudport.visibilidade.service.ProcessamentoEventoIdempotenteService;
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
    private static final String ORIGEM = "RAIL";

    private final MovimentoConteinerService movimentoConteinerService;
    private final ProcessamentoEventoIdempotenteService processamentoEventoService;

    public RailEventListener(MovimentoConteinerService movimentoConteinerService,
                             ProcessamentoEventoIdempotenteService processamentoEventoService) {
        this.movimentoConteinerService = movimentoConteinerService;
        this.processamentoEventoService = processamentoEventoService;
    }

    @RabbitListener(queues = RabbitMQConfig.VISIBILIDADE_RAIL_QUEUE)
    public void handleRailEvent(Map<String, Object> event) {
        String eventType = texto(event, "eventType");
        if (!StringUtils.hasText(eventType)) {
            throw new IllegalArgumentException("Evento ferroviario sem eventType.");
        }

        switch (eventType) {
            case "rail.container.moved":
                processarEventoLegado(event, eventType);
                break;
            case "rail.movimentacao.concluida":
                processarEventoOperacional(event, eventType);
                break;
            default:
                LOGGER.debug("Evento ferroviario sem processador registrado. eventType={}", eventType);
        }
    }

    private void processarEventoLegado(Map<String, Object> event, String eventType) {
        String containerId = primeiroTexto(event, "containerId", "codigoConteiner");
        if (!StringUtils.hasText(containerId)) {
            throw new IllegalArgumentException("Evento rail.container.moved sem containerId.");
        }

        String origem = texto(event, "origem");
        String destino = texto(event, "destino");
        String equipamento = primeiroTexto(event, "equipamentoId", "equipamento", "locomotivaId");
        String responsavel = primeiroTexto(event, "responsavel", "usuario", "operatorId");

        boolean processado = processamentoEventoService.processarUmaVez(
                ORIGEM,
                eventType,
                event,
                eventoId -> movimentoConteinerService.registrarMovimentoRail(
                        eventoId, containerId, origem, destino, equipamento, responsavel));
        if (processado) {
            LOGGER.info("Movimento ferroviario legado registrado. containerId={} origem={} destino={}",
                    containerId, origem, destino);
        } else {
            LOGGER.debug("Redelivery ferroviario legado ignorado. containerId={}", containerId);
        }
    }

    private void processarEventoOperacional(Map<String, Object> event, String eventType) {
        EventoMovimentacaoTremConcluidaMensagem mensagem = new EventoMovimentacaoTremConcluidaMensagem();
        mensagem.setIdVisitaTrem(longo(event, "idVisitaTrem"));
        mensagem.setIdOrdemMovimentacao(longo(event, "idOrdemMovimentacao"));
        mensagem.setCodigoConteiner(primeiroTexto(event, "codigoConteiner", "containerId"));
        mensagem.setTipoMovimentacao(texto(event, "tipoMovimentacao"));
        mensagem.setConcluidoEm(dataHora(event, "concluidoEm"));
        mensagem.setStatusEvento(texto(event, "statusEvento"));

        if (!StringUtils.hasText(mensagem.getCodigoConteiner())) {
            throw new IllegalArgumentException(
                    "Evento rail.movimentacao.concluida sem codigoConteiner.");
        }

        boolean processado = processamentoEventoService.processarUmaVez(
                ORIGEM,
                eventType,
                event,
                eventoId -> movimentoConteinerService.registrarMovimentoFerroviario(
                        eventoId, mensagem));
        if (processado) {
            LOGGER.info("Movimento ferroviario real registrado. containerId={} visita={} ordem={} tipo={}",
                    mensagem.getCodigoConteiner(), mensagem.getIdVisitaTrem(),
                    mensagem.getIdOrdemMovimentacao(), mensagem.getTipoMovimentacao());
        } else {
            LOGGER.debug("Redelivery ferroviario ignorado. containerId={}",
                    mensagem.getCodigoConteiner());
        }
    }

    private OffsetDateTime dataHora(Map<String, Object> event, String chave) {
        String valor = texto(event, chave);
        if (!StringUtils.hasText(valor)) {
            return null;
        }
        try {
            return OffsetDateTime.parse(valor);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(
                    "Data invalida recebida no evento ferroviario. campo=" + chave + "; valor=" + valor,
                    ex);
        }
    }

    private Long longo(Map<String, Object> event, String chave) {
        if (event == null) {
            return null;
        }
        Object valor = event.get(chave);
        if (valor instanceof Number) {
            return ((Number) valor).longValue();
        }
        if (valor == null) {
            return null;
        }
        try {
            return Long.valueOf(String.valueOf(valor).trim());
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
