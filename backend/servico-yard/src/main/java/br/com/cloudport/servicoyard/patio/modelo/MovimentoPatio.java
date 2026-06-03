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

/**
 * Registro unificado de operações sobre contêineres no pátio.
 * Substitui HistoricoOperacaoConteiner — todos os eventos de ciclo de vida
 * (alocação, inspeção, liberação, transferência) são gravados aqui.
 */
@Entity
@Table(name = "movimento_patio")
public class MovimentoPatio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conteiner_id", nullable = false)
    private ConteinerPatio conteiner;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_movimento", nullable = false, length = 30)
    private TipoMovimentoPatio tipoMovimento;

    @Column(name = "descricao", nullable = false, length = 160)
    private String descricao;

    @Column(name = "posicao_anterior", length = 120)
    private String posicaoAnterior;

    @Column(name = "posicao_atual", length = 120)
    private String posicaoAtual;

    @Column(name = "responsavel", length = 120)
    private String responsavel;

    @Column(name = "registrado_em", nullable = false)
    private LocalDateTime registradoEm;

    public MovimentoPatio() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ConteinerPatio getConteiner() { return conteiner; }
    public void setConteiner(ConteinerPatio conteiner) { this.conteiner = conteiner; }

    public TipoMovimentoPatio getTipoMovimento() { return tipoMovimento; }
    public void setTipoMovimento(TipoMovimentoPatio tipoMovimento) { this.tipoMovimento = tipoMovimento; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public String getPosicaoAnterior() { return posicaoAnterior; }
    public void setPosicaoAnterior(String posicaoAnterior) { this.posicaoAnterior = posicaoAnterior; }

    public String getPosicaoAtual() { return posicaoAtual; }
    public void setPosicaoAtual(String posicaoAtual) { this.posicaoAtual = posicaoAtual; }

    public String getResponsavel() { return responsavel; }
    public void setResponsavel(String responsavel) { this.responsavel = responsavel; }

    public LocalDateTime getRegistradoEm() { return registradoEm; }
    public void setRegistradoEm(LocalDateTime registradoEm) { this.registradoEm = registradoEm; }
}
