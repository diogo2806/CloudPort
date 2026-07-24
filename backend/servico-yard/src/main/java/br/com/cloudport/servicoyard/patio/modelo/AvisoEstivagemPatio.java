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
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;

@Entity
@Table(name = "aviso_estivagem_patio", uniqueConstraints = {
        @UniqueConstraint(name = "uk_aviso_estivagem_chave", columnNames = "chave_estavel"),
        @UniqueConstraint(name = "uk_aviso_estivagem_unidade_posicao_regra",
                columnNames = {"codigo_unidade", "codigo_posicao", "regra"})
})
public class AvisoEstivagemPatio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chave_estavel", nullable = false, length = 255)
    private String chaveEstavel;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unidade_id", nullable = false)
    private ConteinerPatio unidade;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "posicao_id", nullable = false)
    private PosicaoPatio posicao;

    @Column(name = "codigo_unidade", nullable = false, length = 40)
    private String codigoUnidade;

    @Column(name = "codigo_posicao", nullable = false, length = 120)
    private String codigoPosicao;

    @Column(name = "bloco", length = 40)
    private String bloco;

    @Column(name = "linha", nullable = false)
    private Integer linha;

    @Column(name = "coluna", nullable = false)
    private Integer coluna;

    @Column(name = "camada", nullable = false, length = 40)
    private String camada;

    @Enumerated(EnumType.STRING)
    @Column(name = "regra", nullable = false, length = 40)
    private TipoRegraEstivagemPatio regra;

    @Enumerated(EnumType.STRING)
    @Column(name = "severidade", nullable = false, length = 20)
    private SeveridadeAvisoEstivagemPatio severidade;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 40)
    private EstadoAvisoEstivagemPatio estado;

    @Column(name = "valor_observado", nullable = false, length = 1000)
    private String valorObservado;

    @Column(name = "valor_esperado", length = 1000)
    private String valorEsperado;

    @Column(name = "acao_sugerida", length = 1000)
    private String acaoSugerida;

    @Column(name = "responsavel", length = 120)
    private String responsavel;

    @Column(name = "prazo")
    private LocalDateTime prazo;

    @Column(name = "acao_corretiva", length = 2000)
    private String acaoCorretiva;

    @Column(name = "evidencia", length = 2000)
    private String evidencia;

    @Column(name = "resultado_revalidacao", length = 2000)
    private String resultadoRevalidacao;

    @Column(name = "ocorrencias", nullable = false)
    private int ocorrencias = 1;

    @Column(name = "bloqueia_operacao", nullable = false)
    private boolean bloqueiaOperacao;

    @Column(name = "aberto_em", nullable = false)
    private LocalDateTime abertoEm;

    @Column(name = "ultima_revalidacao_em")
    private LocalDateTime ultimaRevalidacaoEm;

    @Column(name = "resolvido_em")
    private LocalDateTime resolvidoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @Version
    @Column(name = "versao", nullable = false)
    private Long versao;

    @PrePersist
    public void criarAuditoria() {
        LocalDateTime agora = LocalDateTime.now();
        if (abertoEm == null) {
            abertoEm = agora;
        }
        atualizadoEm = agora;
    }

    @PreUpdate
    public void atualizarAuditoria() {
        atualizadoEm = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getChaveEstavel() { return chaveEstavel; }
    public void setChaveEstavel(String chaveEstavel) { this.chaveEstavel = chaveEstavel; }
    public ConteinerPatio getUnidade() { return unidade; }
    public void setUnidade(ConteinerPatio unidade) { this.unidade = unidade; }
    public PosicaoPatio getPosicao() { return posicao; }
    public void setPosicao(PosicaoPatio posicao) { this.posicao = posicao; }
    public String getCodigoUnidade() { return codigoUnidade; }
    public void setCodigoUnidade(String codigoUnidade) { this.codigoUnidade = codigoUnidade; }
    public String getCodigoPosicao() { return codigoPosicao; }
    public void setCodigoPosicao(String codigoPosicao) { this.codigoPosicao = codigoPosicao; }
    public String getBloco() { return bloco; }
    public void setBloco(String bloco) { this.bloco = bloco; }
    public Integer getLinha() { return linha; }
    public void setLinha(Integer linha) { this.linha = linha; }
    public Integer getColuna() { return coluna; }
    public void setColuna(Integer coluna) { this.coluna = coluna; }
    public String getCamada() { return camada; }
    public void setCamada(String camada) { this.camada = camada; }
    public TipoRegraEstivagemPatio getRegra() { return regra; }
    public void setRegra(TipoRegraEstivagemPatio regra) { this.regra = regra; }
    public SeveridadeAvisoEstivagemPatio getSeveridade() { return severidade; }
    public void setSeveridade(SeveridadeAvisoEstivagemPatio severidade) { this.severidade = severidade; }
    public EstadoAvisoEstivagemPatio getEstado() { return estado; }
    public void setEstado(EstadoAvisoEstivagemPatio estado) { this.estado = estado; }
    public String getValorObservado() { return valorObservado; }
    public void setValorObservado(String valorObservado) { this.valorObservado = valorObservado; }
    public String getValorEsperado() { return valorEsperado; }
    public void setValorEsperado(String valorEsperado) { this.valorEsperado = valorEsperado; }
    public String getAcaoSugerida() { return acaoSugerida; }
    public void setAcaoSugerida(String acaoSugerida) { this.acaoSugerida = acaoSugerida; }
    public String getResponsavel() { return responsavel; }
    public void setResponsavel(String responsavel) { this.responsavel = responsavel; }
    public LocalDateTime getPrazo() { return prazo; }
    public void setPrazo(LocalDateTime prazo) { this.prazo = prazo; }
    public String getAcaoCorretiva() { return acaoCorretiva; }
    public void setAcaoCorretiva(String acaoCorretiva) { this.acaoCorretiva = acaoCorretiva; }
    public String getEvidencia() { return evidencia; }
    public void setEvidencia(String evidencia) { this.evidencia = evidencia; }
    public String getResultadoRevalidacao() { return resultadoRevalidacao; }
    public void setResultadoRevalidacao(String resultadoRevalidacao) { this.resultadoRevalidacao = resultadoRevalidacao; }
    public int getOcorrencias() { return ocorrencias; }
    public void setOcorrencias(int ocorrencias) { this.ocorrencias = ocorrencias; }
    public boolean isBloqueiaOperacao() { return bloqueiaOperacao; }
    public void setBloqueiaOperacao(boolean bloqueiaOperacao) { this.bloqueiaOperacao = bloqueiaOperacao; }
    public LocalDateTime getAbertoEm() { return abertoEm; }
    public LocalDateTime getUltimaRevalidacaoEm() { return ultimaRevalidacaoEm; }
    public void setUltimaRevalidacaoEm(LocalDateTime ultimaRevalidacaoEm) { this.ultimaRevalidacaoEm = ultimaRevalidacaoEm; }
    public LocalDateTime getResolvidoEm() { return resolvidoEm; }
    public void setResolvidoEm(LocalDateTime resolvidoEm) { this.resolvidoEm = resolvidoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public Long getVersao() { return versao; }
}
