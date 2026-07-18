package br.com.cloudport.runtime.integracao;

import br.com.cloudport.servicocargageral.integracao.yard.CapacidadeCargoLotCliente;
import br.com.cloudport.servicoyard.inventario.dto.CapacidadeCargoLotDTOs;
import br.com.cloudport.servicoyard.inventario.servico.CapacidadeCargoLotServico;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "cloudport.modulo.capacidade-carga-geral.integracao", havingValue = "local")
public class CapacidadeCargoLotLocalAdapter extends CapacidadeCargoLotCliente {

    private final CapacidadeCargoLotServico servico;

    public CapacidadeCargoLotLocalAdapter(CapacidadeCargoLotServico servico) {
        super(new RestTemplateBuilder(), "http://capacidade-local.invalid");
        this.servico = servico;
    }

    @Override
    public ReservaCapacidadeResposta reservar(
            String posicao,
            UUID commandId,
            UUID loteId,
            BigDecimal quantidade,
            BigDecimal volumeM3,
            BigDecimal pesoKg,
            String usuario) {
        return converter(servico.reservar(
                posicao,
                new CapacidadeCargoLotDTOs.ReservarCapacidadeRequest(
                        commandId,
                        loteId,
                        quantidade,
                        volumeM3,
                        pesoKg,
                        usuario)));
    }

    @Override
    public ReservaCapacidadeResposta confirmar(UUID reservaId, String usuario, String motivo) {
        return converter(servico.confirmar(
                reservaId,
                new CapacidadeCargoLotDTOs.ComandoCapacidadeRequest(usuario, motivo)));
    }

    @Override
    public ReservaCapacidadeResposta cancelar(UUID reservaId, String usuario, String motivo) {
        return converter(servico.cancelar(
                reservaId,
                new CapacidadeCargoLotDTOs.ComandoCapacidadeRequest(usuario, motivo)));
    }

    private ReservaCapacidadeResposta converter(
            CapacidadeCargoLotDTOs.ReservaCapacidadeResposta origem) {
        return new ReservaCapacidadeResposta(
                origem.id(),
                origem.commandId(),
                origem.loteId(),
                origem.posicao(),
                origem.quantidade(),
                origem.volumeM3(),
                origem.pesoKg(),
                origem.status().name(),
                origem.restricoes());
    }
}
