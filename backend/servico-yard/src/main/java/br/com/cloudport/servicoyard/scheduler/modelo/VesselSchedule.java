package br.com.cloudport.servicoyard.scheduler.modelo;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "vessel_schedule")
public class VesselSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_navio", nullable = false, length = 80)
    private String codigoNavio;

    @Column(name = "nome_berco", nullable = false, length = 80)
    private String nomeBerco;

    @Column(name = "tempo_previsto", nullable = false)
    private LocalDateTime tempoPrevisto;

    @Column(name = "tempo_termino", nullable = false)
    private LocalDateTime tempoTermino;

    @Column(name = "prioridade", nullable = false, length = 30)
    private String prioridade;

    @Column(name = "capacidade_requerida", nullable = false)
    private Integer capacidadeRequerida;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    public Long getId() { return id; }
    public String getCodigoNavio() { return codigoNavio; }
    public void setCodigoNavio(String codigoNavio) { this.codigoNavio = codigoNavio; }
    public String getNomeBerco() { return nomeBerco; }
    public void setNomeBerco(String nomeBerco) { this.nomeBerco = nomeBerco; }
    public LocalDateTime getTempoPrevisto() { return tempoPrevisto; }
    public void setTempoPrevisto(LocalDateTime tempoPrevisto) { this.tempoPrevisto = tempoPrevisto; }
    public LocalDateTime getTempoTermino() { return tempoTermino; }
    public void setTempoTermino(LocalDateTime tempoTermino) { this.tempoTermino = tempoTermino; }
    public String getPrioridade() { return prioridade; }
    public void setPrioridade(String prioridade) { this.prioridade = prioridade; }
    public Integer getCapacidadeRequerida() { return capacidadeRequerida; }
    public void setCapacidadeRequerida(Integer capacidadeRequerida) { this.capacidadeRequerida = capacidadeRequerida; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
}
