package br.com.cloudport.servicoyard.patio.listatrabalho.modelo;

import br.com.cloudport.servicoyard.patio.modelo.StatusConteiner;
import br.com.cloudport.servicoyard.patio.modelo.TipoMovimentoPatio;
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
import br.com.cloudport.servicoyard.patio.modelo.ConteinerPatio;

@Entity
@Table(name = "ordem_trabalho_patio")
public class OrdemTrabalhoPatio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conteiner_id")
    private ConteinerPatio conteiner;

    @Column(name = "codigo_conteiner", nullable = false, length = 30)
    private String codigoConteiner;

    @Column(name = "tipo_carga", nullable = false, length = 40)
    private String tipoCarga;

    @Column(name = "destino", nullable = false, length = 60)
    private String destino;

    @Column(name = "linha_destino", nullable = false)
    private Integer linhaDestino;

    @Column(name = "coluna_destino", nullable = false)
    private Integer colunaDestino;

    @Column(name = "camada_destino", nullable = false, length = 40)
    private String camadaDestino;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_movimento", nullable = false, length = 20)
    private TipoMovimentoPatio tipoMovimento;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_ordem", nullable = false, length = 20)
    private StatusOrdemTrabalhoPatio statusOrdem;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_conteiner_destino", nullable = false, length = 30)
    private StatusConteiner statusConteinerDestino;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @Column(name = "concluido_em")
    private LocalDateTime concluidoEm;

    public OrdemTrabalhoPatio() {
    }

    public OrdemTrabalhoPatio(ConteinerPatio conteiner,
                              String codigoConteiner,
                              String tipoCarga,
                              String destino,
                              Integer linhaDestino,
                              Integer colunaDestino,
                              String camadaDestino,
                              TipoMovimentoPatio tipoMovimento,
                              StatusOrdemTrabalhoPatio statusOrdem,
                              StatusConteiner statusConteinerDestino,
                              LocalDateTime criadoEm,
                              LocalDateTime atualizadoEm) {
        this.conteiner = conteiner;
        this.codigoConteiner = codigoConteiner;
        this.tipoCarga = tipoCarga;
        this.destino = destino;
        this.linhaDestino = linhaDestino;
        this.colunaDestino = colunaDestino;
        this.camadaDestino = camadaDestino;
        this.tipoMovimento = tipoMovimento;
        this.statusOrdem = statusOrdem;
        this.statusConteinerDestino = statusConteinerDestino;
        this.criadoEm = criadoEm;
        this.atualizadoEm = atualizadoEm;
    }

    public Long getId() {
        return id;
    }

    public ConteinerPatio getConteiner() {
        return conteiner;
    }

    public void setConteiner(ConteinerPatio conteiner) {
        this.conteiner = conteiner;
    }

    public String getCodigoConteiner() {
        return codigoConteiner;
    }

    public void setCodigoConteiner(String codigoConteiner) {
        this.codigoConteiner = codigoConteiner;
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

    public Integer getLinhaDestino() {
        return linhaDestino;
    }

    public void setLinhaDestino(Integer linhaDestino) {
        this.linhaDestino = linhaDestino;
    }

    public Integer getColunaDestino() {
        return colunaDestino;
    }

    public void setColunaDestino(Integer colunaDestino) {
        this.colunaDestino = colunaDestino;
    }

    public String getCamadaDestino() {
        return camadaDestino;
    }

    public void setCamadaDestino(String camadaDestino) {
        this.camadaDestino = camadaDestino;
    }

    public TipoMovimentoPatio getTipoMovimento() {
        return tipoMovimento;
    }

    public void setTipoMovimento(TipoMovimentoPatio tipoMovimento) {
        this.tipoMovimento = tipoMovimento;
    }

    public StatusOrdemTrabalhoPatio getStatusOrdem() {
        return statusOrdem;
    }

    public void setStatusOrdem(StatusOrdemTrabalhoPatio statusOrdem) {
        this.statusOrdem = statusOrdem;
    }

    public StatusConteiner getStatusConteinerDestino() {
        return statusConteinerDestino;
    }

    public void setStatusConteinerDestino(StatusConteiner statusConteinerDestino) {
        this.statusConteinerDestino = statusConteinerDestino;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(LocalDateTime atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }

    public LocalDateTime getConcluidoEm() {
        return concluidoEm;
    }

    public void setConcluidoEm(LocalDateTime concluidoEm) {
        this.concluidoEm = concluidoEm;
    }
}
