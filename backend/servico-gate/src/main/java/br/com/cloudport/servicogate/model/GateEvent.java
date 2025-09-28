package br.com.cloudport.servicogate.model;

import br.com.cloudport.servicogate.model.enums.MotivoExcecao;
import br.com.cloudport.servicogate.model.enums.StatusGate;
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
@Table(name = "gate_event")
public class GateEvent extends AbstractAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private StatusGate status;

    @Enumerated(EnumType.STRING)
    @Column(name = "motivo_excecao", length = 40)
    private MotivoExcecao motivoExcecao;

    @Column(name = "observacao", length = 500)
    private String observacao;

    @Column(name = "usuario_responsavel", length = 80)
    private String usuarioResponsavel;

    @Column(name = "registrado_em", nullable = false)
    private LocalDateTime registradoEm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gate_pass_id", nullable = false)
    private GatePass gatePass;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public StatusGate getStatus() {
        return status;
    }

    public void setStatus(StatusGate status) {
        this.status = status;
    }

    public MotivoExcecao getMotivoExcecao() {
        return motivoExcecao;
    }

    public void setMotivoExcecao(MotivoExcecao motivoExcecao) {
        this.motivoExcecao = motivoExcecao;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    public String getUsuarioResponsavel() {
        return usuarioResponsavel;
    }

    public void setUsuarioResponsavel(String usuarioResponsavel) {
        this.usuarioResponsavel = usuarioResponsavel;
    }

    public LocalDateTime getRegistradoEm() {
        return registradoEm;
    }

    public void setRegistradoEm(LocalDateTime registradoEm) {
        this.registradoEm = registradoEm;
    }

    public GatePass getGatePass() {
        return gatePass;
    }

    public void setGatePass(GatePass gatePass) {
        this.gatePass = gatePass;
    }
}
