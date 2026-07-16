package br.com.cloudport.servicoyard.patio.listatrabalho.dto;

import br.com.cloudport.servicoyard.patio.modelo.EquipamentoPatio;

public record EquipamentoOperacionalDto(
        Long id,
        String identificador,
        String tipo,
        String status,
        Integer linha,
        Integer coluna
) {
    public static EquipamentoOperacionalDto de(EquipamentoPatio equipamento) {
        return new EquipamentoOperacionalDto(
                equipamento.getId(),
                equipamento.getIdentificador(),
                equipamento.getTipoEquipamento().name(),
                equipamento.getStatusOperacional().name(),
                equipamento.getLinha(),
                equipamento.getColuna());
    }
}
