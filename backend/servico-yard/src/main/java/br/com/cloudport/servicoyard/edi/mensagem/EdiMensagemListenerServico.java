package br.com.cloudport.servicoyard.edi.mensagem;

import br.com.cloudport.servicoyard.edi.dto.CoarriMensagemDto;
import br.com.cloudport.servicoyard.edi.dto.CoprarMensagemDto;
import br.com.cloudport.servicoyard.edi.dto.ProcessamentoEdiRespostaDto;
import br.com.cloudport.servicoyard.edi.modelo.TipoMensagemEdi;
import br.com.cloudport.servicoyard.edi.servico.EdiAuditoriaServico;
import javax.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

/**
 * Registra mensagens COPRAR e COARRI recebidas pelo RabbitMQ na fila
 * transacional e idempotente de processamento EDI.
 *
 * Filas configuradas em EdiRabbitConfiguracao:
 *   edi.mensagens.coprar  → registra mudanças no plano de carga
 *   edi.mensagens.coarri  → registra confirmações de operação
 *
 * O efeito de negócio é executado exclusivamente por EdiProcessamentoWorker.
 * Exceções de recepção não são capturadas para impedir a confirmação da
 * mensagem pelo container RabbitMQ quando a persistência falhar.
 */
@Service
public class EdiMensagemListenerServico {

    private static final Logger log = LoggerFactory.getLogger(EdiMensagemListenerServico.class);

    private final EdiAuditoriaServico auditoria;

    public EdiMensagemListenerServico(EdiAuditoriaServico auditoria) {
        this.auditoria = auditoria;
    }

    @RabbitListener(
            queues = "${cloudport.yard.integracoes.edi.queue.coprar}",
            autoStartup = "${cloudport.runtime.consumers-enabled:true}")
    public void receberCoprar(@Valid CoprarMensagemDto dto) {
        ProcessamentoEdiRespostaDto processamento = auditoria.registrarRecebimento(
                TipoMensagemEdi.COPRAR,
                dto.getConteudoEdifact(),
                dto.getCodigoNavio(),
                dto.getCodigoViagem(),
                dto.getReferenciaMensagem(),
                null
        );
        log.info("COPRAR registrado: navio={} viagem={} processamentoId={}",
                dto.getCodigoNavio(), dto.getCodigoViagem(), processamento.id());
    }

    @RabbitListener(
            queues = "${cloudport.yard.integracoes.edi.queue.coarri}",
            autoStartup = "${cloudport.runtime.consumers-enabled:true}")
    public void receberCoarri(@Valid CoarriMensagemDto dto) {
        ProcessamentoEdiRespostaDto processamento = auditoria.registrarRecebimento(
                TipoMensagemEdi.COARRI,
                dto.getConteudoEdifact(),
                dto.getCodigoNavio(),
                dto.getCodigoViagem(),
                dto.getReferenciaMensagem(),
                null
        );
        log.info("COARRI registrado: navio={} viagem={} processamentoId={}",
                dto.getCodigoNavio(), dto.getCodigoViagem(), processamento.id());
    }
}
