package br.com.cloudport.servicoyard.estivagembulk.dto;

import java.util.ArrayList;
import java.util.List;

public class PosicaoBobinaDto {

    private Long id;
    private Long bobinaId;
    private String codigoBobina;
    private Double pesoKg;
    private Long poraoId;
    private Integer poraoNumero;
    private Long setorId;
    private String setorNome;
    private Integer camada;
    private Double posicaoX;
    private Double posicaoY;
    private Double anguloInclinacao;
    private Double espessuraDunnageMm;
    private Integer quantidadeLinhasDunnage;
    private Double larguraDunnageMm;
    private Double comprimentoContatoDunnageMm;
    private Integer quantidadeCalcos;
    private Double espacamentoFileirasMm;
    private String tipoLashing;
    private Double forcaRequeridaLashingKn;
    private Double capacidadeLashingDisponivelKn;
    private Integer sequenciaDescarga;
    private String referenciaRegra;
    private String versaoEspecificacao;
    private String responsavelValidacao;
    private String resultadoValidacao;
    private String alertaTanktop;
    private List<MaterialLashingDto> materiaisLashing = new ArrayList<>();

    public PosicaoBobinaDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBobinaId() {
        return bobinaId;
    }

    public void setBobinaId(Long bobinaId) {
        this.bobinaId = bobinaId;
    }

    public String getCodigoBobina() {
        return codigoBobina;
    }

    public void setCodigoBobina(String codigoBobina) {
        this.codigoBobina = codigoBobina;
    }

    public Double getPesoKg() {
        return pesoKg;
    }

    public void setPesoKg(Double pesoKg) {
        this.pesoKg = pesoKg;
    }

    public Long getPoraoId() {
        return poraoId;
    }

    public void setPoraoId(Long poraoId) {
        this.poraoId = poraoId;
    }

    public Integer getPoraoNumero() {
        return poraoNumero;
    }

    public void setPoraoNumero(Integer poraoNumero) {
        this.poraoNumero = poraoNumero;
    }

    public Long getSetorId() {
        return setorId;
    }

    public void setSetorId(Long setorId) {
        this.setorId = setorId;
    }

    public String getSetorNome() {
        return setorNome;
    }

    public void setSetorNome(String setorNome) {
        this.setorNome = setorNome;
    }

    public Integer getCamada() {
        return camada;
    }

    public void setCamada(Integer camada) {
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

    public String getTipoLashing() {
        return tipoLashing;
    }

    public void setTipoLashing(String tipoLashing) {
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

    public String getResultadoValidacao() {
        return resultadoValidacao;
    }

    public void setResultadoValidacao(String resultadoValidacao) {
        this.resultadoValidacao = resultadoValidacao;
    }

    public String getAlertaTanktop() {
        return alertaTanktop;
    }

    public void setAlertaTanktop(String alertaTanktop) {
        this.alertaTanktop = alertaTanktop;
    }

    public List<MaterialLashingDto> getMateriaisLashing() {
        return materiaisLashing;
    }

    public void setMateriaisLashing(List<MaterialLashingDto> materiaisLashing) {
        this.materiaisLashing = materiaisLashing;
    }
}
