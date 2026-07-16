package br.com.cloudport.monolitonavio.integracao;

import br.com.cloudport.serviconaviosiderurgico.cliente.PosicaoPatioYardCliente;
import br.com.cloudport.servicoyard.patio.dto.PosicaoReservaPatioDto;
import br.com.cloudport.servicoyard.patio.servico.ConsultaReservaPatioServico;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "cloudport.modulo.yard.integracao", havingValue = "local")
public class PosicaoPatioLocalAdapter implements PosicaoPatioYardCliente {

    private final ConsultaReservaPatioServico consultaReservaPatioServico;

    public PosicaoPatioLocalAdapter(ConsultaReservaPatioServico consultaReservaPatioServico) {
        this.consultaReservaPatioServico = consultaReservaPatioServico;
    }

    @Override
    public List<PosicaoPatioYardDTO> listarPosicoes() {
        return consultaReservaPatioServico.listarPosicoesReservaveis().stream()
                .map(this::converter)
                .toList();
    }

    private PosicaoPatioYardDTO converter(PosicaoReservaPatioDto origem) {
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
        destino.setBloco(origem.getBloco());
        destino.setBloqueada(origem.isBloqueada());
        destino.setInterditada(origem.isInterditada());
        destino.setAreaPermitida(origem.isAreaPermitida());
        destino.setTiposCargaPermitidos(origem.getTiposCargaPermitidos());
        destino.setPesoMaximoToneladas(origem.getPesoMaximoToneladas());
        destino.setAlturaMaximaMetros(origem.getAlturaMaximaMetros());
        destino.setCamadaMaxima(origem.getCamadaMaxima());
        destino.setCapacidadePilha(origem.getCapacidadePilha());
        destino.setOcupacaoPilha(origem.getOcupacaoPilha());
        return destino;
    }
}
