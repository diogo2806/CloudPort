package br.com.cloudport.servicoyard.patio.purgatorio.servico;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class BloqueioDispatchPurgatorioAspecto {

    private final PurgatorioWorkInstructionServico servico;

    public BloqueioDispatchPurgatorioAspecto(PurgatorioWorkInstructionServico servico) {
        this.servico = servico;
    }

    @Before("execution(* br.com.cloudport.servicoyard.patio.listatrabalho.servico.WorkQueueOperacaoServico.despachar(..)) && args(workQueueId,..)")
    public void bloquearDispatchComCasoAberto(Long workQueueId) {
        servico.validarDispatch(workQueueId);
    }
}
