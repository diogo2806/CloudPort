package br.com.cloudport.servicocargageral.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public final class AmarradoCargaDTOs {

    private AmarradoCargaDTOs() {
    }

    public record CriarAmarradoRequest(
            @NotBlank @Size(max = 100) String codigo,
            @NotBlank @Size(max = 80) String visitaNavioId,
            @NotEmpty @Size(max = 100) List<@NotNull UUID> loteIds) {
    }

    public record ReferenciaAmarradoResposta(
            UUID loteId,
            String loteCodigo,
            String conhecimentoNumero,
            int itemSequencia,
            String descricaoItem,
            String codigoArmazenagem) {
    }

    public record AmarradoResposta(
            UUID id,
            String codigo,
            String visitaNavioId,
            boolean misto,
            boolean integro,
            int quantidadeReferencias,
            List<String> gruposArmazenagem,
            List<ReferenciaAmarradoResposta> referencias,
            OffsetDateTime criadoEm,
            OffsetDateTime atualizadoEm) {
    }
}
