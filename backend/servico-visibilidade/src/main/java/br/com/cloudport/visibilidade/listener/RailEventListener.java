package br.com.cloudport.visibilidade.listener;

import br.com.cloudport.visibilidade.config.RabbitMQConfig;
import br.com.cloudport.visibilidade.dto.evento.EventoMovimentacaoTremConcluidaMensagem;
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

    private final MovimentoConteinerService movimentoConteinerService;

    public RailEventListener(MovimentoConteinerService movimentoConteinerService) {
        this.movimentoConteinerService = movimentoConteinerService;
    }

    @RabbitListener(queues = RabbitMQConfig.VISIBILIDADE_RAIL_QUEUE)
    public void handleRailEvent(Map<String, Object> event) {
        String eventType = texto(event, "eventType");

        if ("rail.container.moved".equals(eventType)) {
            processarEventoLegado(event);
            return;
        }
        if ("rail.movimentacao.concluida".equals(eventType)
                || StringUtils.hasText(texto(event, "codigoConteiner"))) {
            processarEventoOperacional(event);
            return;
        }

        if (!StringUtils.hasText(eventType)) {
            LOGGER.warn("Evento ferroviario ignorado porque nao corresponde a um contrato conhecido.");
        } else {
            LOGGER.debug("Evento ferroviario sem processador registrado. eventType={}", eventType);
        }
    }

    private void processarEventoLegado(Map<String, Object> event) {
        String containerId = primeiroTexto(event, "containerId", "codigoConteiner");
        if (!StringUtils.hasText(containerId)) {
            LOGGER.warn("Evento rail.container.moved ignorado porque containerId nao foi informado.");
            return;
        }

        String origem = texto(event, "origem");
        String destino = texto(event, "destino");
        String equipamento = primeiroTexto(event, "equipamentoId", "equipamento", "locomotivaId");
        String responsavel = primeiroTexto(event, "responsavel", "usuario", "operatorId");

        movimentoConteinerService.registrarMovimentoRail(
                containerId, origem, destino, equipamento, responsavel);
        LOGGER.info("Movimento ferroviario legado registrado. containerId={} origem={} destino={}",
                containerId, origem, destino);
    }

    private void processarEventoOperacional(Map<String, Object> event) {
        EventoMovimentacaoTremConcluidaMensagem mensagem = new EventoMovimentacaoTremConcluidaMensagem();
        mensagem.setIdVisitaTrem(longo(event, "idVisitaTrem"));
        mensagem.setIdOrdemMovimentacao(longo(event, "idOrdemMovimentacao"));
        mensagem.setCodigoConteiner(primeiroTexto(event, "codigoConteiner", "containerId"));
        mensagem.setTipoMovimentacao(texto(event, "tipoMovimentacao"));
        mensagem.setConcluidoEm(dataHora(event, "concluidoEm"));
        mensagem.setStatusEvento(texto(event, "statusEvento"));

        if (!StringUtils.hasText(mensagem.getCodigoConteiner())) {
            LOGGER.warn("Evento rail.movimentacao.concluida ignorado porque codigoConteiner nao foi informado.");
            return;
        }

        movimentoConteinerService.registrarMovimentoFerroviario(mensagem);
        LOGGER.info("Movimento ferroviario real registrado. containerId={} visita={} ordem={} tipo={}",
                mensagem.getCodigoConteiner(), mensagem.getIdVisitaTrem(),
                mensagem.getIdOrdemMovimentacao(), mensagem.getTipoMovimentacao());
    }

    private OffsetDateTime dataHora(Map<String, Object> event, String chave) {
        String valor = texto(event, chave);
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
