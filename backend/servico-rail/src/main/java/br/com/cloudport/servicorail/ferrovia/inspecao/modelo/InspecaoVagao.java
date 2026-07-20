package br.com.cloudport.servicorail.ferrovia.inspecao.modelo;

import br.com.cloudport.servicorail.ferrovia.modelo.VisitaTrem;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OrderColumn;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Version;

@Entity
@Table(name = "inspecao_vagao")
public class InspecaoVagao {

    public enum StatusInspecaoVagao {
        APROVADA,
        REPROVADA,
        LIBERADA_OVERRIDE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visita_trem_id", nullable = false)
    private VisitaTrem visitaTrem;

    @Column(name = "identificador_vagao", nullable = false, length = 35)
    private String identificadorVagao;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private StatusInspecaoVagao status;

    @Column(name = "rodas_aprovadas", nullable = false)
    private Boolean rodasAprovadas;

    @Column(name = "freios_aprovados", nullable = false)
    private Boolean freiosAprovados;

    @Column(name = "engates_aprovados", nullable = false)
    private Boolean engatesAprovados;

    @Column(name = "estrutura_aprovada", nullable = false)
    private Boolean estruturaAprovada;

    @Column(name = "lacres_aprovados", nullable = false)
    private Boolean lacresAprovados;

    @Column(name = "responsavel", nullable = false, length = 120)
    private String responsavel;

    @Column(name = "observacao", length = 1000)
    private String observacao;

    @Column(name = "inspecionado_em", nullable = false)
    private LocalDateTime inspecionadoEm;

    @Column(name = "override_por", length = 120)
    private String overridePor;

    @Column(name = "override_motivo", length = 500)
    private String overrideMotivo;

    @Column(name = "liberado_em")
    private LocalDateTime liberadoEm;

    @ElementCollection
    @CollectionTable(name = "defeito_inspecao_vagao",
            joinColumns = @JoinColumn(name = "inspecao_vagao_id"))
    @OrderColumn(name = "ordem_defeito")
    private List<DefeitoInspecaoVagao> defeitos = new ArrayList<>();

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @Version
    @Column(name = "versao", nullable = false)
    private Long versao;

    @PrePersist
    public void aoCriar() {
        LocalDateTime agora = LocalDateTime.now();
        this.criadoEm = agora;
        this.atualizadoEm = agora;
        if (this.inspecionadoEm == null) {
            this.inspecionadoEm = agora;
        }
    }

    @PreUpdate
    public void aoAtualizar() {
        this.atualizadoEm = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public VisitaTrem getVisitaTrem() {
        return visitaTrem;
    }

    public void setVisitaTrem(VisitaTrem visitaTrem) {
        this.visitaTrem = visitaTrem;
    }

    public String getIdentificadorVagao() {
        return identificadorVagao;
    }

    public void setIdentificadorVagao(String identificadorVagao) {
        this.identificadorVagao = identificadorVagao;
    }

    public StatusInspecaoVagao getStatus() {
        return status;
    }

    public void setStatus(StatusInspecaoVagao status) {
        this.status = status;
    }

    public Boolean getRodasAprovadas() {
        return rodasAprovadas;
    }

    public void setRodasAprovadas(Boolean rodasAprovadas) {
        this.rodasAprovadas = rodasAprovadas;
    }

    public Boolean getFreiosAprovados() {
        return freiosAprovados;
    }

    public void setFreiosAprovados(Boolean freiosAprovados) {
        this.freiosAprovados = freiosAprovados;
    }

    public Boolean getEngatesAprovados() {
        return engatesAprovados;
    }

    public void setEngatesAprovados(Boolean engatesAprovados) {
        this.engatesAprovados = engatesAprovados;
    }

    public Boolean getEstruturaAprovada() {
        return estruturaAprovada;
    }

    public void setEstruturaAprovada(Boolean estruturaAprovada) {
        this.estruturaAprovada = estruturaAprovada;
    }

    public Boolean getLacresAprovados() {
        return lacresAprovados;
    }

    public void setLacresAprovados(Boolean lacresAprovados) {
        this.lacresAprovados = lacresAprovados;
    }

    public String getResponsavel() {
        return responsavel;
    }

    public void setResponsavel(String responsavel) {
        this.responsavel = responsavel;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    public LocalDateTime getInspecionadoEm() {
        return inspecionadoEm;
    }

    public void setInspecionadoEm(LocalDateTime inspecionadoEm) {
        this.inspecionadoEm = inspecionadoEm;
    }

    public String getOverridePor() {
        return overridePor;
    }

    public void setOverridePor(String overridePor) {
        this.overridePor = overridePor;
    }

    public String getOverrideMotivo() {
        return overrideMotivo;
    }

    public void setOverrideMotivo(String overrideMotivo) {
        this.overrideMotivo = overrideMotivo;
    }

    public LocalDateTime getLiberadoEm() {
        return liberadoEm;
    }

    public void setLiberadoEm(LocalDateTime liberadoEm) {
        this.liberadoEm = liberadoEm;
    }

    public List<DefeitoInspecaoVagao> getDefeitos() {
        return defeitos;
    }

    public void definirDefeitos(List<DefeitoInspecaoVagao> defeitos) {
        this.defeitos.clear();
        if (defeitos != null) {
            this.defeitos.addAll(defeitos);
        }
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    public Long getVersao() {
        return versao;
    }
}
