package br.com.cloudport.serviconavio.estiva.entidade;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.time.LocalDateTime;

/**
 * Terno (crane split / work queue): turma/guindaste que opera um intervalo
 * contíguo de baias do navio, com uma ordem de execução.
 */
@Entity
@Table(name = "terno", uniqueConstraints = {
        @UniqueConstraint(name = "uk_terno_identificador", columnNames = {"plano_id", "identificador"})
})
public class Terno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plano_id", nullable = false)
    private PlanoEstiva plano;

    @Column(name = "identificador", nullable = false, length = 40)
    private String identificador;

    @Column(name = "sequencia", nullable = false)
    private int sequencia;

    @Column(name = "baia_inicial", nullable = false)
    private int baiaInicial;

    @Column(name = "baia_final", nullable = false)
    private int baiaFinal;

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

    public PlanoEstiva getPlano() {
        return plano;
    }

    public void setPlano(PlanoEstiva plano) {
        this.plano = plano;
    }

    public String getIdentificador() {
        return identificador;
    }

    public void setIdentificador(String identificador) {
        this.identificador = identificador;
    }

    public int getSequencia() {
        return sequencia;
    }

    public void setSequencia(int sequencia) {
        this.sequencia = sequencia;
    }

    public int getBaiaInicial() {
        return baiaInicial;
    }

    public void setBaiaInicial(int baiaInicial) {
        this.baiaInicial = baiaInicial;
    }

    public int getBaiaFinal() {
        return baiaFinal;
    }

    public void setBaiaFinal(int baiaFinal) {
        this.baiaFinal = baiaFinal;
    }

    public boolean cobreBaia(int baia) {
        return baia >= baiaInicial && baia <= baiaFinal;
    }

    public boolean sobrepoe(int inicio, int fim) {
        return inicio <= baiaFinal && fim >= baiaInicial;
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
