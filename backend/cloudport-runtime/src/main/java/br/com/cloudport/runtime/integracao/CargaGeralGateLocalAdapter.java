package br.com.cloudport.runtime.integracao;

import br.com.cloudport.servicocargageral.dominio.OperacoesIntermodaisTipos.EstagioGateCarga;
import br.com.cloudport.servicocargageral.dominio.OperacoesIntermodaisTipos.TipoMovimentoGateCarga;
import br.com.cloudport.servicocargageral.dto.OperacoesIntermodaisDTOs;
import br.com.cloudport.servicocargageral.dto.OperacoesIntermodaisDTOs.ReservaGateCargaResposta;
import br.com.cloudport.servicocargageral.servico.OperacoesIntermodaisServico;
import br.com.cloudport.servicogate.integration.cargageral.CargaGeralGateCliente;
import br.com.cloudport.servicogate.integration.cargageral.CargaGeralGatePorta.ReservaGateResposta;
import br.com.cloudport.servicogate.integration.cargageral.CargaGeralGatePorta.ReservarGateRequest;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "cloudport.modulo.gate-carga-geral.integracao", havingValue = "local")
public class CargaGeralGateLocalAdapter extends CargaGeralGateCliente {

    private final OperacoesIntermodaisServico servico;

    public CargaGeralGateLocalAdapter(OperacoesIntermodaisServico servico) {
        super(new RestTemplateBuilder(), "http://carga-geral-local.invalid");
        this.servico = servico;
    }

    @Override
    public ReservaGateResposta reservar(ReservarGateRequest request) {
        ReservaGateCargaResposta resposta = servico.reservarGate(
                new OperacoesIntermodaisDTOs.ReservarGateCargaRequest(
                        request.commandId(),
                        request.agendamentoCodigo(),
                        request.blNumero(),
                        request.deliveryOrder(),
                        request.loteId(),
                        TipoMovimentoGateCarga.valueOf(request.tipoMovimento()),
                        EstagioGateCarga.valueOf(request.estagioConfirmacao()),
                        request.quantidade(),
                        request.volumeM3(),
                        request.pesoKg(),
                        request.usuario()));
        return mapear(resposta);
    }

    @Override
    public ReservaGateResposta confirmar(UUID reservaId, UUID commandId, String estagio, String usuario) {
        if (reservaId == null) {
            return null;
        }
        ReservaGateCargaResposta resposta = servico.confirmarGate(
                reservaId,
                new OperacoesIntermodaisDTOs.ConfirmarGateCargaRequest(
                        commandId,
                        EstagioGateCarga.valueOf(estagio),
                        usuario));
        return mapear(resposta);
    }

    @Override
    public ReservaGateResposta compensar(
            UUID reservaId,
            UUID commandId,
            String motivo,
            String usuario) {
        if (reservaId == null) {
            return null;
        }
        ReservaGateCargaResposta resposta = servico.compensarGate(
                reservaId,
                new OperacoesIntermodaisDTOs.CompensarGateCargaRequest(
                        commandId,
                        motivo,
                        usuario));
        return mapear(resposta);
    }

    private ReservaGateResposta mapear(ReservaGateCargaResposta resposta) {
        return new ReservaGateResposta(
                resposta.id(),
                resposta.agendamentoCodigo(),
                resposta.status().name(),
                resposta.estagioConfirmacao().name());
    }
}
