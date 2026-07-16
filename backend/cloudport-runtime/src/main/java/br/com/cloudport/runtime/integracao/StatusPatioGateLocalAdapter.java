package br.com.cloudport.runtime.integracao;

import br.com.cloudport.servicogate.integration.yard.ClienteStatusPatio;
import br.com.cloudport.servicogate.integration.yard.dto.StatusPatioResposta;
import br.com.cloudport.servicogate.monitoring.IntegracaoDegradacaoHandler;
import br.com.cloudport.servicoyard.patio.dto.StatusPatioDto;
import br.com.cloudport.servicoyard.patio.servico.StatusPatioServico;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@ConditionalOnProperty(
        name = "cloudport.modulo.yard.status-integracao",
        havingValue = "local")
public class StatusPatioGateLocalAdapter extends ClienteStatusPatio {

    private final StatusPatioServico statusPatioServico;

    public StatusPatioGateLocalAdapter(
            RestTemplate restTemplate,
            IntegracaoDegradacaoHandler degradacaoHandler,
            StatusPatioServico statusPatioServico) {
        super(restTemplate, "http://yard-local.invalid", "/yard/status", degradacaoHandler,
                "Consultar o módulo local de pátio.");
        this.statusPatioServico = statusPatioServico;
    }

    @Override
    public Optional<StatusPatioResposta> consultarStatus(String authorizationHeader) {
        StatusPatioDto origem = statusPatioServico.verificarDisponibilidade();
        StatusPatioResposta resposta = new StatusPatioResposta();
        resposta.setStatus(origem.getStatus() == null ? null : origem.getStatus().name());
        resposta.setDescricao(origem.getDescricao());
        resposta.setVerificadoEm(origem.getVerificadoEm() == null ? null : origem.getVerificadoEm().toString());
        return Optional.of(resposta);
    }
}
