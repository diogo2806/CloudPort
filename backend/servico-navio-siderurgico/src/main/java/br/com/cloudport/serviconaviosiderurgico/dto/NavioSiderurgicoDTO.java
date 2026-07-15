package br.com.cloudport.serviconaviosiderurgico.dto;

import br.com.cloudport.serviconaviosiderurgico.dominio.NavioSiderurgico;
import br.com.cloudport.serviconaviosiderurgico.dominio.StatusNavioSiderurgico;
import java.math.BigDecimal;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public record NavioSiderurgicoDTO(
        Long id,
        Long navioCadastroId,
        @NotBlank @Size(max = 120) String nome,
        @NotBlank @Size(max = 10) String codigoImo,
        @NotBlank @Size(max = 60) String paisBandeira,
        @NotBlank @Size(max = 80) String empresaArmadora,
        @NotBlank @Size(max = 40) String tipoNavio,
        BigDecimal loaMetros,
        BigDecimal dwtToneladas,
        @NotNull @Min(1) Integer quantidadePoroes,
        StatusNavioSiderurgico status
) {
    public static NavioSiderurgicoDTO de(NavioSiderurgico navio) {
        return new NavioSiderurgicoDTO(
                navio.getId(),
                navio.getNavioCadastroId(),
                navio.getNome(),
                navio.getCodigoImo(),
                navio.getPaisBandeira(),
                navio.getEmpresaArmadora(),
                navio.getTipoNavio(),
                navio.getLoaMetros(),
                navio.getDwtToneladas(),
                navio.getQuantidadePoroes(),
                navio.getStatus()
        );
    }
}
