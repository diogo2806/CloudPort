package br.com.cloudport.serviconaviosiderurgico.dto;

import br.com.cloudport.serviconaviosiderurgico.dominio.FaseVisitaNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.StatusPlanoGuindaste;
import java.time.LocalDateTime;
import java.util.List;

public record QuayMonitorDTO(
        Long visitaNavioId,
        String codigoVisita,
        Long navioId,
        String navioNome,
        FaseVisitaNavio fase,
        String bercoPrevisto,
        String bercoAtual,
        LocalDateTime eta,
        LocalDateTime etb,
        LocalDateTime atb,
        LocalDateTime inicioOperacao,
        LocalDateTime fimOperacao,
        LocalDateTime etd,
        LocalDateTime atd,
        StatusPlanoGuindaste statusPlanoGuindaste,
        ProdutividadeCaisDTO produtividade,
        List<String> alertas
) {
}
