package br.com.cloudport.servicoyard.inventario.modelo;

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
import javax.persistence.Table;

@Entity
@Table(name = "vinculo_equipamento")
public class VinculoEquipamento {

    public enum PapelEquipamento {
        PRIMARIO,
        TRANSPORTE,
        PAYLOAD,
        ACESSORIO,
        ACESSORIO_NO_CHASSI
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unidade_principal_id", nullable = false)
    private UnidadeInventario unidadePrincipal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unidade_relacionada_id", nullable = false)
    private UnidadeInventario unidadeRelacionada;

    @Enumerated(EnumType.STRING)
    @Column(name = "papel", nullable = false, length = 30)
    private PapelEquipamento papel;

    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;

    @Column(name = "montado_em", nullable = false)
    private LocalDateTime montadoEm;

    @Column(name = "desmontado_em")
    private LocalDateTime desmontadoEm;

    @Column(name = "responsavel_montagem", length = 120)
    private String responsavelMontagem;

    @Column(name = "responsavel_desmontagem", length = 120)
    private String responsavelDesmontagem;

    @Column(name = "observacoes", length = 500)
    private String observacoes;

    @PrePersist
    public void prePersist() {
        if (montadoEm == null) {
            montadoEm = LocalDateTime.now();
        }
    }

    public void desmontar(String responsavel, LocalDateTime data) {
        ativo = false;
        desmontadoEm = data;
        responsavelDesmontagem = responsavel;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UnidadeInventario getUnidadePrincipal() {
        return unidadePrincipal;
    }

    public void setUnidadePrincipal(UnidadeInventario unidadePrincipal) {
        this.unidadePrincipal = unidadePrincipal;
    }

    public UnidadeInventario getUnidadeRelacionada() {
        return unidadeRelacionada;
    }

    public void setUnidadeRelacionada(UnidadeInventario unidadeRelacionada) {
        this.unidadeRelacionada = unidadeRelacionada;
    }

    public PapelEquipamento getPapel() {
        return papel;
    }

    public void setPapel(PapelEquipamento papel) {
        this.papel = papel;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public LocalDateTime getMontadoEm() {
        return montadoEm;
    }

    public void setMontadoEm(LocalDateTime montadoEm) {
        this.montadoEm = montadoEm;
    }

    public LocalDateTime getDesmontadoEm() {
        return desmontadoEm;
    }

    public String getResponsavelMontagem() {
        return responsavelMontagem;
    }

    public void setResponsavelMontagem(String responsavelMontagem) {
        this.responsavelMontagem = responsavelMontagem;
    }

    public String getResponsavelDesmontagem() {
        return responsavelDesmontagem;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }
}
