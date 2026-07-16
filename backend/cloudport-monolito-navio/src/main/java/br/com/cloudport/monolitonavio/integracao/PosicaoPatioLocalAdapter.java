package br.com.cloudport.monolitonavio.integracao;

import br.com.cloudport.serviconaviosiderurgico.cliente.PosicaoPatioYardCliente;
import br.com.cloudport.servicoyard.patio.dto.PosicaoPatioDto;
import br.com.cloudport.servicoyard.patio.servico.MapaPatioServico;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "cloudport.modulo.yard.integracao", havingValue = "local")
public class PosicaoPatioLocalAdapter implements PosicaoPatioYardCliente {

    private final MapaPatioServico mapaPatioServico;

    public PosicaoPatioLocalAdapter(MapaPatioServico mapaPatioServico) {
        this.mapaPatioServico = mapaPatioServico;
    }

    @Override
    public List<PosicaoPatioYardDTO> listarPosicoes() {
        return mapaPatioServico.listarPosicoes().stream()
                .map(this::converter)
                .toList();
    }

    private PosicaoPatioYardDTO converter(PosicaoPatioDto origem) {
        PosicaoPatioYardDTO destino = new PosicaoPatioYardDTO();
        destino.setId(origem.getId());
        destino.setLinha(origem.getLinha());
        destino.setColuna(origem.getColuna());
        destino.setCamadaOperacional(origem.getCamadaOperacional());
        destino.setOcupada(origem.isOcupada());
        destino.setCodigoConteiner(origem.getCodigoConteiner());
        destino.setStatusConteiner(origem.getStatusConteiner() == null
                ? null
                : origem.getStatusConteiner().name());
        return destino;
    }
}
