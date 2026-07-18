package br.com.cloudport.servicocargageral.dto;

import br.com.cloudport.servicocargageral.dominio.IdentificacaoCargoLot.TipoIdentificacao;
import java.time.OffsetDateTime;
import java.util.UUID;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public final class IdentificacaoCargoLotDTOs {

    private IdentificacaoCargoLotDTOs() {
    }

    public record RegistrarIdentificacaoRequest(
            @NotBlank @Size(max = 160) String codigo,
            @NotNull TipoIdentificacao tipo,
            @NotNull UUID loteId,
            @Size(max = 160) String embalagemReferencia,
            @NotBlank @Size(max = 120) String usuario) {
    }

    public record IdentificacaoResposta(
            UUID id,
            String codigo,
            TipoIdentificacao tipo,
            UUID loteId,
            String loteCodigo,
            String embalagemReferencia,
            boolean ativo,
            String registradoPor,
            OffsetDateTime registradoEm) {
    }
}
