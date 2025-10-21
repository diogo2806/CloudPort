package br.com.cloudport.servicoyard.patio.modelo;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "conteiner_patio")
public class ConteinerPatio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo", nullable = false, length = 30, unique = true)
    private String codigo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_conteiner", nullable = false, length = 30)
    private StatusConteiner status;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "carga_id")
    private CargaPatio carga;

    @Column(name = "destino", nullable = false, length = 60)
    private String destino;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "posicao_id", nullable = false)
    private PosicaoPatio posicao;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    public ConteinerPatio() {
    }

    public ConteinerPatio(Long id, String codigo, StatusConteiner status,
                          CargaPatio carga, String destino, PosicaoPatio posicao,
                          LocalDateTime atualizadoEm) {
        this.id = id;
        this.codigo = codigo;
        this.status = status;
        this.carga = carga;
        this.destino = destino;
        this.posicao = posicao;
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

    public CargaPatio getCarga() {
        return carga;
    }

    public void setCarga(CargaPatio carga) {
        this.carga = carga;
    }

    public PosicaoPatio getPosicao() {
        return posicao;
    }

    public void setPosicao(PosicaoPatio posicao) {
        this.posicao = posicao;
    }

    public StatusConteiner getStatus() {
        return status;
    }

    public void setStatus(StatusConteiner status) {
        this.status = status;
    }

    public String getDestino() {
        return destino;
    }

    public void setDestino(String destino) {
        this.destino = destino;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(LocalDateTime atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }
}
