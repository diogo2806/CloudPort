package br.com.cloudport.visibilidade.listener;

import br.com.cloudport.visibilidade.config.RabbitMQConfig;
import br.com.cloudport.visibilidade.dto.evento.EventoMovimentoPatioMensagem;
import br.com.cloudport.visibilidade.service.CapacidadeYardService;
import br.com.cloudport.visibilidade.service.MovimentoConteinerService;
import br.com.cloudport.visibilidade.service.ProcessamentoEventoIdempotenteService;
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
    private static final String ORIGEM = "YARD";

    private final MovimentoConteinerService movimentoConteinerService;
    private final CapacidadeYardService capacidadeYardService;
    private final ProcessamentoEventoIdempotenteService processamentoEventoService;

    public YardEventListener(MovimentoConteinerService movimentoConteinerService,
                             CapacidadeYardService capacidadeYardService,
                             ProcessamentoEventoIdempotenteService processamentoEventoService) {
        this.movimentoConteinerService = movimentoConteinerService;
        this.capacidadeYardService = capacidadeYardService;
        this.processamentoEventoService = processamentoEventoService;
    }

    @RabbitListener(queues = RabbitMQConfig.VISIBILIDADE_YARD_QUEUE)
    public void handleYardEvent(Map<String, Object> event) {
        String eventType = texto(event, "eventType");
        if (!StringUtils.hasText(eventType)) {
            throw new IllegalArgumentException("Evento de patio sem eventType.");
        }

        switch (eventType) {
            case "yard.container.stored":
                processarArmazenagem(event, eventType);
                break;
            case "yard.capacity_updated":
                processarCapacidade(event, eventType);
                break;
            case "yard.movimento.registrado":
                processarMovimentoOperacional(event, eventType);
                break;
            default:
                LOGGER.debug("Evento de patio sem processador registrado. eventType={}", eventType);
        }
    }

    private void processarArmazenagem(Map<String, Object> event, String eventType) {
        String containerId = primeiroTexto(event, "containerId", "codigoConteiner");
        if (!StringUtils.hasText(containerId)) {
            throw new IllegalArgumentException("Evento yard.container.stored sem containerId.");
        }

        String zona = texto(event, "zona");
        String posicao = texto(event, "posicao");
        String equipamento = primeiroTexto(event, "equipamentoId", "equipamento", "cheId");
        String responsavel = primeiroTexto(event, "responsavel", "usuario", "operatorId");

        boolean processado = processamentoEventoService.processarUmaVez(
                ORIGEM,
                eventType,
                event,
                eventoId -> movimentoConteinerService.registrarArmazenagemYard(
                        eventoId, containerId, zona, posicao, equipamento, responsavel));
        if (processado) {
            LOGGER.info("Armazenagem de conteiner registrada. containerId={} zona={} posicao={}",
                    containerId, zona, posicao);
        } else {
            LOGGER.debug("Redelivery de armazenagem ignorado. containerId={}", containerId);
        }
    }

    private void processarMovimentoOperacional(Map<String, Object> event, String eventType) {
        EventoMovimentoPatioMensagem mensagem = new EventoMovimentoPatioMensagem();
        mensagem.setCodigoConteiner(primeiroTexto(event, "codigoConteiner", "containerId"));
        mensagem.setTipoMovimento(texto(event, "tipoMovimento"));
        mensagem.setDescricao(texto(event, "descricao"));
        mensagem.setDestino(texto(event, "destino"));
        mensagem.setLinha(inteiro(event, "linha"));
        mensagem.setColuna(inteiro(event, "coluna"));
        mensagem.setCamadaOperacional(texto(event, "camadaOperacional"));
        mensagem.setRegistradoEm(dataHora(event, "registradoEm"));

        if (!StringUtils.hasText(mensagem.getCodigoConteiner())) {
            throw new IllegalArgumentException(
                    "Evento yard.movimento.registrado sem codigoConteiner.");
        }

        boolean processado = processamentoEventoService.processarUmaVez(
                ORIGEM,
                eventType,
                event,
                eventoId -> movimentoConteinerService.registrarMovimentoPatio(eventoId, mensagem));
        if (processado) {
            LOGGER.info("Movimento real de patio registrado. containerId={} tipo={} linha={} coluna={} camada={}",
                    mensagem.getCodigoConteiner(), mensagem.getTipoMovimento(), mensagem.getLinha(),
                    mensagem.getColuna(), mensagem.getCamadaOperacional());
        } else {
            LOGGER.debug("Redelivery de movimento de patio ignorado. containerId={}",
                    mensagem.getCodigoConteiner());
        }
    }

    private void processarCapacidade(Map<String, Object> event, String eventType) {
        String zona = texto(event, "zona");
        Integer ocupacaoAtual = inteiro(event, "ocupacaoAtual");

        if (!StringUtils.hasText(zona) || ocupacaoAtual == null) {
            throw new IllegalArgumentException(
                    "Evento yard.capacity_updated sem zona ou ocupacaoAtual valida.");
        }
        if (ocupacaoAtual < 0) {
            throw new IllegalArgumentException(
                    "Evento yard.capacity_updated com ocupacaoAtual negativa.");
        }

        boolean processado = processamentoEventoService.processarUmaVez(
                ORIGEM,
                eventType,
                event,
                eventoId -> capacidadeYardService.atualizarOcupacao(zona, ocupacaoAtual));
        if (processado) {
            LOGGER.info("Ocupacao do patio atualizada. zona={} ocupacaoAtual={}", zona, ocupacaoAtual);
        } else {
            LOGGER.debug("Redelivery de capacidade do patio ignorado. zona={}", zona);
        }
    }

    private LocalDateTime dataHora(Map<String, Object> event, String chave) {
        String valor = texto(event, chave);
        if (!StringUtils.hasText(valor)) {
            return null;
        }
        try {
            return LocalDateTime.parse(valor);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(
                    "Data invalida recebida no evento de patio. campo=" + chave + "; valor=" + valor,
                    ex);
        }
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
