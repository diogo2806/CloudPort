package br.com.cloudport.runtime.integracao;

import br.com.cloudport.serviconaviosiderurgico.cliente.OtimizacaoYardCliente;
import br.com.cloudport.servicoyard.scheduler.dto.SchedulerPlanoOperacionalRequisicaoDto;
import br.com.cloudport.servicoyard.scheduler.dto.SchedulerResultDto;
import br.com.cloudport.servicoyard.scheduler.servico.PredictiveSchedulerService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "cloudport.modulo.yard.integracao", havingValue = "local")
public class OtimizacaoYardLocalAdapter implements OtimizacaoYardCliente {

    private static final TypeReference<Map<String, Object>> TIPO_MAPA = new TypeReference<>() {
    };

    private final PredictiveSchedulerService schedulerService;
    private final ObjectMapper objectMapper;

    public OtimizacaoYardLocalAdapter(
            PredictiveSchedulerService schedulerService,
            ObjectMapper objectMapper) {
        this.schedulerService = schedulerService;
        this.objectMapper = objectMapper;
    }

    @Override
    public Map<String, Object> otimizar(Map<String, Object> requisicao) {
        SchedulerPlanoOperacionalRequisicaoDto comando = objectMapper.convertValue(
                requisicao,
                SchedulerPlanoOperacionalRequisicaoDto.class
        );
        SchedulerResultDto resultado = schedulerService.gerarPlanoOperacional(comando);
        return objectMapper.convertValue(resultado, TIPO_MAPA);
    }
}
