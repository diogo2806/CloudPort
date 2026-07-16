package br.com.cloudport.runtime.integracao;

import br.com.cloudport.serviconaviosiderurgico.cliente.PosicaoPatioYardCliente;
import br.com.cloudport.servicoyard.patio.servico.ConsultaReservaPatioServico;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "cloudport.modulo.yard.integracao", havingValue = "local")
public class PosicaoPatioYardLocalAdapter extends PosicaoPatioYardCliente {

    private final ConsultaReservaPatioServico consultaReservaPatioServico;
    private final ObjectMapper objectMapper;

    public PosicaoPatioYardLocalAdapter(
            ConsultaReservaPatioServico consultaReservaPatioServico,
            ObjectMapper objectMapper) {
        super(new RestTemplateBuilder(), "http://yard-local.invalid");
        this.consultaReservaPatioServico = consultaReservaPatioServico;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<PosicaoPatioYardDTO> listarPosicoes() {
        JavaType tipo = objectMapper.getTypeFactory()
                .constructCollectionType(List.class, PosicaoPatioYardDTO.class);
        return objectMapper.convertValue(consultaReservaPatioServico.listarPosicoesReservaveis(), tipo);
    }
}
