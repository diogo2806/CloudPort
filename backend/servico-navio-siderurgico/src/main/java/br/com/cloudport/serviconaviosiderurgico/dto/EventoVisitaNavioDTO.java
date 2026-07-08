package br.com.cloudport.serviconaviosiderurgico.dto;

import br.com.cloudport.serviconaviosiderurgico.dominio.EventoVisitaNavio;
import java.time.LocalDateTime;

public record EventoVisitaNavioDTO(
        Long id,
        Long visitaNavioId,
        Long itemOperacaoId,
        String tipoEvento,
        String descricao,
        String usuario,
        LocalDateTime criadoEm,
        String dadosAntes,
        String dadosDepois
) {
    public static EventoVisitaNavioDTO de(EventoVisitaNavio evento) {
        return new EventoVisitaNavioDTO(
                evento.getId(),
                evento.getVisitaNavio().getId(),
                evento.getItemOperacao() == null ? null : evento.getItemOperacao().getId(),
                evento.getTipoEvento(),
                evento.getDescricao(),
                evento.getUsuario(),
                evento.getCriadoEm(),
                evento.getDadosAntes(),
                evento.getDadosDepois()
        );
    }
}
