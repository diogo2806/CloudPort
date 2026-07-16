package br.com.cloudport.serviconaviosiderurgico.dto;

import br.com.cloudport.serviconaviosiderurgico.dominio.ReservaPosicaoPatioNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.StatusReservaPatioNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.TipoReservaPatioNavio;
import java.time.LocalDateTime;

public record ReservaPatioNavioDTO(
        Long id,
        Long visitaNavioId,
        Long itemOperacaoNavioId,
        String posicaoPatioId,
        String bloco,
        Integer linha,
        Integer coluna,
        String camada,
        TipoReservaPatioNavio tipoReserva,
        StatusReservaPatioNavio status,
        String motivoCancelamento,
        LocalDateTime expiraEm,
        Long reservaAnteriorId,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm
) {
    public static ReservaPatioNavioDTO de(ReservaPosicaoPatioNavio reserva) {
        return new ReservaPatioNavioDTO(
                reserva.getId(),
                reserva.getVisitaNavioId(),
                reserva.getItemOperacaoNavioId(),
                reserva.getPosicaoPatioId(),
                reserva.getBloco(),
                reserva.getLinha(),
                reserva.getColuna(),
                reserva.getCamada(),
                reserva.getTipoReserva(),
                reserva.getStatus(),
                reserva.getMotivoCancelamento(),
                reserva.getExpiraEm(),
                reserva.getReservaAnteriorId(),
                reserva.getCriadoEm(),
                reserva.getAtualizadoEm()
        );
    }
}
