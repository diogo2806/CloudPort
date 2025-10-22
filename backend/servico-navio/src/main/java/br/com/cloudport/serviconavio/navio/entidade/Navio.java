package br.com.cloudport.serviconavio.navio.entidade;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "navio")
public class Navio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "identificador")
    private Long identificador;

    @Column(name = "nome", nullable = false, length = 120)
    private String nome;

    @Column(name = "codigo_imo", nullable = false, unique = true, length = 10)
    private String codigoImo;

    @Column(name = "pais_bandeira", nullable = false, length = 60)
    private String paisBandeira;

    @Column(name = "empresa_armadora", nullable = false, length = 80)
    private String empresaArmadora;

    @Column(name = "capacidade_teu", nullable = false)
    private Integer capacidadeTeu;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_operacao", nullable = false, length = 30)
    private StatusOperacaoNavio statusOperacao;

    @Column(name = "data_prevista_atracacao", nullable = false)
    private LocalDateTime dataPrevistaAtracacao;

    @Column(name = "data_efetiva_atracacao")
    private LocalDateTime dataEfetivaAtracacao;

    @Column(name = "data_efetiva_desatracacao")
    private LocalDateTime dataEfetivaDesatracacao;

    @Column(name = "berco_previsto", length = 20)
    private String bercoPrevisto;

    @Column(name = "berco_atual", length = 20)
    private String bercoAtual;

    @Column(name = "observacoes", length = 500)
    private String observacoes;

    public Long getIdentificador() {
        return identificador;
    }

    public void setIdentificador(Long identificador) {
        this.identificador = identificador;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCodigoImo() {
        return codigoImo;
    }

    public void setCodigoImo(String codigoImo) {
        this.codigoImo = codigoImo;
    }

    public String getPaisBandeira() {
        return paisBandeira;
    }

    public void setPaisBandeira(String paisBandeira) {
        this.paisBandeira = paisBandeira;
    }

    public String getEmpresaArmadora() {
        return empresaArmadora;
    }

    public void setEmpresaArmadora(String empresaArmadora) {
        this.empresaArmadora = empresaArmadora;
    }

    public Integer getCapacidadeTeu() {
        return capacidadeTeu;
    }

    public void setCapacidadeTeu(Integer capacidadeTeu) {
        this.capacidadeTeu = capacidadeTeu;
    }

    public StatusOperacaoNavio getStatusOperacao() {
        return statusOperacao;
    }

    public void setStatusOperacao(StatusOperacaoNavio statusOperacao) {
        this.statusOperacao = statusOperacao;
    }

    public LocalDateTime getDataPrevistaAtracacao() {
        return dataPrevistaAtracacao;
    }

    public void setDataPrevistaAtracacao(LocalDateTime dataPrevistaAtracacao) {
        this.dataPrevistaAtracacao = dataPrevistaAtracacao;
    }

    public LocalDateTime getDataEfetivaAtracacao() {
        return dataEfetivaAtracacao;
    }

    public void setDataEfetivaAtracacao(LocalDateTime dataEfetivaAtracacao) {
        this.dataEfetivaAtracacao = dataEfetivaAtracacao;
    }

    public LocalDateTime getDataEfetivaDesatracacao() {
        return dataEfetivaDesatracacao;
    }

    public void setDataEfetivaDesatracacao(LocalDateTime dataEfetivaDesatracacao) {
        this.dataEfetivaDesatracacao = dataEfetivaDesatracacao;
    }

    public String getBercoPrevisto() {
        return bercoPrevisto;
    }

    public void setBercoPrevisto(String bercoPrevisto) {
        this.bercoPrevisto = bercoPrevisto;
    }

    public String getBercoAtual() {
        return bercoAtual;
    }

    public void setBercoAtual(String bercoAtual) {
        this.bercoAtual = bercoAtual;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }
}
