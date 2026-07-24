package br.com.cloudport.servicoyard.patio.avisoestivagem.modelo;

import br.com.cloudport.servicoyard.patio.avisoestivagem.modelo.AvisoEstivagemPatioEnums.SeveridadeAvisoEstivagemPatio;
import br.com.cloudport.servicoyard.patio.avisoestivagem.modelo.AvisoEstivagemPatioEnums.StatusAvisoEstivagemPatio;
import br.com.cloudport.servicoyard.patio.avisoestivagem.modelo.AvisoEstivagemPatioEnums.TipoRegraEstivagemPatio;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;

@Entity
@Table(name = "aviso_estivagem_patio", uniqueConstraints = {
        @UniqueConstraint(name = "uk_aviso_estivagem_chave_estavel", columnNames = "chave_estavel")
})
public class AvisoEstivagemPatio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chave_estavel", nullable = false, length = 220)
    private String chaveEstavel;

    @Column(name = "codigo_unidade", nullable = false, length = 40)
    private String codigoUnidade;

    @Column(name = "posicao_id", nullable = false)
    private Long posicaoId;

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
    @Column(name = "status", nullable = false, length = 40)
    private StatusAvisoEstivagemPatio status;

    @Column(name = "descricao", nullable = false, length = 1000)
    private String descricao;

    @Column(name = "valor_observado", length = 1000)
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

    @Column(name = "resultado", length = 2000)
    private String resultado;

    @Column(name = "aberto_em", nullable = false)
    private LocalDateTime abertoEm;

    @Column(name = "ultima_revalidacao_em", nullable = false)
    private LocalDateTime ultimaRevalidacaoEm;

    @Column(name = "resolvido_em")
    private LocalDateTime resolvidoEm;

    @Column(name = "recorrencias", nullable = false)
    private Integer recorrencias = 0;

    @Version
    @Column(name = "versao", nullable = false)
    private Long versao;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @PrePersist
    public void prepararInclusao() {
        LocalDateTime agora = LocalDateTime.now();
        criadoEm = criadoEm == null ? agora : criadoEm;
        atualizadoEm = agora;
        abertoEm = abertoEm == null ? agora : abertoEm;
        ultimaRevalidacaoEm = ultimaRevalidacaoEm == null ? agora : ultimaRevalidacaoEm;
        status = status == null ? StatusAvisoEstivagemPatio.ABERTO : status;
        recorrencias = recorrencias == null ? 0 : recorrencias;
    }

    @PreUpdate
    public void prepararAtualizacao() {
        atualizadoEm = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getChaveEstavel() { return chaveEstavel; }
    public void setChaveEstavel(String chaveEstavel) { this.chaveEstavel = chaveEstavel; }
    public String getCodigoUnidade() { return codigoUnidade; }
    public void setCodigoUnidade(String codigoUnidade) { this.codigoUnidade = codigoUnidade; }
    public Long getPosicaoId() { return posicaoId; }
    public void setPosicaoId(Long posicaoId) { this.posicaoId = posicaoId; }
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
    public StatusAvisoEstivagemPatio getStatus() { return status; }
    public void setStatus(StatusAvisoEstivagemPatio status) { this.status = status; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
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
    public String getResultado() { return resultado; }
    public void setResultado(String resultado) { this.resultado = resultado; }
    public LocalDateTime getAbertoEm() { return abertoEm; }
    public void setAbertoEm(LocalDateTime abertoEm) { this.abertoEm = abertoEm; }
    public LocalDateTime getUltimaRevalidacaoEm() { return ultimaRevalidacaoEm; }
    public void setUltimaRevalidacaoEm(LocalDateTime ultimaRevalidacaoEm) { this.ultimaRevalidacaoEm = ultimaRevalidacaoEm; }
    public LocalDateTime getResolvidoEm() { return resolvidoEm; }
    public void setResolvidoEm(LocalDateTime resolvidoEm) { this.resolvidoEm = resolvidoEm; }
    public Integer getRecorrencias() { return recorrencias; }
    public void setRecorrencias(Integer recorrencias) { this.recorrencias = recorrencias; }
    public Long getVersao() { return versao; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(LocalDateTime atualizadoEm) { this.atualizadoEm = atualizadoEm; }
}
