package br.com.cloudport.servicoyard.scheduler.dto;

import br.com.cloudport.servicoyard.scheduler.modelo.EstadoPlanoPosicaoOperacional;
import br.com.cloudport.servicoyard.scheduler.modelo.HistoricoPlanoPosicaoOperacional;
import java.time.LocalDateTime;

public class HistoricoPlanoPosicaoOperacionalDto {

    private Long id;
    private EstadoPlanoPosicaoOperacional estadoAnterior;
    private EstadoPlanoPosicaoOperacional estadoNovo;
    private String motivo;
    private String operador;
    private Long versaoPlano;
    private LocalDateTime ocorridoEm;

    public static HistoricoPlanoPosicaoOperacionalDto deEntidade(HistoricoPlanoPosicaoOperacional entidade) {
        HistoricoPlanoPosicaoOperacionalDto dto = new HistoricoPlanoPosicaoOperacionalDto();
        dto.id = entidade.getId();
        dto.estadoAnterior = entidade.getEstadoAnterior();
        dto.estadoNovo = entidade.getEstadoNovo();
        dto.motivo = entidade.getMotivo();
        dto.operador = entidade.getOperador();
        dto.versaoPlano = entidade.getVersaoPlano();
        dto.ocorridoEm = entidade.getOcorridoEm();
        return dto;
    }

    public Long getId() { return id; }
    public EstadoPlanoPosicaoOperacional getEstadoAnterior() { return estadoAnterior; }
    public EstadoPlanoPosicaoOperacional getEstadoNovo() { return estadoNovo; }
    public String getMotivo() { return motivo; }
    public String getOperador() { return operador; }
    public Long getVersaoPlano() { return versaoPlano; }
    public LocalDateTime getOcorridoEm() { return ocorridoEm; }
}
