package br.com.cloudport.servicoyard.patio.dto;

import br.com.cloudport.servicoyard.patio.modelo.SeveridadeAvisoEstivagemPatio;
import br.com.cloudport.servicoyard.patio.modelo.TipoRegraEstivagemPatio;

public record ViolacaoEstivagemPatioDto(
        TipoRegraEstivagemPatio regra,
        SeveridadeAvisoEstivagemPatio severidade,
        String valorObservado,
        String valorEsperado,
        String acaoSugerida,
        boolean bloqueiaOperacao) {
}
