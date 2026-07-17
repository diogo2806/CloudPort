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
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;

@Entity
@Table(name = "posicao_bobina")
public class PosicaoBobina {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plano_estiva_id")
    private PlanoEstivaBulk plano;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bobina_id")
    private BobinaManifesto bobina;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "porao_id")
    private PoraoNavio porao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "setor_id")
    private SetorTanktop setor;

    @Column(nullable = false)
    private int camada;

    @Column(name = "posicao_x")
    private Double posicaoX;

    @Column(name = "posicao_y")
    private Double posicaoY;

    @Column(name = "angulo_inclinacao")
    private Double anguloInclinacao;

    @Column(name = "espessura_dunnage_mm")
    private Double espessuraDunnageMm;

    @Column(name = "quantidade_linhas_dunnage")
    private Integer quantidadeLinhasDunnage;

    @Column(name = "largura_dunnage_mm")
    private Double larguraDunnageMm;

    @Column(name = "comprimento_contato_dunnage_mm")
    private Double comprimentoContatoDunnageMm;

    @Column(name = "quantidade_calcos")
    private Integer quantidadeCalcos;

    @Column(name = "espacamento_fileiras_mm")
    private Double espacamentoFileirasMm;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_lashing", length = 25)
    private TipoLashing tipoLashing;

    @Column(name = "forca_requerida_lashing_kn")
    private Double forcaRequeridaLashingKn;

    @Column(name = "capacidade_lashing_disponivel_kn")
    private Double capacidadeLashingDisponivelKn;

    @Column(name = "sequencia_descarga")
    private Integer sequenciaDescarga;

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

    @Column(name = "alerta_tanktop", length = 200)
    private String alertaTanktop;

    private LocalDateTime criadoEm;

    private LocalDateTime atualizadoEm;

    @PrePersist
    @PreUpdate
    void touch() {
        LocalDateTime now = LocalDateTime.now();
        if (criadoEm == null) {
            criadoEm = now;
        }
        atualizadoEm = now;
    }

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

    public BobinaManifesto getBobina() {
        return bobina;
    }

    public void setBobina(BobinaManifesto bobina) {
        this.bobina = bobina;
    }

    public PoraoNavio getPorao() {
        return porao;
    }

    public void setPorao(PoraoNavio porao) {
        this.porao = porao;
    }

    public SetorTanktop getSetor() {
        return setor;
    }

    public void setSetor(SetorTanktop setor) {
        this.setor = setor;
    }

    public int getCamada() {
        return camada;
    }

    public void setCamada(int camada) {
        this.camada = camada;
    }

    public Double getPosicaoX() {
        return posicaoX;
    }

    public void setPosicaoX(Double posicaoX) {
        this.posicaoX = posicaoX;
    }

    public Double getPosicaoY() {
        return posicaoY;
    }

    public void setPosicaoY(Double posicaoY) {
        this.posicaoY = posicaoY;
    }

    public Double getAnguloInclinacao() {
        return anguloInclinacao;
    }

    public void setAnguloInclinacao(Double anguloInclinacao) {
        this.anguloInclinacao = anguloInclinacao;
    }

    public Double getEspessuraDunnageMm() {
        return espessuraDunnageMm;
    }

    public void setEspessuraDunnageMm(Double espessuraDunnageMm) {
        this.espessuraDunnageMm = espessuraDunnageMm;
    }

    public Integer getQuantidadeLinhasDunnage() {
        return quantidadeLinhasDunnage;
    }

    public void setQuantidadeLinhasDunnage(Integer quantidadeLinhasDunnage) {
        this.quantidadeLinhasDunnage = quantidadeLinhasDunnage;
    }

    public Double getLarguraDunnageMm() {
        return larguraDunnageMm;
    }

    public void setLarguraDunnageMm(Double larguraDunnageMm) {
        this.larguraDunnageMm = larguraDunnageMm;
    }

    public Double getComprimentoContatoDunnageMm() {
        return comprimentoContatoDunnageMm;
    }

    public void setComprimentoContatoDunnageMm(Double comprimentoContatoDunnageMm) {
        this.comprimentoContatoDunnageMm = comprimentoContatoDunnageMm;
    }

    public Integer getQuantidadeCalcos() {
        return quantidadeCalcos;
    }

    public void setQuantidadeCalcos(Integer quantidadeCalcos) {
        this.quantidadeCalcos = quantidadeCalcos;
    }

    public Double getEspacamentoFileirasMm() {
        return espacamentoFileirasMm;
    }

    public void setEspacamentoFileirasMm(Double espacamentoFileirasMm) {
        this.espacamentoFileirasMm = espacamentoFileirasMm;
    }

    public TipoLashing getTipoLashing() {
        return tipoLashing;
    }

    public void setTipoLashing(TipoLashing tipoLashing) {
        this.tipoLashing = tipoLashing;
    }

    public Double getForcaRequeridaLashingKn() {
        return forcaRequeridaLashingKn;
    }

    public void setForcaRequeridaLashingKn(Double forcaRequeridaLashingKn) {
        this.forcaRequeridaLashingKn = forcaRequeridaLashingKn;
    }

    public Double getCapacidadeLashingDisponivelKn() {
        return capacidadeLashingDisponivelKn;
    }

    public void setCapacidadeLashingDisponivelKn(Double capacidadeLashingDisponivelKn) {
        this.capacidadeLashingDisponivelKn = capacidadeLashingDisponivelKn;
    }

    public Integer getSequenciaDescarga() {
        return sequenciaDescarga;
    }

    public void setSequenciaDescarga(Integer sequenciaDescarga) {
        this.sequenciaDescarga = sequenciaDescarga;
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

    public String getAlertaTanktop() {
        return alertaTanktop;
    }

    public void setAlertaTanktop(String alertaTanktop) {
        this.alertaTanktop = alertaTanktop;
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
}
