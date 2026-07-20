package br.com.cloudport.servicoyard.patio.dispatch;

import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DispatchAutomaticoExecutor {

    private final DispatchDinamicoServico dispatchServico;

    public DispatchAutomaticoExecutor(DispatchDinamicoServico dispatchServico) {
        this.dispatchServico = dispatchServico;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<DispatchDtos.Decisao> despacharProxima(
            Long workQueueId,
            Long equipamentoId,
            String gatilho) {
        return dispatchServico.despacharProximaAutomaticamente(
                workQueueId, equipamentoId, gatilho);
    }
}
