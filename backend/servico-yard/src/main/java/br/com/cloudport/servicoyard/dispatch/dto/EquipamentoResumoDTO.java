package br.com.cloudport.servicoyard.dispatch.dto;

import br.com.cloudport.servicoyard.patio.modelo.StatusEquipamento;
import br.com.cloudport.servicoyard.patio.modelo.TipoEquipamento;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EquipamentoResumoDTO {
    private final Long id;
    private final String identificador;
    private final TipoEquipamento tipoEquipamento;
    private final StatusEquipamento statusOperacional;
}
