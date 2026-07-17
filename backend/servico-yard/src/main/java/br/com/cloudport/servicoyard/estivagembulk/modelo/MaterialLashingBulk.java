package br.com.cloudport.servicoyard.estivagembulk.modelo;

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
@Table(name = "material_lashing_bulk")
public class MaterialLashingBulk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plano_estiva_id")
    private PlanoEstivaBulk plano;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "posicao_bobina_id")
    private PosicaoBobina posicao;

    @Enumerated(EnumType.STRING)
    @Column(length = 25)
    private TipoLashing tipo;

    private int quantidade;

    @Column(name = "comprimento_m")
    private Double comprimentoM;

    @Column(name = "peso_unitario_kg")
    private Double pesoUnitarioKg;

    @Column(name = "ponto_amarracao", length = 80)
    private String pontoAmarracao;

    @Column(name = "capacidade_nominal_kn")
    private Double capacidadeNominalKn;

    @Column(name = "carga_trabalho_segura_kn")
    private Double cargaTrabalhoSeguraKn;

    @Column(length = 100)
    private String certificado;

    @Column(name = "referencia_regra", length = 120)
    private String referenciaRegra;

    @Column(name = "versao_especificacao", length = 60)
    private String versaoEspecificacao;

    @Column(name = "responsavel_validacao", length = 100)
    private String responsavelValidacao;

    @Enumerated(EnumType.STRING)
    @Column(name = "resultado_validacao", length = 20)
    private ResultadoValidacaoSeguranca resultadoValidacao = ResultadoValidacaoSeguranca.PENDENTE;

    @Column(name = "validado_em")
    private LocalDateTime validadoEm;

    @Column(length = 200)
    private String descricao;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PlanoEstivaBulk getPlano() {
        return plano;
    }

    public void setPlano(PlanoEstivaBulk plano) {
        this.plano = plano;
    }

    public PosicaoBobina getPosicao() {
        return posicao;
    }

    public void setPosicao(PosicaoBobina posicao) {
        this.posicao = posicao;
    }

    public TipoLashing getTipo() {
        return tipo;
    }

    public void setTipo(TipoLashing tipo) {
        this.tipo = tipo;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(int quantidade) {
        this.quantidade = quantidade;
    }

    public Double getComprimentoM() {
        return comprimentoM;
    }

    public void setComprimentoM(Double comprimentoM) {
        this.comprimentoM = comprimentoM;
    }

    public Double getPesoUnitarioKg() {
        return pesoUnitarioKg;
    }

    public void setPesoUnitarioKg(Double pesoUnitarioKg) {
        this.pesoUnitarioKg = pesoUnitarioKg;
    }

    public String getPontoAmarracao() {
        return pontoAmarracao;
    }

    public void setPontoAmarracao(String pontoAmarracao) {
        this.pontoAmarracao = pontoAmarracao;
    }

    public Double getCapacidadeNominalKn() {
        return capacidadeNominalKn;
    }

    public void setCapacidadeNominalKn(Double capacidadeNominalKn) {
        this.capacidadeNominalKn = capacidadeNominalKn;
    }

    public Double getCargaTrabalhoSeguraKn() {
        return cargaTrabalhoSeguraKn;
    }

    public void setCargaTrabalhoSeguraKn(Double cargaTrabalhoSeguraKn) {
        this.cargaTrabalhoSeguraKn = cargaTrabalhoSeguraKn;
    }

    public String getCertificado() {
        return certificado;
    }

    public void setCertificado(String certificado) {
        this.certificado = certificado;
    }

    public String getReferenciaRegra() {
        return referenciaRegra;
    }

    public void setReferenciaRegra(String referenciaRegra) {
        this.referenciaRegra = referenciaRegra;
    }

    public String getVersaoEspecificacao() {
        return versaoEspecificacao;
    }

    public void setVersaoEspecificacao(String versaoEspecificacao) {
        this.versaoEspecificacao = versaoEspecificacao;
    }

    public String getResponsavelValidacao() {
        return responsavelValidacao;
    }

    public void setResponsavelValidacao(String responsavelValidacao) {
        this.responsavelValidacao = responsavelValidacao;
    }

    public ResultadoValidacaoSeguranca getResultadoValidacao() {
        return resultadoValidacao;
    }

    public void setResultadoValidacao(ResultadoValidacaoSeguranca resultadoValidacao) {
        this.resultadoValidacao = resultadoValidacao;
    }

    public LocalDateTime getValidadoEm() {
        return validadoEm;
    }

    public void setValidadoEm(LocalDateTime validadoEm) {
        this.validadoEm = validadoEm;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }
}
