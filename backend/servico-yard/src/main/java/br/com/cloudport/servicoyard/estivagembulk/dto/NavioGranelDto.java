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
    private Double tpc;
    private Double lcb;
    private Double km;
    private Double mct1cm;
    private Double caladoMaximo;
    private Double trimMaximo;
    private Double bandaMaxima;
    private Double gmMinimo;
    private Double pesoLeveToneladas;
    private Double lcgPesoLeve;
    private Double tcgPesoLeve;
    private Double vcgPesoLeve;
    private Double pesoLastroToneladas;
    private Double lcgLastro;
    private Double tcgLastro;
    private Double vcgLastro;
    private Double bmMaxPermitido;
    private Double sfMaxPermitido;
    private String versaoDadosHidrostaticos;
    private String versaoDadosEstruturais;
    private String posicoesSecoes;
    private String pesoLeveSecoes;
    private String empuxoSecoes;
    private String limitesSfSecoes;
    private String limitesBmSecoes;
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
    public Double getTpc() { return tpc; }
    public void setTpc(Double tpc) { this.tpc = tpc; }
    public Double getLcb() { return lcb; }
    public void setLcb(Double lcb) { this.lcb = lcb; }
    public Double getKm() { return km; }
    public void setKm(Double km) { this.km = km; }
    public Double getMct1cm() { return mct1cm; }
    public void setMct1cm(Double mct1cm) { this.mct1cm = mct1cm; }
    public Double getCaladoMaximo() { return caladoMaximo; }
    public void setCaladoMaximo(Double caladoMaximo) { this.caladoMaximo = caladoMaximo; }
    public Double getTrimMaximo() { return trimMaximo; }
    public void setTrimMaximo(Double trimMaximo) { this.trimMaximo = trimMaximo; }
    public Double getBandaMaxima() { return bandaMaxima; }
    public void setBandaMaxima(Double bandaMaxima) { this.bandaMaxima = bandaMaxima; }
    public Double getGmMinimo() { return gmMinimo; }
    public void setGmMinimo(Double gmMinimo) { this.gmMinimo = gmMinimo; }
    public Double getPesoLeveToneladas() { return pesoLeveToneladas; }
    public void setPesoLeveToneladas(Double pesoLeveToneladas) { this.pesoLeveToneladas = pesoLeveToneladas; }
    public Double getLcgPesoLeve() { return lcgPesoLeve; }
    public void setLcgPesoLeve(Double lcgPesoLeve) { this.lcgPesoLeve = lcgPesoLeve; }
    public Double getTcgPesoLeve() { return tcgPesoLeve; }
    public void setTcgPesoLeve(Double tcgPesoLeve) { this.tcgPesoLeve = tcgPesoLeve; }
    public Double getVcgPesoLeve() { return vcgPesoLeve; }
    public void setVcgPesoLeve(Double vcgPesoLeve) { this.vcgPesoLeve = vcgPesoLeve; }
    public Double getPesoLastroToneladas() { return pesoLastroToneladas; }
    public void setPesoLastroToneladas(Double pesoLastroToneladas) { this.pesoLastroToneladas = pesoLastroToneladas; }
    public Double getLcgLastro() { return lcgLastro; }
    public void setLcgLastro(Double lcgLastro) { this.lcgLastro = lcgLastro; }
    public Double getTcgLastro() { return tcgLastro; }
    public void setTcgLastro(Double tcgLastro) { this.tcgLastro = tcgLastro; }
    public Double getVcgLastro() { return vcgLastro; }
    public void setVcgLastro(Double vcgLastro) { this.vcgLastro = vcgLastro; }
    public Double getBmMaxPermitido() { return bmMaxPermitido; }
    public void setBmMaxPermitido(Double bmMaxPermitido) { this.bmMaxPermitido = bmMaxPermitido; }
    public Double getSfMaxPermitido() { return sfMaxPermitido; }
    public void setSfMaxPermitido(Double sfMaxPermitido) { this.sfMaxPermitido = sfMaxPermitido; }
    public String getVersaoDadosHidrostaticos() { return versaoDadosHidrostaticos; }
    public void setVersaoDadosHidrostaticos(String versaoDadosHidrostaticos) { this.versaoDadosHidrostaticos = versaoDadosHidrostaticos; }
    public String getVersaoDadosEstruturais() { return versaoDadosEstruturais; }
    public void setVersaoDadosEstruturais(String versaoDadosEstruturais) { this.versaoDadosEstruturais = versaoDadosEstruturais; }
    public String getPosicoesSecoes() { return posicoesSecoes; }
    public void setPosicoesSecoes(String posicoesSecoes) { this.posicoesSecoes = posicoesSecoes; }
    public String getPesoLeveSecoes() { return pesoLeveSecoes; }
    public void setPesoLeveSecoes(String pesoLeveSecoes) { this.pesoLeveSecoes = pesoLeveSecoes; }
    public String getEmpuxoSecoes() { return empuxoSecoes; }
    public void setEmpuxoSecoes(String empuxoSecoes) { this.empuxoSecoes = empuxoSecoes; }
    public String getLimitesSfSecoes() { return limitesSfSecoes; }
    public void setLimitesSfSecoes(String limitesSfSecoes) { this.limitesSfSecoes = limitesSfSecoes; }
    public String getLimitesBmSecoes() { return limitesBmSecoes; }
    public void setLimitesBmSecoes(String limitesBmSecoes) { this.limitesBmSecoes = limitesBmSecoes; }
    public boolean isTemplate() { return isTemplate; }
    public void setTemplate(boolean isTemplate) { this.isTemplate = isTemplate; }
    public int getTotalPoroes() { return totalPoroes; }
    public void setTotalPoroes(int totalPoroes) { this.totalPoroes = totalPoroes; }
    public List<PoraoNavioDto> getPoroes() { return poroes; }
    public void setPoroes(List<PoraoNavioDto> poroes) { this.poroes = poroes; }
}
