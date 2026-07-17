package br.com.cloudport.serviconaviosiderurgico.dominio;

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
import javax.persistence.Version;

@Entity
@Table(name = "visita_navio")
public class VisitaNavio {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "navio_id", nullable = false) private NavioSiderurgico navio;
    @Column(name = "codigo_visita", nullable = false, unique = true, length = 60) private String codigoVisita;
    @Column(name = "viagem_entrada", length = 40) private String viagemEntrada;
    @Column(name = "viagem_saida", length = 40) private String viagemSaida;
    @Column(name = "linha_operadora", length = 80) private String linhaOperadora;
    @Column(name = "terminal_facility", length = 80) private String terminalFacility;
    @Column(name = "berco_previsto", length = 40) private String bercoPrevisto;
    @Column(name = "berco_atual", length = 40) private String bercoAtual;
    private LocalDateTime eta; private LocalDateTime ata; private LocalDateTime etb; private LocalDateTime atb;
    @Column(name = "inicio_operacao") private LocalDateTime inicioOperacao;
    @Column(name = "fim_operacao") private LocalDateTime fimOperacao;
    private LocalDateTime etd; private LocalDateTime atd;
    @Column(name = "janela_recebimento_inicio") private LocalDateTime janelaRecebimentoInicio;
    @Column(name = "janela_recebimento_fim") private LocalDateTime janelaRecebimentoFim;
    @Column(name = "cutoff_operacional") private LocalDateTime cutoffOperacional;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private FaseVisitaNavio fase = FaseVisitaNavio.PREVISTA;
    @Column(length = 1000) private String observacoes;
    @Version @Column(name = "versao", nullable = false) private Long versao;
    @Column(name = "criado_em", nullable = false) private LocalDateTime criadoEm;
    @Column(name = "atualizado_em", nullable = false) private LocalDateTime atualizadoEm;
    @PrePersist void prePersist() { LocalDateTime agora = LocalDateTime.now(); criadoEm = agora; atualizadoEm = agora; }
    @PreUpdate void preUpdate() { atualizadoEm = LocalDateTime.now(); }
    public Long getId() { return id; }
    public NavioSiderurgico getNavio() { return navio; }
    public void setNavio(NavioSiderurgico navio) { this.navio = navio; }
    public String getCodigoVisita() { return codigoVisita; }
    public void setCodigoVisita(String codigoVisita) { this.codigoVisita = codigoVisita; }
    public String getViagemEntrada() { return viagemEntrada; }
    public void setViagemEntrada(String viagemEntrada) { this.viagemEntrada = viagemEntrada; }
    public String getViagemSaida() { return viagemSaida; }
    public void setViagemSaida(String viagemSaida) { this.viagemSaida = viagemSaida; }
    public String getLinhaOperadora() { return linhaOperadora; }
    public void setLinhaOperadora(String linhaOperadora) { this.linhaOperadora = linhaOperadora; }
    public String getTerminalFacility() { return terminalFacility; }
    public void setTerminalFacility(String terminalFacility) { this.terminalFacility = terminalFacility; }
    public String getBercoPrevisto() { return bercoPrevisto; }
    public void setBercoPrevisto(String bercoPrevisto) { this.bercoPrevisto = bercoPrevisto; }
    public String getBercoAtual() { return bercoAtual; }
    public void setBercoAtual(String bercoAtual) { this.bercoAtual = bercoAtual; }
    public LocalDateTime getEta() { return eta; }
    public void setEta(LocalDateTime eta) { this.eta = eta; }
    public LocalDateTime getAta() { return ata; }
    public void setAta(LocalDateTime ata) { this.ata = ata; }
    public LocalDateTime getEtb() { return etb; }
    public void setEtb(LocalDateTime etb) { this.etb = etb; }
    public LocalDateTime getAtb() { return atb; }
    public void setAtb(LocalDateTime atb) { this.atb = atb; }
    public LocalDateTime getInicioOperacao() { return inicioOperacao; }
    public void setInicioOperacao(LocalDateTime inicioOperacao) { this.inicioOperacao = inicioOperacao; }
    public LocalDateTime getFimOperacao() { return fimOperacao; }
    public void setFimOperacao(LocalDateTime fimOperacao) { this.fimOperacao = fimOperacao; }
    public LocalDateTime getEtd() { return etd; }
    public void setEtd(LocalDateTime etd) { this.etd = etd; }
    public LocalDateTime getAtd() { return atd; }
    public void setAtd(LocalDateTime atd) { this.atd = atd; }
    public LocalDateTime getJanelaRecebimentoInicio() { return janelaRecebimentoInicio; }
    public void setJanelaRecebimentoInicio(LocalDateTime valor) { this.janelaRecebimentoInicio = valor; }
    public LocalDateTime getJanelaRecebimentoFim() { return janelaRecebimentoFim; }
    public void setJanelaRecebimentoFim(LocalDateTime valor) { this.janelaRecebimentoFim = valor; }
    public LocalDateTime getCutoffOperacional() { return cutoffOperacional; }
    public void setCutoffOperacional(LocalDateTime valor) { this.cutoffOperacional = valor; }
    public FaseVisitaNavio getFase() { return fase; }
    public void setFase(FaseVisitaNavio fase) { this.fase = fase; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
    public Long getVersao() { return versao; }
    public void setVersao(Long versao) { this.versao = versao; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
}
