package br.com.cloudport.servicoyard.container.servico;

import br.com.cloudport.servicoyard.container.dto.MovimentacaoNavioConcluidaEventoDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class MovimentacaoNavioListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(MovimentacaoNavioListener.class);

    private final ProcessadorMovimentacaoNavioService processadorMovimentacaoNavioService;

    public MovimentacaoNavioListener(ProcessadorMovimentacaoNavioService processadorMovimentacaoNavioService) {
        this.processadorMovimentacaoNavioService = processadorMovimentacaoNavioService;
    }

    @RabbitListener(queues = "${cloudport.yard.integracoes.navio.queue}")
    public void aoReceberMovimentacao(MovimentacaoNavioConcluidaEventoDto evento) {
        LOGGER.info("event=movimentacao_navio.recebida ordem={} escala={} conteiner={}",
                evento != null ? evento.getIdOrdemMovimentacao() : null,
                evento != null ? evento.getIdEscala() : null,
                evento != null ? evento.getCodigoConteiner() : null);
        processadorMovimentacaoNavioService.processar(evento);
    }
}
