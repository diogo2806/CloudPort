package br.com.cloudport.serviconaviosiderurgico.dto;

import br.com.cloudport.serviconaviosiderurgico.dominio.VisitaNavio;

public record VisitaPlanejamentoDTO(
        Long visitaNavioId,
        Long navioSiderurgicoId,
        Long navioCadastroId,
        String codigoVisita,
        String viagemEntrada,
        String viagemSaida,
        String fase,
        Long versao) {

    public static VisitaPlanejamentoDTO de(VisitaNavio visita) {
        return new VisitaPlanejamentoDTO(
                visita.getId(),
                visita.getNavio().getId(),
                visita.getNavio().getNavioCadastroId(),
                visita.getCodigoVisita(),
                visita.getViagemEntrada(),
                visita.getViagemSaida(),
                visita.getFase() == null ? null : visita.getFase().name(),
                visita.getVersao());
    }
}
