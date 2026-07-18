package br.com.cloudport.servicoyard.inventario.modelo;

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
import javax.persistence.PrePersist;
import javax.persistence.Table;

@Entity
@Table(name = "contagem_inventario_fisico")
public class ContagemInventarioFisico {

    public enum StatusContagem {
        CONFERENTE,
        DIVERGENTE,
        NAO_LOCALIZADA,
        NAO_PREVISTA,
        RESOLVIDA
    }

    public enum TipoDivergencia {
        POSICAO,
        UNIDADE_NAO_LOCALIZADA,
        UNIDADE_NAO_PREVISTA,
        ATRIBUTO,
        LACRE,
        CONDICAO
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lote", nullable = false, length = 80)
    private String lote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unidade_id")
    private UnidadeInventario unidade;

    @Column(name = "identificacao_lida", nullable = false, length = 40)
    private String identificacaoLida;

    @Column(name = "posicao_esperada", length = 120)
    private String posicaoEsperada;

    @Column(name = "posicao_lida", length = 120)
    private String posicaoLida;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_contagem", nullable = false, length = 30)
    private StatusContagem status;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_divergencia", length = 40)
    private TipoDivergencia tipoDivergencia;

    @Column(name = "observacoes", length = 500)
    private String observacoes;

    @Column(name = "responsavel", length = 120)
    private String responsavel;

    @Column(name = "registrado_em", nullable = false)
    private LocalDateTime registradoEm;

    @Column(name = "resolvido_em")
    private LocalDateTime resolvidoEm;

    @Column(name = "resolvido_por", length = 120)
    private String resolvidoPor;

    @PrePersist
    public void prePersist() {
        if (registradoEm == null) {
            registradoEm = LocalDateTime.now();
        }
    }

    public void resolver(String responsavelResolucao) {
        status = StatusContagem.RESOLVIDA;
        resolvidoEm = LocalDateTime.now();
        resolvidoPor = responsavelResolucao;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLote() {
        return lote;
    }

    public void setLote(String lote) {
        this.lote = lote;
    }

    public UnidadeInventario getUnidade() {
        return unidade;
    }

    public void setUnidade(UnidadeInventario unidade) {
        this.unidade = unidade;
    }

    public String getIdentificacaoLida() {
        return identificacaoLida;
    }

    public void setIdentificacaoLida(String identificacaoLida) {
        this.identificacaoLida = identificacaoLida;
    }

    public String getPosicaoEsperada() {
        return posicaoEsperada;
    }

    public void setPosicaoEsperada(String posicaoEsperada) {
        this.posicaoEsperada = posicaoEsperada;
    }

    public String getPosicaoLida() {
        return posicaoLida;
    }

    public void setPosicaoLida(String posicaoLida) {
        this.posicaoLida = posicaoLida;
    }

    public StatusContagem getStatus() {
        return status;
    }

    public void setStatus(StatusContagem status) {
        this.status = status;
    }

    public TipoDivergencia getTipoDivergencia() {
        return tipoDivergencia;
    }

    public void setTipoDivergencia(TipoDivergencia tipoDivergencia) {
        this.tipoDivergencia = tipoDivergencia;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public String getResponsavel() {
        return responsavel;
    }

    public void setResponsavel(String responsavel) {
        this.responsavel = responsavel;
    }

    public LocalDateTime getRegistradoEm() {
        return registradoEm;
    }

    public void setRegistradoEm(LocalDateTime registradoEm) {
        this.registradoEm = registradoEm;
    }

    public LocalDateTime getResolvidoEm() {
        return resolvidoEm;
    }

    public String getResolvidoPor() {
        return resolvidoPor;
    }
}
