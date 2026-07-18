package br.com.cloudport.runtime.integracao;

import br.com.cloudport.servicocargageral.integracao.inventario.InventarioConteinerCliente;
import br.com.cloudport.servicoyard.inventario.dto.ReservaConteinerCargaGeralDTOs;
import br.com.cloudport.servicoyard.inventario.servico.ReservaConteinerCargaGeralServico;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "cloudport.modulo.inventario.integracao", havingValue = "local")
public class InventarioConteinerLocalAdapter extends InventarioConteinerCliente {

    private final ReservaConteinerCargaGeralServico servico;

    public InventarioConteinerLocalAdapter(ReservaConteinerCargaGeralServico servico) {
        super(new RestTemplateBuilder(), "http://inventario-local.invalid");
        this.servico = servico;
    }

    @Override
    public List<ConteinerInventarioResposta> listarElegiveis() {
        return servico.listarElegiveis().stream()
                .map(this::converter)
                .toList();
    }

    @Override
    public ConteinerInventarioResposta reservar(String identificacao, UUID operacaoId, String usuario) {
        return converter(servico.reservar(
                identificacao,
                new ReservaConteinerCargaGeralDTOs.ReservarConteinerRequest(operacaoId, usuario)));
    }

    @Override
    public ConteinerInventarioResposta liberar(
            UUID operacaoId,
            String usuario,
            String motivo,
            String resultado) {
        return converter(servico.liberar(
                operacaoId,
                new ReservaConteinerCargaGeralDTOs.LiberarConteinerRequest(
                        usuario,
                        motivo,
                        ReservaConteinerCargaGeralDTOs.ResultadoReserva.valueOf(resultado))));
    }

    private ConteinerInventarioResposta converter(
            ReservaConteinerCargaGeralDTOs.ConteinerInventarioResposta origem) {
        return new ConteinerInventarioResposta(
                origem.unidadeId(),
                origem.identificacao(),
                origem.estado(),
                origem.condicao(),
                origem.posicaoAtual(),
                origem.operacaoId(),
                origem.statusReserva());
    }
}
