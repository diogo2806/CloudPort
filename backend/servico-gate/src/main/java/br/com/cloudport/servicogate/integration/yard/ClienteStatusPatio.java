package br.com.cloudport.servicogate.integration.yard;

import br.com.cloudport.servicogate.integration.yard.dto.StatusPatioResposta;
import java.util.Optional;

/** Porta usada pelo Gate para consultar a disponibilidade operacional do Yard. */
public interface ClienteStatusPatio {

    Optional<StatusPatioResposta> consultarStatus(String authorizationHeader);
}
