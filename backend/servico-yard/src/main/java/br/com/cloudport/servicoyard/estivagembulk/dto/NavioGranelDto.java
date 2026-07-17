package br.com.cloudport.servicoyard.estivagembulk.dto;

import java.util.ArrayList;
import java.util.List;

public class NavioGranelDto {
    private Long id;
    private Long navioCadastroId;
    private Long versaoPerfil;
    private Long versaoNavioCanonico;
    private String imo;
    private String nome;
    private String classe;
    private Double lpp;
    private Double boca;
    private Double calado;
    private Double deslocamento;
    private Double gm;
    private Double bmMaxPermitido;
    private Double sfMaxPermitido;
    private boolean isTemplate;
    private int totalPoroes;
    private List<PoraoNavioDto> poroes = new ArrayList<>();
    public NavioGranelDto() { }
    public NavioGranelDto(Long id, String imo, String nome, String classe, Double lpp, Double boca,
            Double calado, Double deslocamento, Double gm, Double bmMaxPermitido, Double sfMaxPermitido,
            boolean isTemplate, int totalPoroes) {
        this(id, null, null, null, imo, nome, classe, lpp, boca, calado, deslocamento, gm,
                bmMaxPermitido, sfMaxPermitido, isTemplate, totalPoroes);
    }
    public NavioGranelDto(Long id, Long navioCadastroId, Long versaoPerfil, Long versaoNavioCanonico,
            String imo, String nome, String classe, Double lpp, Double boca, Double calado,
            Double deslocamento, Double gm, Double bmMaxPermitido, Double sfMaxPermitido,
            boolean isTemplate, int totalPoroes) {
        this.id = id; this.navioCadastroId = navioCadastroId; this.versaoPerfil = versaoPerfil;
        this.versaoNavioCanonico = versaoNavioCanonico; this.imo = imo; this.nome = nome;
        this.classe = classe; this.lpp = lpp; this.boca = boca; this.calado = calado;
        this.deslocamento = deslocamento; this.gm = gm; this.bmMaxPermitido = bmMaxPermitido;
        this.sfMaxPermitido = sfMaxPermitido; this.isTemplate = isTemplate; this.totalPoroes = totalPoroes;
    }
    public Long getId() { return id; } public void setId(Long v) { id = v; }
    public Long getNavioCadastroId() { return navioCadastroId; } public void setNavioCadastroId(Long v) { navioCadastroId = v; }
    public Long getVersaoPerfil() { return versaoPerfil; } public void setVersaoPerfil(Long v) { versaoPerfil = v; }
    public Long getVersaoNavioCanonico() { return versaoNavioCanonico; } public void setVersaoNavioCanonico(Long v) { versaoNavioCanonico = v; }
    public String getImo() { return imo; } public void setImo(String v) { imo = v; }
    public String getNome() { return nome; } public void setNome(String v) { nome = v; }
    public String getClasse() { return classe; } public void setClasse(String v) { classe = v; }
    public Double getLpp() { return lpp; } public void setLpp(Double v) { lpp = v; }
    public Double getBoca() { return boca; } public void setBoca(Double v) { boca = v; }
    public Double getCalado() { return calado; } public void setCalado(Double v) { calado = v; }
    public Double getDeslocamento() { return deslocamento; } public void setDeslocamento(Double v) { deslocamento = v; }
    public Double getGm() { return gm; } public void setGm(Double v) { gm = v; }
    public Double getBmMaxPermitido() { return bmMaxPermitido; } public void setBmMaxPermitido(Double v) { bmMaxPermitido = v; }
    public Double getSfMaxPermitido() { return sfMaxPermitido; } public void setSfMaxPermitido(Double v) { sfMaxPermitido = v; }
    public boolean isTemplate() { return isTemplate; } public void setTemplate(boolean v) { isTemplate = v; }
    public int getTotalPoroes() { return totalPoroes; } public void setTotalPoroes(int v) { totalPoroes = v; }
    public List<PoraoNavioDto> getPoroes() { return poroes; } public void setPoroes(List<PoraoNavioDto> v) { poroes = v; }
}
