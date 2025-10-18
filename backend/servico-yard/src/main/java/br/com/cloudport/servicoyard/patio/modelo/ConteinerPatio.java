package br.com.cloudport.servicoyard.patio.modelo;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "conteiner_patio")
public class ConteinerPatio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo", nullable = false, length = 30, unique = true)
    private String codigo;

    @Column(name = "linha", nullable = false)
    private Integer linha;

    @Column(name = "coluna", nullable = false)
    private Integer coluna;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_conteiner", nullable = false, length = 30)
    private StatusConteiner status;

    @Column(name = "tipo_carga", nullable = false, length = 40)
    private String tipoCarga;

    @Column(name = "destino", nullable = false, length = 60)
    private String destino;

    @Column(name = "camada_operacional", nullable = false, length = 40)
    private String camadaOperacional;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    public ConteinerPatio() {
    }

    public ConteinerPatio(Long id, String codigo, Integer linha, Integer coluna, StatusConteiner status,
                          String tipoCarga, String destino, String camadaOperacional, LocalDateTime atualizadoEm) {
        this.id = id;
        this.codigo = codigo;
        this.linha = linha;
        this.coluna = coluna;
        this.status = status;
        this.tipoCarga = tipoCarga;
        this.destino = destino;
        this.camadaOperacional = camadaOperacional;
        this.atualizadoEm = atualizadoEm;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public Integer getLinha() {
        return linha;
    }

    public void setLinha(Integer linha) {
        this.linha = linha;
    }

    public Integer getColuna() {
        return coluna;
    }

    public void setColuna(Integer coluna) {
        this.coluna = coluna;
    }

    public StatusConteiner getStatus() {
        return status;
    }

    public void setStatus(StatusConteiner status) {
        this.status = status;
    }

    public String getTipoCarga() {
        return tipoCarga;
    }

    public void setTipoCarga(String tipoCarga) {
        this.tipoCarga = tipoCarga;
    }

    public String getDestino() {
        return destino;
    }

    public void setDestino(String destino) {
        this.destino = destino;
    }

    public String getCamadaOperacional() {
        return camadaOperacional;
    }

    public void setCamadaOperacional(String camadaOperacional) {
        this.camadaOperacional = camadaOperacional;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(LocalDateTime atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }
}
