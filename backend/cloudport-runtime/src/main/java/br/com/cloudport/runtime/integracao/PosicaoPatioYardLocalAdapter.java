package br.com.cloudport.runtime.integracao;

import br.com.cloudport.serviconaviosiderurgico.cliente.PosicaoPatioYardCliente;
import br.com.cloudport.servicoyard.patio.servico.MapaPatioServico;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "cloudport.modulo.yard.integracao", havingValue = "local")
public class PosicaoPatioYardLocalAdapter extends PosicaoPatioYardCliente {

    private final MapaPatioServico mapaPatioServico;
    private final ObjectMapper objectMapper;

    public PosicaoPatioYardLocalAdapter(MapaPatioServico mapaPatioServico, ObjectMapper objectMapper) {
        super(new RestTemplateBuilder(), "http://yard-local.invalid");
        this.mapaPatioServico = mapaPatioServico;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<PosicaoPatioYardDTO> listarPosicoes() {
        JavaType tipo = objectMapper.getTypeFactory()
                .constructCollectionType(List.class, PosicaoPatioYardDTO.class);
        return objectMapper.convertValue(mapaPatioServico.listarPosicoes(), tipo);
    }
}
