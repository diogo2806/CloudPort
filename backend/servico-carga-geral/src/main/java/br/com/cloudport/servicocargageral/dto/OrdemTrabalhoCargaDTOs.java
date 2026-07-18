package br.com.cloudport.servicocargageral.dto;

import br.com.cloudport.servicocargageral.dominio.StatusOrdemTrabalhoCarga;
import br.com.cloudport.servicocargageral.dominio.TipoEventoOrdemCarga;
import br.com.cloudport.servicocargageral.dominio.TipoServicoOrdemCarga;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;

public final class OrdemTrabalhoCargaDTOs {
    private OrdemTrabalhoCargaDTOs() { }

    public record CriarOrdemRequest(
            @NotBlank @Size(max = 80) String numero,
            @NotNull TipoServicoOrdemCarga tipo,
            @NotNull @Min(1) @Max(999) Integer prioridade,
            @NotNull OffsetDateTime janelaInicio,
            @NotNull OffsetDateTime janelaFim,
            @NotBlank @Size(max = 120) String local,
            @NotEmpty List<@Valid CriarItemRequest> itens) { }

    public record CriarItemRequest(
            @NotNull UUID loteId,
            @NotNull @Positive BigDecimal quantidade,
            @Size(max = 500) String observacao) { }

    public record AtribuirRecursosRequest(@Size(max = 80) String equipeId, @Size(max = 80) String equipamentoId) { }
    public record RegistrarEventoRequest(@NotBlank @Size(max = 1000) String descricao) { }
    public record CancelarOrdemRequest(@NotBlank @Size(max = 1000) String motivo) { }

    public record ItemResposta(UUID id, UUID loteId, String codigoLote, BigDecimal quantidade, String observacao) { }
    public record EventoResposta(UUID id, TipoEventoOrdemCarga tipo, String descricao, String usuario, OffsetDateTime ocorridoEm) { }
    public record OrdemResposta(UUID id, String numero, TipoServicoOrdemCarga tipo, StatusOrdemTrabalhoCarga status,
            Integer prioridade, OffsetDateTime janelaInicio, OffsetDateTime janelaFim, String local,
            String equipeId, String equipamentoId, String motivoCancelamento, Long versao,
            List<ItemResposta> itens, List<EventoResposta> eventos) { }
}
