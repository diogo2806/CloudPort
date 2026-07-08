package br.com.cloudport.serviconaviosiderurgico.dto;

import br.com.cloudport.serviconaviosiderurgico.dominio.FaseVisitaNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.VisitaNavio;
import java.time.LocalDateTime;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public record VisitaNavioDTO(
        Long id,
        @NotNull(message = "Navio e obrigatorio.") Long navioId,
        String navioNome,
        @NotBlank(message = "Codigo da visita e obrigatorio.") String codigoVisita,
        String viagemEntrada,
        String viagemSaida,
        String linhaOperadora,
        String terminalFacility,
        String bercoPrevisto,
        String bercoAtual,
        LocalDateTime eta,
        LocalDateTime ata,
        LocalDateTime etb,
        LocalDateTime atb,
        LocalDateTime inicioOperacao,
        LocalDateTime fimOperacao,
        LocalDateTime etd,
        LocalDateTime atd,
        LocalDateTime janelaRecebimentoInicio,
        LocalDateTime janelaRecebimentoFim,
        LocalDateTime cutoffOperacional,
        FaseVisitaNavio fase,
        String observacoes,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm
) {
    public static VisitaNavioDTO de(VisitaNavio visita) {
        return new VisitaNavioDTO(
                visita.getId(),
                visita.getNavio().getId(),
                visita.getNavio().getNome(),
                visita.getCodigoVisita(),
                visita.getViagemEntrada(),
                visita.getViagemSaida(),
                visita.getLinhaOperadora(),
                visita.getTerminalFacility(),
                visita.getBercoPrevisto(),
                visita.getBercoAtual(),
                visita.getEta(),
                visita.getAta(),
                visita.getEtb(),
                visita.getAtb(),
                visita.getInicioOperacao(),
                visita.getFimOperacao(),
                visita.getEtd(),
                visita.getAtd(),
                visita.getJanelaRecebimentoInicio(),
                visita.getJanelaRecebimentoFim(),
                visita.getCutoffOperacional(),
                visita.getFase(),
                visita.getObservacoes(),
                visita.getCriadoEm(),
                visita.getAtualizadoEm()
        );
    }
}
