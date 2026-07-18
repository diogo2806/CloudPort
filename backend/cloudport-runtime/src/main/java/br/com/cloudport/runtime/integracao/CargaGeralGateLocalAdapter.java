package br.com.cloudport.runtime.integracao;

import br.com.cloudport.servicocargageral.dominio.OperacoesIntermodaisTipos.EstagioGateCarga;
import br.com.cloudport.servicocargageral.dto.OperacoesIntermodaisDTOs;
import br.com.cloudport.servicocargageral.servico.OperacoesIntermodaisServico;
import br.com.cloudport.servicogate.integration.cargageral.CargaGeralGateCliente;
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
    public void confirmar(UUID reservaId, UUID commandId, String estagio, String usuario) {
        if (reservaId == null) {
            return;
        }
        servico.confirmarGate(
                reservaId,
                new OperacoesIntermodaisDTOs.ConfirmarGateCargaRequest(
                        commandId,
                        EstagioGateCarga.valueOf(estagio),
                        usuario));
    }
}
