package br.com.cloudport.visibilidade.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "status_navio")
public class StatusNavio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "navio_id", unique = true, nullable = false)
    private String navioId;

    @Column(name = "nome_navio")
    private String nomeNavio;

    @Column(name = "status_operacional")
    private String statusOperacional; // ancorando, operando, pronto_para_partir, etc.

    @Column(name = "berco_alocado")
    private String bercoAlocado;

    @Column(name = "eta_estimado")
    private LocalDateTime etaEstimado;

    @Column(name = "chegada_real")
    private LocalDateTime chegadaReal;

    @Column(name = "atraso_minutos")
    private Integer atrasoMinutos;

    @Column(name = "porcentagem_completa")
    private Double porcentagemCompleta;

    @Column(name = "data_atualizacao")
    private LocalDateTime dataAtualizacao;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNavioId() { return navioId; }
    public void setNavioId(String navioId) { this.navioId = navioId; }

    public String getNomeNavio() { return nomeNavio; }
    public void setNomeNavio(String nomeNavio) { this.nomeNavio = nomeNavio; }

    public String getStatusOperacional() { return statusOperacional; }
    public void setStatusOperacional(String statusOperacional) { this.statusOperacional = statusOperacional; }

    public String getBercoAlocado() { return bercoAlocado; }
    public void setBercoAlocado(String bercoAlocado) { this.bercoAlocado = bercoAlocado; }

    public LocalDateTime getEtaEstimado() { return etaEstimado; }
    public void setEtaEstimado(LocalDateTime etaEstimado) { this.etaEstimado = etaEstimado; }

    public LocalDateTime getChegadaReal() { return chegadaReal; }
    public void setChegadaReal(LocalDateTime chegadaReal) { this.chegadaReal = chegadaReal; }

    public Integer getAtrasoMinutos() { return atrasoMinutos; }
    public void setAtrasoMinutos(Integer atrasoMinutos) { this.atrasoMinutos = atrasoMinutos; }

    public Double getPorcentagemCompleta() { return porcentagemCompleta; }
    public void setPorcentagemCompleta(Double porcentagemCompleta) { this.porcentagemCompleta = porcentagemCompleta; }

    public LocalDateTime getDataAtualizacao() { return dataAtualizacao; }
    public void setDataAtualizacao(LocalDateTime dataAtualizacao) { this.dataAtualizacao = dataAtualizacao; }
}