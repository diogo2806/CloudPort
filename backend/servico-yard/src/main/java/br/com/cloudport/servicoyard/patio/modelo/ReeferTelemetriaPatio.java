package br.com.cloudport.servicoyard.patio.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name = "reefer_telemetria_patio")
public class ReeferTelemetriaPatio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conteiner_id", nullable = false, unique = true)
    private ConteinerPatio conteiner;

    @Column(name = "temperatura_atual_celsius", nullable = false, precision = 7, scale = 2)
    private BigDecimal temperaturaAtualCelsius;

    @Column(name = "temperatura_minima_celsius", nullable = false, precision = 7, scale = 2)
    private BigDecimal temperaturaMinimaCelsius;

    @Column(name = "temperatura_maxima_celsius", nullable = false, precision = 7, scale = 2)
    private BigDecimal temperaturaMaximaCelsius;

    @Column(name = "ligado", nullable = false)
    private boolean ligado;

    @Column(name = "registrado_em", nullable = false)
    private LocalDateTime registradoEm;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ConteinerPatio getConteiner() { return conteiner; }
    public void setConteiner(ConteinerPatio conteiner) { this.conteiner = conteiner; }
    public BigDecimal getTemperaturaAtualCelsius() { return temperaturaAtualCelsius; }
    public void setTemperaturaAtualCelsius(BigDecimal temperaturaAtualCelsius) { this.temperaturaAtualCelsius = temperaturaAtualCelsius; }
    public BigDecimal getTemperaturaMinimaCelsius() { return temperaturaMinimaCelsius; }
    public void setTemperaturaMinimaCelsius(BigDecimal temperaturaMinimaCelsius) { this.temperaturaMinimaCelsius = temperaturaMinimaCelsius; }
    public BigDecimal getTemperaturaMaximaCelsius() { return temperaturaMaximaCelsius; }
    public void setTemperaturaMaximaCelsius(BigDecimal temperaturaMaximaCelsius) { this.temperaturaMaximaCelsius = temperaturaMaximaCelsius; }
    public boolean isLigado() { return ligado; }
    public void setLigado(boolean ligado) { this.ligado = ligado; }
    public LocalDateTime getRegistradoEm() { return registradoEm; }
    public void setRegistradoEm(LocalDateTime registradoEm) { this.registradoEm = registradoEm; }
}