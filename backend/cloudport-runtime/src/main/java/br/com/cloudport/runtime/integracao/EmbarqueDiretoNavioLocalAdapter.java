package br.com.cloudport.runtime.integracao;

import br.com.cloudport.servicogate.porta.navio.EmbarqueDiretoNavioPorta;
import br.com.cloudport.serviconavio.estiva.dto.EmbarqueDiretoGateResultadoDTO;
import br.com.cloudport.serviconavio.estiva.servico.PlanoEstivaServico;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "cloudport.modulo.navio.integracao",
        havingValue = "local")
public class EmbarqueDiretoNavioLocalAdapter implements EmbarqueDiretoNavioPorta {

    private final PlanoEstivaServico planoEstivaServico;

    public EmbarqueDiretoNavioLocalAdapter(PlanoEstivaServico planoEstivaServico) {
        this.planoEstivaServico = planoEstivaServico;
    }

    @Override
    public Resultado embarcar(Comando comando) {
        EmbarqueDiretoGateResultadoDTO resultado = planoEstivaServico.embarcarDiretoDoGate(
                comando.getAtribuicaoEstivaId(),
                comando.getCodigoConteiner(),
                comando.getEmbarcadoEm());
        return new Resultado(
                resultado.getAtribuicaoEstivaId(),
                resultado.getPlanoEstivaId(),
                resultado.getCodigoConteiner(),
                resultado.getBaia(),
                resultado.getFileira(),
                resultado.getCamada(),
                resultado.getEmbarcadoEm());
    }
}
