package br.com.cloudport.servicoyard.edi.mensagem;

import br.com.cloudport.servicoyard.edi.dto.CoarriMensagemDto;
import br.com.cloudport.servicoyard.edi.dto.CoprarMensagemDto;
import br.com.cloudport.servicoyard.edi.servico.EdiProcessadorServico;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

/**
 * Consome mensagens COPRAR e COARRI do RabbitMQ em tempo real.
 *
 * Filas configuradas em EdiRabbitConfiguracao:
 *   edi.mensagens.coprar  → processa mudanças no plano de carga
 *   edi.mensagens.coarri  → processa confirmações de operação
 *
 * Cada mensagem processada atualiza o Bay Plan e dispara um evento
 * WebSocket em /topico/edi/bay-plan/{codigoNavio}.
 */
@Service
public class EdiMensagemListenerServico {

    private static final Logger log = LoggerFactory.getLogger(EdiMensagemListenerServico.class);

    private final EdiProcessadorServico processador;

    public EdiMensagemListenerServico(EdiProcessadorServico processador) {
        this.processador = processador;
    }

    @RabbitListener(
            queues = "${cloudport.yard.integracoes.edi.queue.coprar}",
            autoStartup = "${cloudport.runtime.consumers-enabled:true}")
    public void receberCoprar(CoprarMensagemDto dto) {
        try {
            log.info("COPRAR recebido: navio={} viagem={}",
                    dto.getCodigoNavio(), dto.getCodigoViagem());
            processador.processarCoprar(dto);
        } catch (Exception e) {
            log.error("Erro ao processar COPRAR navio={} viagem={}: {}",
                    dto.getCodigoNavio(), dto.getCodigoViagem(), e.getMessage(), e);
        }
    }

    @RabbitListener(
            queues = "${cloudport.yard.integracoes.edi.queue.coarri}",
            autoStartup = "${cloudport.runtime.consumers-enabled:true}")
    public void receberCoarri(CoarriMensagemDto dto) {
        try {
            log.info("COARRI recebido: navio={} viagem={}",
                    dto.getCodigoNavio(), dto.getCodigoViagem());
            processador.processarCoarri(dto);
        } catch (Exception e) {
            log.error("Erro ao processar COARRI navio={} viagem={}: {}",
                    dto.getCodigoNavio(), dto.getCodigoViagem(), e.getMessage(), e);
        }
    }
}
