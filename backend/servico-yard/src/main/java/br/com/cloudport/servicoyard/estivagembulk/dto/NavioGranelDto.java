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

    public NavioGranelDto() {
    }

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
        this.id = id;
        this.navioCadastroId = navioCadastroId;
        this.versaoPerfil = versaoPerfil;
        this.versaoNavioCanonico = versaoNavioCanonico;
        this.imo = imo;
        this.nome = nome;
        this.classe = classe;
        this.lpp = lpp;
        this.boca = boca;
        this.calado = calado;
        this.deslocamento = deslocamento;
        this.gm = gm;
        this.bmMaxPermitido = bmMaxPermitido;
        this.sfMaxPermitido = sfMaxPermitido;
        this.isTemplate = isTemplate;
        this.totalPoroes = totalPoroes;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getNavioCadastroId() { return navioCadastroId; }
    public void setNavioCadastroId(Long navioCadastroId) { this.navioCadastroId = navioCadastroId; }
    public Long getVersaoPerfil() { return versaoPerfil; }
    public void setVersaoPerfil(Long versaoPerfil) { this.versaoPerfil = versaoPerfil; }
    public Long getVersaoNavioCanonico() { return versaoNavioCanonico; }
    public void setVersaoNavioCanonico(Long versaoNavioCanonico) { this.versaoNavioCanonico = versaoNavioCanonico; }
    public String getImo() { return imo; }
    public void setImo(String imo) { this.imo = imo; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getClasse() { return classe; }
    public void setClasse(String classe) { this.classe = classe; }
    public Double getLpp() { return lpp; }
    public void setLpp(Double lpp) { this.lpp = lpp; }
    public Double getBoca() { return boca; }
    public void setBoca(Double boca) { this.boca = boca; }
    public Double getCalado() { return calado; }
    public void setCalado(Double calado) { this.calado = calado; }
    public Double getDeslocamento() { return deslocamento; }
    public void setDeslocamento(Double deslocamento) { this.deslocamento = deslocamento; }
    public Double getGm() { return gm; }
    public void setGm(Double gm) { this.gm = gm; }
    public Double getBmMaxPermitido() { return bmMaxPermitido; }
    public void setBmMaxPermitido(Double bmMaxPermitido) { this.bmMaxPermitido = bmMaxPermitido; }
    public Double getSfMaxPermitido() { return sfMaxPermitido; }
    public void setSfMaxPermitido(Double sfMaxPermitido) { this.sfMaxPermitido = sfMaxPermitido; }
    public boolean isTemplate() { return isTemplate; }
    public void setTemplate(boolean isTemplate) { this.isTemplate = isTemplate; }
    public int getTotalPoroes() { return totalPoroes; }
    public void setTotalPoroes(int totalPoroes) { this.totalPoroes = totalPoroes; }
    public List<PoraoNavioDto> getPoroes() { return poroes; }
    public void setPoroes(List<PoraoNavioDto> poroes) { this.poroes = poroes; }
}
