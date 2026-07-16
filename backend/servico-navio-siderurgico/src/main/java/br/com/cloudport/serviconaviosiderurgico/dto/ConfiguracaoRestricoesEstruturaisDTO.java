package br.com.cloudport.serviconaviosiderurgico.dto;

import br.com.cloudport.serviconaviosiderurgico.dominio.TipoCargaSiderurgica;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;

public record ConfiguracaoRestricoesEstruturaisDTO(
        @DecimalMin(value = "0.001", message = "O limite de peso por porao deve ser positivo.")
        BigDecimal limitePesoPorPoraoToneladas,
        @DecimalMin(value = "0.001", message = "O limite de peso por camada deve ser positivo.")
        BigDecimal limitePesoPorCamadaToneladas,
        @DecimalMin(value = "0.0", message = "O desequilibrio maximo nao pode ser negativo.")
        BigDecimal desequilibrioBombordoBoresteMaximoPercentual,
        @Min(value = 1, message = "A altura maxima deve ser maior que zero.")
        Integer alturaMaximaCamadas,
        @Min(value = 1, message = "A camada inicial de lashing deve ser maior que zero.")
        Integer exigirLashingAPartirCamada,
        Set<String> posicoesComLashing,
        Set<Integer> poroesInterditados,
        List<@Valid RegraSegregacaoDTO> regrasSegregacao
) {
    public record RegraSegregacaoDTO(
            TipoCargaSiderurgica tipoA,
            TipoCargaSiderurgica tipoB,
            boolean mesmoPoraoProibido,
            @Min(value = 0, message = "A distancia minima nao pode ser negativa.") int distanciaMinimaColunas
    ) {
    }
}
