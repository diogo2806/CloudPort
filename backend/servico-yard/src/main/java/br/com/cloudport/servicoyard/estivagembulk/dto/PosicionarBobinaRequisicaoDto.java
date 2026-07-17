package br.com.cloudport.servicoyard.estivagembulk.dto;

import br.com.cloudport.servicoyard.estivagembulk.modelo.TipoLashing;
import java.util.ArrayList;
import java.util.List;

public class PosicionarBobinaRequisicaoDto {

    private Long bobinaId;
    private Long poraoId;
    private Long setorId;
    private int camada;
    private Double posicaoX;
    private Double posicaoY;
    private Double anguloInclinacao;
    private Double espessuraDunnageMm;
    private Integer quantidadeLinhasDunnage;
    private Double larguraDunnageMm;
    private Double comprimentoContatoDunnageMm;
    private Integer quantidadeCalcos;
    private Double espacamentoFileirasMm;
    private TipoLashing tipoLashing;
    private Double forcaRequeridaLashingKn;
    private Integer sequenciaDescarga;
    private String referenciaRegra;
    private String versaoEspecificacao;
    private String responsavelValidacao;
    private List<MaterialLashingDto> materiaisLashing = new ArrayList<>();

    public PosicionarBobinaRequisicaoDto() {
    }

    public PosicionarBobinaRequisicaoDto(Long bobinaId, Long poraoId, Long setorId, int camada,
            double posicaoX, double posicaoY, double espessuraDunnageMm, TipoLashing tipoLashing) {
        this.bobinaId = bobinaId;
        this.poraoId = poraoId;
        this.setorId = setorId;
        this.camada = camada;
        this.posicaoX = posicaoX;
        this.posicaoY = posicaoY;
        this.espessuraDunnageMm = espessuraDunnageMm;
        this.tipoLashing = tipoLashing;
    }

    public Long getBobinaId() {
        return bobinaId;
    }

    public void setBobinaId(Long bobinaId) {
        this.bobinaId = bobinaId;
    }

    public Long getPoraoId() {
        return poraoId;
    }

    public void setPoraoId(Long poraoId) {
        this.poraoId = poraoId;
    }

    public Long getSetorId() {
        return setorId;
    }

    public void setSetorId(Long setorId) {
        this.setorId = setorId;
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

    public List<MaterialLashingDto> getMateriaisLashing() {
        return materiaisLashing;
    }

    public void setMateriaisLashing(List<MaterialLashingDto> materiaisLashing) {
        this.materiaisLashing = materiaisLashing;
    }
}
