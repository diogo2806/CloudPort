package br.com.cloudport.serviconaviosiderurgico.dto;

import br.com.cloudport.contracts.enums.StatusVisitaContrato;
import br.com.cloudport.serviconaviosiderurgico.dominio.VisitaNavio;
import java.time.LocalDateTime;

public record VisitaNavioResumoDTO(
        Long id,
        Long navioId,
        String navioNome,
        String codigoVisita,
        String viagemEntrada,
        String viagemSaida,
        String linhaOperadora,
        String bercoPrevisto,
        String bercoAtual,
        LocalDateTime eta,
        LocalDateTime etb,
        LocalDateTime etd,
        StatusVisitaContrato fase,
        LocalDateTime atualizadoEm
) {

    public static VisitaNavioResumoDTO de(VisitaNavio visita) {
        return new VisitaNavioResumoDTO(
                visita.getId(),
                visita.getNavio().getId(),
                visita.getNavio().getNome(),
                visita.getCodigoVisita(),
                visita.getViagemEntrada(),
                visita.getViagemSaida(),
                visita.getLinhaOperadora(),
                visita.getBercoPrevisto(),
                visita.getBercoAtual(),
                visita.getEta(),
                visita.getEtb(),
                visita.getEtd(),
                StatusVisitaContrato.valueOf(visita.getFase().name()),
                visita.getAtualizadoEm()
        );
    }
}
