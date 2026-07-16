package br.com.cloudport.serviconaviosiderurgico.cliente;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Component
@ConditionalOnProperty(
        name = "cloudport.modulo.yard.integracao",
        havingValue = "http",
        matchIfMissing = true)
public class PlanoOtimizadoYardHttpAdapter implements PlanoOtimizadoYardCliente {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public PlanoOtimizadoYardHttpAdapter(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${cloudport.integracao.yard.base-url:http://localhost:8081}") String baseUrl
    ) {
        this.restTemplate = restTemplateBuilder.build();
        this.baseUrl = removerBarraFinal(baseUrl);
    }

    @Override
    public ResultadoAplicacaoPlanoYardDTO aplicar(AplicacaoPlanoYardDTO comando) {
        ResultadoAplicacaoPlanoYardDTO resultado = restTemplate.postForObject(
                baseUrl + "/yard/patio/planos-otimizados/aplicar",
                comando,
                ResultadoAplicacaoPlanoYardDTO.class);
        if (resultado == null) {
            throw new IllegalStateException("O Yard nao confirmou a aplicacao do plano otimizado.");
        }
        return resultado;
    }

    @Override
    public void compensar(
            String planoId,
            Long visitaNavioId,
            String usuario,
            String motivo,
            List<EstadoAnteriorOrdemYardDTO> estadosAnteriores
    ) {
        CompensacaoPlanoYardDTO comando = new CompensacaoPlanoYardDTO();
        comando.setPlanoId(planoId);
        comando.setVisitaNavioId(visitaNavioId);
        comando.setUsuario(usuario);
        comando.setMotivo(motivo);
        comando.setEstadosAnteriores(estadosAnteriores);
        ResponseEntity<Void> resposta = restTemplate.postForEntity(
                baseUrl + "/yard/patio/planos-otimizados/compensar",
                comando,
                Void.class);
        if (!resposta.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("O Yard nao confirmou a compensacao do plano otimizado.");
        }
    }

    private String removerBarraFinal(String valor) {
        if (!StringUtils.hasText(valor)) {
            return "http://localhost:8081";
        }
        return valor.endsWith("/") ? valor.substring(0, valor.length() - 1) : valor;
    }
}
