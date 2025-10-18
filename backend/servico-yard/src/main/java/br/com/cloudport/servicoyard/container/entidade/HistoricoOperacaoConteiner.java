package br.com.cloudport.servicoyard.container.entidade;

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
import java.time.OffsetDateTime;

@Entity
@Table(name = "historico_operacao_conteiner")
public class HistoricoOperacaoConteiner {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conteiner_id", nullable = false)
    private Conteiner conteiner;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_operacao", nullable = false, length = 40)
    private TipoOperacaoConteiner tipoOperacao;

    @Column(name = "descricao", nullable = false, length = 255)
    private String descricao;

    @Column(name = "posicao_anterior", length = 60)
    private String posicaoAnterior;

    @Column(name = "posicao_atual", length = 60)
    private String posicaoAtual;

    @Column(name = "responsavel", length = 80)
    private String responsavel;

    @Column(name = "data_registro", nullable = false)
    private OffsetDateTime dataRegistro;

    public Long getId() {
        return id;
    }

    public Conteiner getConteiner() {
        return conteiner;
    }

    public void setConteiner(Conteiner conteiner) {
        this.conteiner = conteiner;
    }

    public TipoOperacaoConteiner getTipoOperacao() {
        return tipoOperacao;
    }

    public void setTipoOperacao(TipoOperacaoConteiner tipoOperacao) {
        this.tipoOperacao = tipoOperacao;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getPosicaoAnterior() {
        return posicaoAnterior;
    }

    public void setPosicaoAnterior(String posicaoAnterior) {
        this.posicaoAnterior = posicaoAnterior;
    }

    public String getPosicaoAtual() {
        return posicaoAtual;
    }

    public void setPosicaoAtual(String posicaoAtual) {
        this.posicaoAtual = posicaoAtual;
    }

    public String getResponsavel() {
        return responsavel;
    }

    public void setResponsavel(String responsavel) {
        this.responsavel = responsavel;
    }

    public OffsetDateTime getDataRegistro() {
        return dataRegistro;
    }

    public void setDataRegistro(OffsetDateTime dataRegistro) {
        this.dataRegistro = dataRegistro;
    }
}
