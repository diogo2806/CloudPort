package br.com.cloudport.servicoyard.scheduler.modelo;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.Table;

@Entity
@Table(name = "historico_plano_posicao_operacional")
public class HistoricoPlanoPosicaoOperacional {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plano_id", nullable = false)
    private Long planoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", length = 20)
    private EstadoPlanoPosicaoOperacional estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_novo", nullable = false, length = 20)
    private EstadoPlanoPosicaoOperacional estadoNovo;

    @Column(name = "motivo", nullable = false, length = 1000)
    private String motivo;

    @Column(name = "operador", nullable = false, length = 120)
    private String operador;

    @Column(name = "versao_plano", nullable = false)
    private Long versaoPlano;

    @Column(name = "ocorrido_em", nullable = false)
    private LocalDateTime ocorridoEm;

    @PrePersist
    public void prepararInclusao() {
        ocorridoEm = ocorridoEm == null ? LocalDateTime.now() : ocorridoEm;
    }

    public Long getId() { return id; }
    public Long getPlanoId() { return planoId; }
    public void setPlanoId(Long planoId) { this.planoId = planoId; }
    public EstadoPlanoPosicaoOperacional getEstadoAnterior() { return estadoAnterior; }
    public void setEstadoAnterior(EstadoPlanoPosicaoOperacional estadoAnterior) { this.estadoAnterior = estadoAnterior; }
    public EstadoPlanoPosicaoOperacional getEstadoNovo() { return estadoNovo; }
    public void setEstadoNovo(EstadoPlanoPosicaoOperacional estadoNovo) { this.estadoNovo = estadoNovo; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public String getOperador() { return operador; }
    public void setOperador(String operador) { this.operador = operador; }
    public Long getVersaoPlano() { return versaoPlano; }
    public void setVersaoPlano(Long versaoPlano) { this.versaoPlano = versaoPlano; }
    public LocalDateTime getOcorridoEm() { return ocorridoEm; }
    public void setOcorridoEm(LocalDateTime ocorridoEm) { this.ocorridoEm = ocorridoEm; }
}
