package br.com.cloudport.serviconavio.estiva.entidade;

import br.com.cloudport.serviconavio.escala.entidade.Escala;
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
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Plano de estiva (stowage plan) de uma escala. Define as dimensões do
 * arranjo tridimensional bay/row/tier do navio para esta visita e agrupa as
 * atribuições de contêineres às células.
 */
@Entity
@Table(name = "plano_estiva")
public class PlanoEstiva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "escala_id", nullable = false, unique = true)
    private Escala escala;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusPlanoEstiva status;

    @Column(name = "baias", nullable = false)
    private int baias;

    @Column(name = "fileiras", nullable = false)
    private int fileiras;

    @Column(name = "camadas", nullable = false)
    private int camadas;

    @OneToMany(mappedBy = "plano", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("baia ASC, fileira ASC, camada ASC")
    private List<AtribuicaoEstiva> atribuicoes = new ArrayList<>();

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Escala getEscala() {
        return escala;
    }

    public void setEscala(Escala escala) {
        this.escala = escala;
    }

    public StatusPlanoEstiva getStatus() {
        return status;
    }

    public void setStatus(StatusPlanoEstiva status) {
        this.status = status;
    }

    public int getBaias() {
        return baias;
    }

    public void setBaias(int baias) {
        this.baias = baias;
    }

    public int getFileiras() {
        return fileiras;
    }

    public void setFileiras(int fileiras) {
        this.fileiras = fileiras;
    }

    public int getCamadas() {
        return camadas;
    }

    public void setCamadas(int camadas) {
        this.camadas = camadas;
    }

    public List<AtribuicaoEstiva> getAtribuicoes() {
        return atribuicoes;
    }

    public void setAtribuicoes(List<AtribuicaoEstiva> atribuicoes) {
        this.atribuicoes = atribuicoes;
    }

    public void adicionarAtribuicao(AtribuicaoEstiva atribuicao) {
        atribuicao.setPlano(this);
        this.atribuicoes.add(atribuicao);
    }

    public void removerAtribuicao(AtribuicaoEstiva atribuicao) {
        this.atribuicoes.remove(atribuicao);
        atribuicao.setPlano(null);
    }

    public int capacidadeCelulas() {
        return baias * fileiras * camadas;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(LocalDateTime atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }

    @PrePersist
    public void aoCriar() {
        LocalDateTime agora = LocalDateTime.now();
        this.criadoEm = agora;
        this.atualizadoEm = agora;
    }

    @PreUpdate
    public void aoAtualizar() {
        this.atualizadoEm = LocalDateTime.now();
    }
}
