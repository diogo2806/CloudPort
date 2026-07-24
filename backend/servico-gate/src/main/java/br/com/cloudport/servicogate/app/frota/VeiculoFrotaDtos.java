package br.com.cloudport.servicogate.app.frota;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public final class VeiculoFrotaDtos {

    private VeiculoFrotaDtos() {
    }

    public record Salvar(
            @NotBlank @Size(max = 10) String placa,
            @Size(max = 10) String placaCarreta,
            @Size(max = 60) String modelo,
            @NotBlank @Size(max = 40) String tipo,
            @NotNull Long transportadoraId,
            Boolean ativo) {
    }

    public record Resposta(
            Long id,
            String placa,
            String placaCarreta,
            String modelo,
            String tipo,
            Long transportadoraId,
            String transportadoraNome,
            boolean ativo) {
    }
}
