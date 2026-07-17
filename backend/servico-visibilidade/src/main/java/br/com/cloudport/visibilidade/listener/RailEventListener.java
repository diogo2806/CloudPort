package br.com.cloudport.visibilidade.listener;

import br.com.cloudport.visibilidade.config.RabbitMQConfig;
import br.com.cloudport.visibilidade.dto.evento.EventoMovimentacaoTremConcluidaMensagem;
import br.com.cloudport.visibilidade.dto.evento.EventoRecebido;
import br.com.cloudport.visibilidade.exception.EventoEnvelopeInvalidoException;
import br.com.cloudport.visibilidade.exception.EventoFerroviarioInvalidoException;
import br.com.cloudport.visibilidade.exception.EventoIdentidadeColidenteException;
import br.com.cloudport.visibilidade.service.EventoProcessadoService;
import br.com.cloudport.visibilidade.service.MovimentoConteinerService;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class RailEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(RailEventListener.class);
    private static final String CONSUMIDOR = "RAIL";
    private static final String EVENTO_LEGADO = "rail.container.moved";
    private static final String EVENTO_OPERACIONAL = "rail.movimentacao.concluida";

    private final MovimentoConteinerService movimentoConteinerService;
    private final EventoProcessadoService eventoProcessadoService;
    private final RabbitTemplate rabbitTemplate;
    private final String exchangeRejeitados;
    private final String routingRejeitados;

    public RailEventListener(MovimentoConteinerService movimentoConteinerService,
                             EventoProcessadoService eventoProcessadoService) {
        this(movimentoConteinerService, eventoProcessadoService, null,
                "visibilidade.rail.rejeitados", "rail.rejeitado");
    }

    @Autowired
    public RailEventListener(
            MovimentoConteinerService movimentoConteinerService,
            EventoProcessadoService eventoProcessadoService,
            RabbitTemplate rabbitTemplate,
            @Value("${cloudport.visibilidade.eventos.rail.rejeitados.exchange:visibilidade.rail.rejeitados}") String exchangeRejeitados,
            @Value("${cloudport.visibilidade.eventos.rail.rejeitados.routing:rail.rejeitado}") String routingRejeitados) {
        this.movimentoConteinerService = movimentoConteinerService;
        this.eventoProcessadoService = eventoProcessadoService;
        this.rabbitTemplate = rabbitTemplate;
        this.exchangeRejeitados = exchangeRejeitados;
        this.routingRejeitados = routingRejeitados;
    }

    @RabbitListener(queues = RabbitMQConfig.VISIBILIDADE_RAIL_QUEUE)
    public void handleRailEvent(Map<String, Object> envelope) {
        try {
            EventoRecebido evento = EventoRecebido.de(envelope);
            validarEvento(evento);
            boolean processado = eventoProcessadoService.processarUmaVez(
                    CONSUMIDOR, envelope, this::processarEvento);
            if (!processado) {
                LOGGER.info("Redelivery de evento ferroviario ignorado. identidade={}", evento.getIdentidade());
            }
        } catch (EventoEnvelopeInvalidoException
                 | EventoFerroviarioInvalidoException
                 | EventoIdentidadeColidenteException ex) {
            publicarRejeicao(envelope, ex);
        }
    }

    private void validarEvento(EventoRecebido evento) {
        if (EVENTO_LEGADO.equals(evento.getTipo())) {
            exigirTexto(evento.primeiroTexto("containerId", "codigoConteiner"), "containerId");
            return;
        }
        if (!EVENTO_OPERACIONAL.equals(evento.getTipo())) {
            throw new EventoFerroviarioInvalidoException(
                    "Tipo de evento ferroviario nao suportado: " + evento.getTipo());
        }

        exigirPositivo(evento.longo("idVisitaTrem"), "idVisitaTrem");
        exigirPositivo(evento.longo("idOrdemMovimentacao"), "idOrdemMovimentacao");
        exigirTexto(evento.primeiroTexto("codigoConteiner", "containerId"), "codigoConteiner");
        exigirTexto(evento.texto("tipoMovimentacao"), "tipoMovimentacao");
        exigirTexto(evento.texto("statusEvento"), "statusEvento");
        dataHoraObrigatoria(evento, "concluidoEm");
    }

    private void processarEvento(EventoRecebido evento) {
        if (EVENTO_LEGADO.equals(evento.getTipo())) {
            processarEventoLegado(evento);
            return;
        }
        processarEventoOperacional(evento);
    }

    private void processarEventoLegado(EventoRecebido evento) {
        String containerId = evento.primeiroTexto("containerId", "codigoConteiner");
        String origem = evento.texto("origem");
        String destino = evento.texto("destino");
        String equipamento = evento.primeiroTexto("equipamentoId", "equipamento", "locomotivaId");
        String responsavel = evento.primeiroTexto("responsavel", "usuario", "operatorId");

        movimentoConteinerService.registrarMovimentoRail(
                evento.getIdentidade(), containerId, origem, destino, equipamento, responsavel);
        LOGGER.info("Movimento ferroviario legado registrado. containerId={} origem={} destino={}",
                containerId, origem, destino);
    }

    private void processarEventoOperacional(EventoRecebido evento) {
        EventoMovimentacaoTremConcluidaMensagem mensagem = new EventoMovimentacaoTremConcluidaMensagem();
        mensagem.setIdVisitaTrem(evento.longo("idVisitaTrem"));
        mensagem.setIdOrdemMovimentacao(evento.longo("idOrdemMovimentacao"));
        mensagem.setCodigoConteiner(evento.primeiroTexto("codigoConteiner", "containerId"));
        mensagem.setTipoMovimentacao(evento.texto("tipoMovimentacao"));
        mensagem.setConcluidoEm(dataHoraObrigatoria(evento, "concluidoEm"));
        mensagem.setStatusEvento(evento.texto("statusEvento"));

        movimentoConteinerService.registrarMovimentoFerroviario(evento.getIdentidade(), mensagem);
        LOGGER.info("Movimento ferroviario real registrado. containerId={} visita={} ordem={} tipo={}",
                mensagem.getCodigoConteiner(), mensagem.getIdVisitaTrem(),
                mensagem.getIdOrdemMovimentacao(), mensagem.getTipoMovimentacao());
    }

    private OffsetDateTime dataHoraObrigatoria(EventoRecebido evento, String chave) {
        String valor = evento.texto(chave);
        exigirTexto(valor, chave);
        try {
            return OffsetDateTime.parse(valor);
        } catch (DateTimeParseException ex) {
            throw new EventoFerroviarioInvalidoException(
                    "O campo " + chave + " deve conter data e hora ISO-8601 valida.");
        }
    }

    private void exigirTexto(String valor, String campo) {
        if (!StringUtils.hasText(valor)) {
            throw new EventoFerroviarioInvalidoException(
                    "O campo " + campo + " e obrigatorio no evento ferroviario.");
        }
    }

    private void exigirPositivo(Long valor, String campo) {
        if (valor == null || valor <= 0) {
            throw new EventoFerroviarioInvalidoException(
                    "O campo " + campo + " deve ser um identificador positivo.");
        }
    }

    private void publicarRejeicao(Map<String, Object> envelope, RuntimeException ex) {
        if (rabbitTemplate == null) {
            throw ex;
        }
        Map<String, Object> rejeicao = new LinkedHashMap<>();
        rejeicao.put("rejectedAt", Instant.now().toString());
        rejeicao.put("consumer", CONSUMIDOR);
        rejeicao.put("classification", "PERMANENT_INVALID_EVENT");
        rejeicao.put("reason", ex.getMessage());
        rejeicao.put("exception", ex.getClass().getSimpleName());
        rejeicao.put("originalEnvelope", envelope);
        rabbitTemplate.convertAndSend(exchangeRejeitados, routingRejeitados, rejeicao);
        LOGGER.warn("Evento ferroviario rejeitado permanentemente. motivo={}", ex.getMessage());
    }
}
