package br.com.cloudport.servicoyard.container.servico;

import br.com.cloudport.servicoyard.container.dto.MovimentacaoTremConcluidaEventoDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class MovimentacaoTremListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(MovimentacaoTremListener.class);

    private final ProcessadorMovimentacaoTremService processadorMovimentacaoTremService;

    public MovimentacaoTremListener(ProcessadorMovimentacaoTremService processadorMovimentacaoTremService) {
        this.processadorMovimentacaoTremService = processadorMovimentacaoTremService;
    }

    @RabbitListener(queues = "${cloudport.yard.integracoes.ferrovia.queue}")
    public void aoReceberMovimentacao(MovimentacaoTremConcluidaEventoDto evento) {
        LOGGER.info("event=movimentacao_trem.recebida ordem={} visita={} conteiner={}",
                evento != null ? evento.getIdOrdemMovimentacao() : null,
                evento != null ? evento.getIdVisitaTrem() : null,
                evento != null ? evento.getCodigoConteiner() : null);
        processadorMovimentacaoTremService.processar(evento);
    }
}
