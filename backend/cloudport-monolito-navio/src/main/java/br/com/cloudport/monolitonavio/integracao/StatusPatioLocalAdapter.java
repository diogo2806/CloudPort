package br.com.cloudport.monolitonavio.integracao;

import br.com.cloudport.servicogate.integration.yard.ClienteStatusPatio;
import br.com.cloudport.servicogate.integration.yard.dto.StatusPatioResposta;
import br.com.cloudport.servicogate.monitoring.IntegracaoDegradacaoHandler;
import br.com.cloudport.servicoyard.patio.dto.PosicaoPatioDto;
import br.com.cloudport.servicoyard.patio.servico.MapaPatioServico;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@ConditionalOnProperty(
        name = "cloudport.modulo.yard.status-integracao",
        havingValue = "local")
public class StatusPatioLocalAdapter extends ClienteStatusPatio {

    private final MapaPatioServico mapaPatioServico;

    public StatusPatioLocalAdapter(
            RestTemplate restTemplate,
            IntegracaoDegradacaoHandler degradacaoHandler,
            MapaPatioServico mapaPatioServico) {
        super(restTemplate, "http://yard-local.invalid", "/yard/status", degradacaoHandler,
                "Consultar o módulo local de pátio.");
        this.mapaPatioServico = mapaPatioServico;
    }

    @Override
    public Optional<StatusPatioResposta> consultarStatus(String authorizationHeader) {
        List<PosicaoPatioDto> posicoes = mapaPatioServico.listarPosicoes();
        long ocupadas = posicoes.stream().filter(PosicaoPatioDto::isOcupada).count();
        StatusPatioResposta resposta = new StatusPatioResposta();
        resposta.setStatus("OPERACIONAL");
        resposta.setDescricao("Yard local disponível: " + posicoes.size()
                + " posições cadastradas e " + ocupadas + " ocupadas.");
        resposta.setVerificadoEm(OffsetDateTime.now().toString());
        return Optional.of(resposta);
    }
}
