package br.com.cloudport.monolitonavio.integracao;

import br.com.cloudport.servicogate.integration.yard.ClienteStatusPatio;
import br.com.cloudport.servicogate.integration.yard.dto.StatusPatioResposta;
import br.com.cloudport.servicoyard.patio.dto.PosicaoPatioDto;
import br.com.cloudport.servicoyard.patio.servico.MapaPatioServico;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "cloudport.modulo.yard.integracao", havingValue = "local")
public class StatusPatioLocalAdapter implements ClienteStatusPatio {

    private final MapaPatioServico mapaPatioServico;

    public StatusPatioLocalAdapter(MapaPatioServico mapaPatioServico) {
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
