package br.com.cloudport.runtime.integracao;

import br.com.cloudport.serviconaviosiderurgico.cliente.PlanoOtimizadoYardCliente;
import br.com.cloudport.serviconaviosiderurgico.cliente.PlanoOtimizadoYardCliente.AplicacaoPlanoYardDTO;
import br.com.cloudport.serviconaviosiderurgico.cliente.PlanoOtimizadoYardCliente.CompensacaoPlanoYardDTO;
import br.com.cloudport.serviconaviosiderurgico.cliente.PlanoOtimizadoYardCliente.EstadoAnteriorOrdemYardDTO;
import br.com.cloudport.serviconaviosiderurgico.cliente.PlanoOtimizadoYardCliente.EstadoAnteriorWorkQueueYardDTO;
import br.com.cloudport.serviconaviosiderurgico.cliente.PlanoOtimizadoYardCliente.ResultadoAplicacaoPlanoYardDTO;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.AplicacaoPlanoOtimizadoPatioDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.CompensacaoPlanoOtimizadoPatioDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.ResultadoAplicacaoPlanoOtimizadoPatioDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.servico.PlanoOtimizadoPatioServico;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "cloudport.modulo.yard.integracao", havingValue = "local")
public class PlanoOtimizadoYardLocalAdapter implements PlanoOtimizadoYardCliente {

    private final PlanoOtimizadoPatioServico planoOtimizadoPatioServico;
    private final ObjectMapper objectMapper;

    public PlanoOtimizadoYardLocalAdapter(
            PlanoOtimizadoPatioServico planoOtimizadoPatioServico,
            ObjectMapper objectMapper
    ) {
        this.planoOtimizadoPatioServico = planoOtimizadoPatioServico;
        this.objectMapper = objectMapper;
    }

    @Override
    public ResultadoAplicacaoPlanoYardDTO aplicar(AplicacaoPlanoYardDTO comando) {
        AplicacaoPlanoOtimizadoPatioDto comandoLocal = objectMapper.convertValue(
                comando,
                AplicacaoPlanoOtimizadoPatioDto.class);
        ResultadoAplicacaoPlanoOtimizadoPatioDto resultado = planoOtimizadoPatioServico.aplicar(comandoLocal);
        return objectMapper.convertValue(resultado, ResultadoAplicacaoPlanoYardDTO.class);
    }

    @Override
    public void compensar(
            String planoId,
            Long visitaNavioId,
            String usuario,
            String motivo,
            List<EstadoAnteriorOrdemYardDTO> estadosAnteriores,
            List<EstadoAnteriorWorkQueueYardDTO> estadosAnterioresWorkQueues
    ) {
        CompensacaoPlanoYardDTO comando = new CompensacaoPlanoYardDTO();
        comando.setPlanoId(planoId);
        comando.setVisitaNavioId(visitaNavioId);
        comando.setUsuario(usuario);
        comando.setMotivo(motivo);
        comando.setEstadosAnteriores(estadosAnteriores);
        comando.setEstadosAnterioresWorkQueues(estadosAnterioresWorkQueues);
        CompensacaoPlanoOtimizadoPatioDto comandoLocal = objectMapper.convertValue(
                comando,
                CompensacaoPlanoOtimizadoPatioDto.class);
        planoOtimizadoPatioServico.compensar(comandoLocal);
    }
}
