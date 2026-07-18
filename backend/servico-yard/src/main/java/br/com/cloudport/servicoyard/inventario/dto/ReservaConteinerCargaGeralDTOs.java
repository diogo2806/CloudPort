package br.com.cloudport.servicoyard.inventario.dto;

import java.util.UUID;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public final class ReservaConteinerCargaGeralDTOs {

    private ReservaConteinerCargaGeralDTOs() {
    }

    public enum ResultadoReserva {
        CONCLUIDA,
        CANCELADA
    }

    public record ReservarConteinerRequest(
            @NotNull UUID operacaoId,
            @NotBlank @Size(max = 120) String usuario) {
    }

    public record LiberarConteinerRequest(
            @NotBlank @Size(max = 120) String usuario,
            @Size(max = 1000) String motivo,
            @NotNull ResultadoReserva resultado) {
    }

    public record ConteinerInventarioResposta(
            Long unidadeId,
            String identificacao,
            String estado,
            String condicao,
            String posicaoAtual,
            UUID operacaoId,
            String statusReserva) {
    }
}
