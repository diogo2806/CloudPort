package br.com.cloudport.servicoyard.patio.modelo;

import br.com.cloudport.servicoyard.inventario.modelo.UnidadeInventario;
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
import javax.persistence.OneToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;

@Entity
@Table(name = "yard_position_divergence")
public class DivergenciaPosicaoPatio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "unidade_id", nullable = false)
    private UnidadeInventario unidade;

    @Column(name = "identificacao_unidade", nullable = false, length = 40)
    private String identificacaoUnidade;

    @Column(name = "posicao_esperada", nullable = false, length = 120)
    private String posicaoEsperada;

    @Column(name = "posicao_encontrada", nullable = false, length = 120)
    private String posicaoEncontrada;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private StatusDivergenciaPosicao status;

    @Column(name = "bloqueada", nullable = false)
    private boolean bloqueada = true;

    @Column(name = "responsavel", length = 120)
    private String responsavel;

    @Column(name = "evidencia", length = 1000)
    private String evidencia;

    @Column(name = "decisao", length = 1000)
    private String decisao;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "instrucao_corretiva_id")
    private InstrucaoTrabalho instrucaoCorretiva;

    @Column(name = "aberta_por", nullable = false, length = 120)
    private String abertaPor;

    @Column(name = "aberta_em", nullable = false)
    private LocalDateTime abertaEm;

    @Column(name = "investigacao_iniciada_em")
    private LocalDateTime investigacaoIniciadaEm;

    @Column(name = "resolvida_em")
    private LocalDateTime resolvidaEm;

    @Column(name = "cancelada_em")
    private LocalDateTime canceladaEm;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void criarAuditoria() {
        LocalDateTime agora = LocalDateTime.now();
        if (abertaEm == null) {
            abertaEm = agora;
        }
        updatedAt = agora;
    }

    @PreUpdate
    public void atualizarAuditoria() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public UnidadeInventario getUnidade() { return unidade; }
    public void setUnidade(UnidadeInventario unidade) { this.unidade = unidade; }
    public String getIdentificacaoUnidade() { return identificacaoUnidade; }
    public void setIdentificacaoUnidade(String identificacaoUnidade) { this.identificacaoUnidade = identificacaoUnidade; }
    public String getPosicaoEsperada() { return posicaoEsperada; }
    public void setPosicaoEsperada(String posicaoEsperada) { this.posicaoEsperada = posicaoEsperada; }
    public String getPosicaoEncontrada() { return posicaoEncontrada; }
    public void setPosicaoEncontrada(String posicaoEncontrada) { this.posicaoEncontrada = posicaoEncontrada; }
    public StatusDivergenciaPosicao getStatus() { return status; }
    public void setStatus(StatusDivergenciaPosicao status) { this.status = status; }
    public boolean isBloqueada() { return bloqueada; }
    public void setBloqueada(boolean bloqueada) { this.bloqueada = bloqueada; }
    public String getResponsavel() { return responsavel; }
    public void setResponsavel(String responsavel) { this.responsavel = responsavel; }
    public String getEvidencia() { return evidencia; }
    public void setEvidencia(String evidencia) { this.evidencia = evidencia; }
    public String getDecisao() { return decisao; }
    public void setDecisao(String decisao) { this.decisao = decisao; }
    public InstrucaoTrabalho getInstrucaoCorretiva() { return instrucaoCorretiva; }
    public void setInstrucaoCorretiva(InstrucaoTrabalho instrucaoCorretiva) { this.instrucaoCorretiva = instrucaoCorretiva; }
    public String getAbertaPor() { return abertaPor; }
    public void setAbertaPor(String abertaPor) { this.abertaPor = abertaPor; }
    public LocalDateTime getAbertaEm() { return abertaEm; }
    public LocalDateTime getInvestigacaoIniciadaEm() { return investigacaoIniciadaEm; }
    public void setInvestigacaoIniciadaEm(LocalDateTime investigacaoIniciadaEm) { this.investigacaoIniciadaEm = investigacaoIniciadaEm; }
    public LocalDateTime getResolvidaEm() { return resolvidaEm; }
    public void setResolvidaEm(LocalDateTime resolvidaEm) { this.resolvidaEm = resolvidaEm; }
    public LocalDateTime getCanceladaEm() { return canceladaEm; }
    public void setCanceladaEm(LocalDateTime canceladaEm) { this.canceladaEm = canceladaEm; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
