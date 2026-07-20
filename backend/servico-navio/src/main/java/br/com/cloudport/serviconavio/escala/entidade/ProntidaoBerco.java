package br.com.cloudport.serviconavio.escala.entidade;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(
        name = "prontidao_berco",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_prontidao_berco_escala_versao",
                columnNames = {"escala_id", "versao_checklist"}))
public class ProntidaoBerco {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "escala_id", nullable = false)
    private Escala escala;

    @Column(name = "versao_checklist", nullable = false)
    private Integer versaoChecklist;

    @Column(nullable = false, length = 40)
    private String berco;

    @Column(name = "calado_metros", nullable = false, precision = 10, scale = 3)
    private BigDecimal caladoMetros;

    @Column(name = "berco_confirmado", nullable = false)
    private Boolean bercoConfirmado;

    @Column(name = "calado_confirmado", nullable = false)
    private Boolean caladoConfirmado;

    @Column(name = "defensas_confirmadas", nullable = false)
    private Boolean defensasConfirmadas;

    @Column(name = "amarracao_confirmada", nullable = false)
    private Boolean amarracaoConfirmada;

    @Column(name = "acesso_confirmado", nullable = false)
    private Boolean acessoConfirmado;

    @Column(name = "recursos_confirmados", nullable = false)
    private Boolean recursosConfirmados;

    @Column(name = "restricoes_avaliadas", nullable = false)
    private Boolean restricoesAvaliadas;

    @Column(name = "liberacoes_confirmadas", nullable = false)
    private Boolean liberacoesConfirmadas;

    @Column(length = 1000)
    private String recursos;

    @Column(length = 1000)
    private String restricoes;

    @Column(length = 1000)
    private String liberacoes;

    @Column(length = 1000)
    private String observacoes;

    @Column(nullable = false, length = 120)
    private String responsavel;

    @Column(name = "confirmado_em", nullable = false)
    private LocalDateTime confirmadoEm;

    public List<String> motivosBloqueio() {
        List<String> motivos = new ArrayList<>();
        adicionarSePendente(motivos, bercoConfirmado, "Berço não confirmado.");
        adicionarSePendente(motivos, caladoConfirmado, "Calado operacional não confirmado.");
        adicionarSePendente(motivos, defensasConfirmadas, "Defensas não confirmadas.");
        adicionarSePendente(motivos, amarracaoConfirmada, "Amarração não confirmada.");
        adicionarSePendente(motivos, acessoConfirmado, "Acesso ao navio não confirmado.");
        adicionarSePendente(motivos, recursosConfirmados, "Recursos operacionais não confirmados.");
        adicionarSePendente(motivos, restricoesAvaliadas, "Restrições operacionais não avaliadas.");
        adicionarSePendente(motivos, liberacoesConfirmadas, "Liberações operacionais não confirmadas.");
        return motivos;
    }

    public boolean isPronto() {
        return motivosBloqueio().isEmpty();
    }

    private void adicionarSePendente(List<String> motivos, Boolean confirmado, String mensagem) {
        if (!Boolean.TRUE.equals(confirmado)) {
            motivos.add(mensagem);
        }
    }

    @PrePersist
    public void aoCriar() {
        if (confirmadoEm == null) {
            confirmadoEm = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Escala getEscala() { return escala; }
    public void setEscala(Escala escala) { this.escala = escala; }
    public Integer getVersaoChecklist() { return versaoChecklist; }
    public void setVersaoChecklist(Integer versaoChecklist) { this.versaoChecklist = versaoChecklist; }
    public String getBerco() { return berco; }
    public void setBerco(String berco) { this.berco = berco; }
    public BigDecimal getCaladoMetros() { return caladoMetros; }
    public void setCaladoMetros(BigDecimal caladoMetros) { this.caladoMetros = caladoMetros; }
    public Boolean getBercoConfirmado() { return bercoConfirmado; }
    public void setBercoConfirmado(Boolean bercoConfirmado) { this.bercoConfirmado = bercoConfirmado; }
    public Boolean getCaladoConfirmado() { return caladoConfirmado; }
    public void setCaladoConfirmado(Boolean caladoConfirmado) { this.caladoConfirmado = caladoConfirmado; }
    public Boolean getDefensasConfirmadas() { return defensasConfirmadas; }
    public void setDefensasConfirmadas(Boolean defensasConfirmadas) { this.defensasConfirmadas = defensasConfirmadas; }
    public Boolean getAmarracaoConfirmada() { return amarracaoConfirmada; }
    public void setAmarracaoConfirmada(Boolean amarracaoConfirmada) { this.amarracaoConfirmada = amarracaoConfirmada; }
    public Boolean getAcessoConfirmado() { return acessoConfirmado; }
    public void setAcessoConfirmado(Boolean acessoConfirmado) { this.acessoConfirmado = acessoConfirmado; }
    public Boolean getRecursosConfirmados() { return recursosConfirmados; }
    public void setRecursosConfirmados(Boolean recursosConfirmados) { this.recursosConfirmados = recursosConfirmados; }
    public Boolean getRestricoesAvaliadas() { return restricoesAvaliadas; }
    public void setRestricoesAvaliadas(Boolean restricoesAvaliadas) { this.restricoesAvaliadas = restricoesAvaliadas; }
    public Boolean getLiberacoesConfirmadas() { return liberacoesConfirmadas; }
    public void setLiberacoesConfirmadas(Boolean liberacoesConfirmadas) { this.liberacoesConfirmadas = liberacoesConfirmadas; }
    public String getRecursos() { return recursos; }
    public void setRecursos(String recursos) { this.recursos = recursos; }
    public String getRestricoes() { return restricoes; }
    public void setRestricoes(String restricoes) { this.restricoes = restricoes; }
    public String getLiberacoes() { return liberacoes; }
    public void setLiberacoes(String liberacoes) { this.liberacoes = liberacoes; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
    public String getResponsavel() { return responsavel; }
    public void setResponsavel(String responsavel) { this.responsavel = responsavel; }
    public LocalDateTime getConfirmadoEm() { return confirmadoEm; }
    public void setConfirmadoEm(LocalDateTime confirmadoEm) { this.confirmadoEm = confirmadoEm; }
}
