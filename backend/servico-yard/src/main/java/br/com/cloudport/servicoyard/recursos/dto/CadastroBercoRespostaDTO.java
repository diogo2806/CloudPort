package br.com.cloudport.servicoyard.recursos.dto;

import br.com.cloudport.servicoyard.recursos.entidade.StatusBerco;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CadastroBercoRespostaDTO(
        Long id,
        String codigo,
        String nome,
        Integer comprimentoMetros,
        BigDecimal caladoMetros,
        Integer guinchesPermanentes,
        Integer capacidadeToneladasDia,
        String voltagem,
        boolean aguaPotavel,
        boolean energiaGenerica,
        boolean iluminacaoNoturna,
        boolean sistemaSeguranca,
        boolean cobertura,
        boolean compatContainer,
        boolean compatBreakbulk,
        boolean compatRoro,
        boolean compatCargaGeral,
        boolean compatReefer,
        boolean compatPerigosa,
        boolean compatGranel,
        String zonaPrimaria,
        String zonaSecundaria,
        Integer distanciaZonaMetros,
        Integer tempoTransporteMinutos,
        String diasOperacao,
        LocalDate ultimaManutencao,
        LocalDate proximaManutencao,
        StatusBerco status,
        String observacoes) {
}
