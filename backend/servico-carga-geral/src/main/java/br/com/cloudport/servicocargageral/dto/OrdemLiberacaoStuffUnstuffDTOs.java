package br.com.cloudport.servicocargageral.dto;

import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.TipoOperacaoStuffUnstuff;
import br.com.cloudport.servicocargageral.dto.StuffUnstuffDTOs.CriarItemOperacaoRequest;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public final class OrdemLiberacaoStuffUnstuffDTOs {

    private OrdemLiberacaoStuffUnstuffDTOs() {
    }

    public record OrigemOperacionalRequest(
            @NotNull TipoOrigemOperacional tipo,
            @NotBlank @Size(max = 120) String identificador,
            @Min(1) long versao,
            @NotNull @DecimalMin("0.001") BigDecimal quantidadeAutorizada,
            @NotNull OffsetDateTime vigenteDe,
            @NotNull OffsetDateTime vigenteAte,
            boolean hold,
            @NotBlank @Size(max = 4000) String snapshot) {
    }

    public record CriarOperacaoComLiberacaoRequest(
            @NotNull TipoOperacaoStuffUnstuff tipo,
            @NotBlank @Size(max = 80) String conteinerId,
            @Size(max = 80) String armazemId,
            @Size(max = 120) String posicaoOperacao,
            @Size(max = 120) String equipeRecurso,
            @Size(max = 80) String lacreInicial,
            @NotBlank @Size(max = 120) String usuario,
            @Size(max = 120) String correlationId,
            @Valid @NotNull OrigemOperacionalRequest origemOperacional,
            @Valid @NotEmpty List<CriarItemOperacaoRequest> itens) {
    }

    public enum TipoOrigemOperacional {
        BILL_OF_LADING,
        DELIVERY_ORDER,
        ORDEM_STUFFING,
        ORDEM_STRIPPING
    }
}
