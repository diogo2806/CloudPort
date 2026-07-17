package br.com.cloudport.servicoyard.estivagembulk.dto;

public class MaterialLashingDto {

    private Long id;
    private Long posicaoId;
    private String tipo;
    private Integer quantidade;
    private Double comprimentoM;
    private Double pesoUnitarioKg;
    private String pontoAmarracao;
    private Double capacidadeNominalKn;
    private Double cargaTrabalhoSeguraKn;
    private String certificado;
    private String referenciaRegra;
    private String versaoEspecificacao;
    private String responsavelValidacao;
    private String resultadoValidacao;
    private String descricao;

    public MaterialLashingDto() {
    }

    public MaterialLashingDto(String tipo, int quantidade, double comprimentoM, double pesoUnitarioKg,
            String descricao) {
        this.tipo = tipo;
        this.quantidade = quantidade;
        this.comprimentoM = comprimentoM;
        this.pesoUnitarioKg = pesoUnitarioKg;
        this.descricao = descricao;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPosicaoId() {
        return posicaoId;
    }

    public void setPosicaoId(Long posicaoId) {
        this.posicaoId = posicaoId;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(Integer quantidade) {
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

    public String getResultadoValidacao() {
        return resultadoValidacao;
    }

    public void setResultadoValidacao(String resultadoValidacao) {
        this.resultadoValidacao = resultadoValidacao;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }
}
