package br.com.cloudport.servicogate.model;

import br.com.cloudport.servicogate.model.enums.GateResourceType;
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
import javax.persistence.Table;

@Entity
@Table(name = "gate_resource_occupation")
public class GateResourceOccupation extends AbstractAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "gate_pass_id", nullable = false)
    private GatePass gatePass;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_recurso", nullable = false, length = 20)
    private GateResourceType tipoRecurso;

    @Column(name = "chave_recurso", nullable = false, length = 120)
    private String chaveRecurso;

    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;

    @Column(name = "ocupado_em", nullable = false)
    private LocalDateTime ocupadoEm;

    @Column(name = "liberado_em")
    private LocalDateTime liberadoEm;

    public Long getId() { return id; }
    public GatePass getGatePass() { return gatePass; }
    public void setGatePass(GatePass gatePass) { this.gatePass = gatePass; }
    public GateResourceType getTipoRecurso() { return tipoRecurso; }
    public void setTipoRecurso(GateResourceType tipoRecurso) { this.tipoRecurso = tipoRecurso; }
    public String getChaveRecurso() { return chaveRecurso; }
    public void setChaveRecurso(String chaveRecurso) { this.chaveRecurso = chaveRecurso; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public LocalDateTime getOcupadoEm() { return ocupadoEm; }
    public void setOcupadoEm(LocalDateTime ocupadoEm) { this.ocupadoEm = ocupadoEm; }
    public LocalDateTime getLiberadoEm() { return liberadoEm; }
    public void setLiberadoEm(LocalDateTime liberadoEm) { this.liberadoEm = liberadoEm; }
}
