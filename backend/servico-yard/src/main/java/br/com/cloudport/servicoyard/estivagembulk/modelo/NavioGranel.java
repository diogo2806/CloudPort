package br.com.cloudport.servicoyard.estivagembulk.modelo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;

@Entity
@Table(name = "navio_granel")
public class NavioGranel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "navio_cadastro_id")
    private Long navioCadastroId;

    @Column(name = "versao_perfil", nullable = false)
    private Long versaoPerfil = 1L;

    @Column(name = "versao_navio_canonico")
    private Long versaoNavioCanonico;

    @Column(length = 10)
    private String imo;

    @Column(nullable = false, length = 100)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ClasseNavio classe;

    @Column(name = "lpp")
    private Double lpp;

    private Double boca;
    private Double calado;
    private Double deslocamento;
    private Double gm;
    private Double tpc;
    private Double lcb;
    private Double km;

    @Column(name = "mct_1cm")
    private Double mct1cm;

    @Column(name = "calado_maximo")
    private Double caladoMaximo;

    @Column(name = "trim_maximo")
    private Double trimMaximo;

    @Column(name = "banda_maxima")
    private Double bandaMaxima;

    @Column(name = "gm_minimo")
    private Double gmMinimo;

    @Column(name = "peso_leve_t")
    private Double pesoLeveToneladas;

    @Column(name = "lcg_peso_leve")
    private Double lcgPesoLeve;

    @Column(name = "tcg_peso_leve")
    private Double tcgPesoLeve;

    @Column(name = "vcg_peso_leve")
    private Double vcgPesoLeve;

    @Column(name = "peso_lastro_t")
    private Double pesoLastroToneladas;

    @Column(name = "lcg_lastro")
    private Double lcgLastro;

    @Column(name = "tcg_lastro")
    private Double tcgLastro;

    @Column(name = "vcg_lastro")
    private Double vcgLastro;

    @Column(name = "bm_max_permitido")
    private Double bmMaxPermitido;

    @Column(name = "sf_max_permitido")
    private Double sfMaxPermitido;

    @Column(name = "versao_dados_hidrostaticos", length = 80)
    private String versaoDadosHidrostaticos;

    @Column(name = "versao_dados_estruturais", length = 80)
    private String versaoDadosEstruturais;

    @Column(name = "posicoes_secoes", columnDefinition = "TEXT")
    private String posicoesSecoes;

    @Column(name = "peso_leve_secoes", columnDefinition = "TEXT")
    private String pesoLeveSecoes;

    @Column(name = "empuxo_secoes", columnDefinition = "TEXT")
    private String empuxoSecoes;

    @Column(name = "limites_sf_secoes", columnDefinition = "TEXT")
    private String limitesSfSecoes;

    @Column(name = "limites_bm_secoes", columnDefinition = "TEXT")
    private String limitesBmSecoes;

    @Column(name = "is_template")
    private boolean isTemplate;

    @OneToMany(mappedBy = "navio", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<PoraoNavio> poroes = new ArrayList<>();

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
    public ClasseNavio getClasse() { return classe; }
    public void setClasse(ClasseNavio classe) { this.classe = classe; }
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
    public void setTemplate(boolean template) { isTemplate = template; }
    public List<PoraoNavio> getPoroes() { return poroes; }
    public void setPoroes(List<PoraoNavio> poroes) { this.poroes = poroes; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(LocalDateTime atualizadoEm) { this.atualizadoEm = atualizadoEm; }
}
