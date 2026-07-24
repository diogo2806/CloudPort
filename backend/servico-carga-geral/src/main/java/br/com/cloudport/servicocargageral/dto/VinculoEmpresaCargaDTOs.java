package br.com.cloudport.servicocargageral.dto;

import br.com.cloudport.servicocargageral.dominio.Empresa.PapelEmpresa;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public final class VinculoEmpresaCargaDTOs {

    private VinculoEmpresaCargaDTOs() {
    }

    public record AtualizarVinculosRequest(
            @NotNull List<@Valid VinculoEmpresaRequest> vinculos) {
    }

    public record VinculoEmpresaRequest(
            @NotNull PapelEmpresa papel,
            @NotNull UUID empresaId) {
    }

    public record VinculoEmpresaResposta(
            PapelEmpresa papel,
            UUID empresaId,
            String codigo,
            String razaoSocial,
            String nomeFantasia,
            boolean ativa,
            Set<PapelEmpresa> papeis,
            String vinculadoPor,
            OffsetDateTime atualizadoEm) {
    }
}
