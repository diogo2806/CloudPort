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
@Table(name = "trem_cadastro", uniqueConstraints = @UniqueConstraint(name = "uk_trem_cadastro_operadora_identificador", columnNames = {"operadora_ferroviaria", "identificador"}))
public class TremCadastro {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long versao;

    @Column(nullable = false, length = 40)
    private String identificador;

    @Column(name = "operadora_ferroviaria", nullable = false, length = 80)
    private String operadoraFerroviaria;

    @Column(length = 120)
    private String descricao;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(length = 500)
    private String observacoes;

    @Column(name = "criado_por", length = 120)
    private String criadoPor;

    @Column(name = "alterado_por", length = 120)
    private String alteradoPor;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @ElementCollection
    @CollectionTable(name = "trem_cadastro_vagao", joinColumns = @JoinColumn(name = "trem_cadastro_id"), uniqueConstraints = @UniqueConstraint(name = "uk_trem_cadastro_vagao_identificador", columnNames = {"trem_cadastro_id", "identificador_vagao"}))
    @OrderColumn(name = "ordem_vagao")
    private List<VagaoVisita> composicaoPadrao = new ArrayList<>();

    public Long getId() { return id; }
    public Long getVersao() { return versao; }
    public String getIdentificador() { return identificador; }
    public void setIdentificador(String identificador) { this.identificador = identificador; }
    public String getOperadoraFerroviaria() { return operadoraFerroviaria; }
    public void setOperadoraFerroviaria(String operadoraFerroviaria) { this.operadoraFerroviaria = operadoraFerroviaria; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
    public String getCriadoPor() { return criadoPor; }
    public void setCriadoPor(String criadoPor) { this.criadoPor = criadoPor; }
    public String getAlteradoPor() { return alteradoPor; }
    public void setAlteradoPor(String alteradoPor) { this.alteradoPor = alteradoPor; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public List<VagaoVisita> getComposicaoPadrao() { return composicaoPadrao; }
    public void definirComposicaoPadrao(List<VagaoVisita> vagoes) { composicaoPadrao.clear(); if (vagoes != null) composicaoPadrao.addAll(vagoes); }

    @PrePersist
    public void aoCriar() { LocalDateTime agora = LocalDateTime.now(); criadoEm = agora; atualizadoEm = agora; }
    @PreUpdate
    public void aoAtualizar() { atualizadoEm = LocalDateTime.now(); }
}
