package br.com.cloudport.runtime.integracao;

import br.com.cloudport.serviconaviosiderurgico.cliente.ConsultaWorkQueueYardCliente;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.WorkQueueValidacaoPlanoDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.servico.WorkQueueOperacaoServico;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "cloudport.modulo.yard.integracao", havingValue = "local")
public class ConsultaWorkQueueYardLocalAdapter extends ConsultaWorkQueueYardCliente {

    private final WorkQueueOperacaoServico workQueueServico;

    public ConsultaWorkQueueYardLocalAdapter(WorkQueueOperacaoServico workQueueServico) {
        super(new RestTemplateBuilder(), "http://yard-local.invalid");
        this.workQueueServico = workQueueServico;
    }

    @Override
    public List<WorkQueueValidacaoYardDTO> listarParaValidacaoPlano(Long visitaNavioId) {
        return workQueueServico.consultarValidacaoPlano(visitaNavioId)
                .stream()
                .map(this::converter)
                .toList();
    }

    private WorkQueueValidacaoYardDTO converter(WorkQueueValidacaoPlanoDto origem) {
        WorkQueueValidacaoYardDTO destino = new WorkQueueValidacaoYardDTO();
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
