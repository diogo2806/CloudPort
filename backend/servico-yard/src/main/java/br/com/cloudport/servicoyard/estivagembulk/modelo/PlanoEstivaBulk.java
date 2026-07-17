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
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Version;

@Entity
@Table(name = "plano_estiva_bulk")
public class PlanoEstivaBulk {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "navio_granel_id") private NavioGranel navio;
    @Column(name = "navio_cadastro_id") private Long navioCadastroId;
    @Column(name = "visita_navio_id") private Long visitaNavioId;
    @Column(name = "codigo_visita", length = 60) private String codigoVisita;
    @Column(name = "versao_perfil_navio") private Long versaoPerfilNavio;
    @Column(name = "versao_navio_canonico") private Long versaoNavioCanonico;
    @Column(name = "versao_visita") private Long versaoVisita;
    @Column(name = "codigo_viagem", length = 30) private String codigoViagem;
    @Column(name = "porto_carga", length = 10) private String portoCarga;
    @Column(name = "porto_descarga", length = 10) private String portoDescarga;
    @Enumerated(EnumType.STRING) @Column(length = 25) private StatusPlanoEstiva status;
    @Column(name = "bm_max_calculado") private Double bmMaxCalculado;
    @Column(name = "sf_max_calculado") private Double sfMaxCalculado;
    @Column(name = "trim_calculado") private Double trimCalculado;
    @Column(name = "list_calculado") private Double listCalculado;
    @Column(name = "calado_saida") private Double caladoSaida;
    @Column(name = "versao_validacao_seguranca") private Long versaoValidacaoSeguranca;
    @Column(name = "versao_especificacao_seguranca", length = 60) private String versaoEspecificacaoSeguranca;
    @Column(name = "referencia_regra_seguranca", length = 120) private String referenciaRegraSeguranca;
    @Column(name = "validado_por_seguranca", length = 100) private String validadoPorSeguranca;
    @Column(name = "validado_em_seguranca") private LocalDateTime validadoEmSeguranca;
    @Enumerated(EnumType.STRING) @Column(name = "resultado_validacao_seguranca", length = 20)
    private ResultadoValidacaoSeguranca resultadoValidacaoSeguranca = ResultadoValidacaoSeguranca.PENDENTE;
    @Version private Long versao;
    @OneToMany(mappedBy = "plano", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<BobinaManifesto> bobinas = new ArrayList<>();
    @OneToMany(mappedBy = "plano", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<PosicaoBobina> posicoes = new ArrayList<>();
    @OneToMany(mappedBy = "plano", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<MaterialLashingBulk> materiais = new ArrayList<>();
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
    public PlanoEstivaBulk() { status = StatusPlanoEstiva.RASCUNHO; }
    @PrePersist @PreUpdate void touch() { LocalDateTime now = LocalDateTime.now(); if (criadoEm == null) criadoEm = now; atualizadoEm = now; }
    public Long getId() { return id; } public void setId(Long v) { id = v; }
    public NavioGranel getNavio() { return navio; } public void setNavio(NavioGranel v) { navio = v; }
    public Long getNavioCadastroId() { return navioCadastroId; } public void setNavioCadastroId(Long v) { navioCadastroId = v; }
    public Long getVisitaNavioId() { return visitaNavioId; } public void setVisitaNavioId(Long v) { visitaNavioId = v; }
    public String getCodigoVisita() { return codigoVisita; } public void setCodigoVisita(String v) { codigoVisita = v; }
    public Long getVersaoPerfilNavio() { return versaoPerfilNavio; } public void setVersaoPerfilNavio(Long v) { versaoPerfilNavio = v; }
    public Long getVersaoNavioCanonico() { return versaoNavioCanonico; } public void setVersaoNavioCanonico(Long v) { versaoNavioCanonico = v; }
    public Long getVersaoVisita() { return versaoVisita; } public void setVersaoVisita(Long v) { versaoVisita = v; }
    public String getCodigoViagem() { return codigoViagem; } public void setCodigoViagem(String v) { codigoViagem = v; }
    public String getPortoCarga() { return portoCarga; } public void setPortoCarga(String v) { portoCarga = v; }
    public String getPortoDescarga() { return portoDescarga; } public void setPortoDescarga(String v) { portoDescarga = v; }
    public StatusPlanoEstiva getStatus() { return status; } public void setStatus(StatusPlanoEstiva v) { status = v; }
    public Double getBmMaxCalculado() { return bmMaxCalculado; } public void setBmMaxCalculado(Double v) { bmMaxCalculado = v; }
    public Double getSfMaxCalculado() { return sfMaxCalculado; } public void setSfMaxCalculado(Double v) { sfMaxCalculado = v; }
    public Double getTrimCalculado() { return trimCalculado; } public void setTrimCalculado(Double v) { trimCalculado = v; }
    public Double getListCalculado() { return listCalculado; } public void setListCalculado(Double v) { listCalculado = v; }
    public Double getCaladoSaida() { return caladoSaida; } public void setCaladoSaida(Double v) { caladoSaida = v; }
    public Long getVersaoValidacaoSeguranca() { return versaoValidacaoSeguranca; } public void setVersaoValidacaoSeguranca(Long v) { versaoValidacaoSeguranca = v; }
    public String getVersaoEspecificacaoSeguranca() { return versaoEspecificacaoSeguranca; } public void setVersaoEspecificacaoSeguranca(String v) { versaoEspecificacaoSeguranca = v; }
    public String getReferenciaRegraSeguranca() { return referenciaRegraSeguranca; } public void setReferenciaRegraSeguranca(String v) { referenciaRegraSeguranca = v; }
    public String getValidadoPorSeguranca() { return validadoPorSeguranca; } public void setValidadoPorSeguranca(String v) { validadoPorSeguranca = v; }
    public LocalDateTime getValidadoEmSeguranca() { return validadoEmSeguranca; } public void setValidadoEmSeguranca(LocalDateTime v) { validadoEmSeguranca = v; }
    public ResultadoValidacaoSeguranca getResultadoValidacaoSeguranca() { return resultadoValidacaoSeguranca; }
    public void setResultadoValidacaoSeguranca(ResultadoValidacaoSeguranca v) { resultadoValidacaoSeguranca = v; }
    public Long getVersao() { return versao; } public void setVersao(Long v) { versao = v; }
    public List<BobinaManifesto> getBobinas() { return bobinas; } public void setBobinas(List<BobinaManifesto> v) { bobinas = v; }
    public List<PosicaoBobina> getPosicoes() { return posicoes; } public void setPosicoes(List<PosicaoBobina> v) { posicoes = v; }
    public List<MaterialLashingBulk> getMateriais() { return materiais; } public void setMateriais(List<MaterialLashingBulk> v) { materiais = v; }
    public LocalDateTime getCriadoEm() { return criadoEm; } public void setCriadoEm(LocalDateTime v) { criadoEm = v; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; } public void setAtualizadoEm(LocalDateTime v) { atualizadoEm = v; }
}
