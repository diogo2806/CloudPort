package br.com.cloudport.monolitonavio.integracao;

import br.com.cloudport.serviconaviosiderurgico.cliente.ConsultaWorkQueueYardPorta;
import br.com.cloudport.serviconaviosiderurgico.cliente.WorkQueueValidacaoYardDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.WorkQueueValidacaoPlanoDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.servico.WorkQueueValidacaoPlanoServico;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "cloudport.modulo.yard.integracao", havingValue = "local")
public class ConsultaWorkQueueYardLocalAdapter implements ConsultaWorkQueueYardPorta {

    private final WorkQueueValidacaoPlanoServico servico;

    public ConsultaWorkQueueYardLocalAdapter(WorkQueueValidacaoPlanoServico servico) {
        this.servico = servico;
    }

    @Override
    public List<WorkQueueValidacaoYardDto> listarParaValidacaoPlano(Long visitaNavioId) {
        return servico.consultar(visitaNavioId).stream().map(this::converter).toList();
    }

    private WorkQueueValidacaoYardDto converter(WorkQueueValidacaoPlanoDto origem) {
        WorkQueueValidacaoYardDto destino = new WorkQueueValidacaoYardDto();
        destino.setId(origem.getId());
        destino.setVisitaNavioId(origem.getVisitaNavioId());
        destino.setIdentificador(origem.getIdentificador());
        destino.setBerco(origem.getBerco());
        destino.setPorao(origem.getPorao());
        destino.setStatus(origem.getStatus());
        destino.setPow(origem.getPow());
        destino.setPoolOperacional(origem.getPoolOperacional());
        destino.setEquipamentoPatioId(origem.getEquipamentoPatioId());
        destino.setEquipamentoIdentificador(origem.getEquipamentoIdentificador());
        destino.setEquipamentoTipo(origem.getEquipamentoTipo());
        destino.setEquipamentoStatus(origem.getEquipamentoStatus());
        destino.setPlanoGuindasteId(origem.getPlanoGuindasteId());
        destino.setRecursoCaisId(origem.getRecursoCaisId());
        destino.setTotalOrdens(origem.getTotalOrdens());
        destino.setTotalOrdensDispatchaveis(origem.getTotalOrdensDispatchaveis());
        destino.setCoberturaValida(origem.isCoberturaValida());
        return destino;
    }
}
