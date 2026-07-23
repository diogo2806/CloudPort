package br.com.cloudport.servicorail.ferrovia.modelo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OrderColumn;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;

@Entity
@Table(name = "trem_mestre", uniqueConstraints = @UniqueConstraint(
        name = "uk_trem_mestre_operadora_identificador",
        columnNames = {"operadora_ferroviaria", "identificador"}))
public class TremMestre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "versao", nullable = false)
    private Long versao;

    @Column(name = "identificador", nullable = false, length = 40)
    private String identificador;

    @Column(name = "operadora_ferroviaria", nullable = false, length = 80)
    private String operadoraFerroviaria;

    @Column(name = "nome_operacional", nullable = false, length = 120)
    private String nomeOperacional;

    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;

    @Column(name = "observacoes", length = 1000)
    private String observacoes;

    @Column(name = "criado_por", nullable = false, length = 120)
    private String criadoPor;

    @Column(name = "alterado_por", nullable = false, length = 120)
    private String alteradoPor;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "alterado_em", nullable = false)
    private LocalDateTime alteradoEm;

    @ElementCollection
    @CollectionTable(name = "trem_mestre_vagao",
            joinColumns = @JoinColumn(name = "trem_mestre_id"),
            uniqueConstraints = {
                    @UniqueConstraint(name = "uk_trem_mestre_vagao_posicao",
                            columnNames = {"trem_mestre_id", "posicao_no_trem"}),
                    @UniqueConstraint(name = "uk_trem_mestre_vagao_identificador",
                            columnNames = {"trem_mestre_id", "identificador_vagao"})
            })
    @OrderColumn(name = "ordem_vagao")
    private List<VagaoVisita> composicaoPadrao = new ArrayList<>();

    public Long getId() { return id; }
    public Long getVersao() { return versao; }
    public String getIdentificador() { return identificador; }
    public void setIdentificador(String identificador) { this.identificador = identificador; }
    public String getOperadoraFerroviaria() { return operadoraFerroviaria; }
    public void setOperadoraFerroviaria(String operadoraFerroviaria) { this.operadoraFerroviaria = operadoraFerroviaria; }
    public String getNomeOperacional() { return nomeOperacional; }
    public void setNomeOperacional(String nomeOperacional) { this.nomeOperacional = nomeOperacional; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
    public String getCriadoPor() { return criadoPor; }
    public void setCriadoPor(String criadoPor) { this.criadoPor = criadoPor; }
    public String getAlteradoPor() { return alteradoPor; }
    public void setAlteradoPor(String alteradoPor) { this.alteradoPor = alteradoPor; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getAlteradoEm() { return alteradoEm; }
    public List<VagaoVisita> getComposicaoPadrao() { return composicaoPadrao; }

    public void definirComposicaoPadrao(List<VagaoVisita> composicaoPadrao) {
        this.composicaoPadrao.clear();
        if (composicaoPadrao != null) {
            this.composicaoPadrao.addAll(composicaoPadrao);
        }
    }

    @PrePersist
    public void aoCriar() {
        LocalDateTime agora = LocalDateTime.now();
        criadoEm = agora;
        alteradoEm = agora;
        if (criadoPor == null) {
            criadoPor = "sistema";
        }
        if (alteradoPor == null) {
            alteradoPor = criadoPor;
        }
    }

    @PreUpdate
    public void aoAtualizar() {
        alteradoEm = LocalDateTime.now();
    }
}
