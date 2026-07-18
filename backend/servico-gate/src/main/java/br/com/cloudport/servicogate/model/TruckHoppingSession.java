package br.com.cloudport.servicogate.model;

import br.com.cloudport.servicogate.model.enums.TruckHoppingStatus;
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
@Table(name = "truck_hopping_session")
public class TruckHoppingSession extends AbstractAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cpf_motorista", nullable = false, length = 14)
    private String cpfMotorista;

    @Column(name = "numero_cnh", nullable = false, length = 20)
    private String numeroCnh;

    @Column(name = "cavalo_atual", nullable = false, length = 10)
    private String cavaloAtual;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TruckHoppingStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gate_in_id")
    private GatePass gateIn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gate_out_id")
    private GatePass gateOut;

    @Column(name = "encerrada_em")
    private LocalDateTime encerradaEm;

    public Long getId() { return id; }
    public String getCpfMotorista() { return cpfMotorista; }
    public void setCpfMotorista(String cpfMotorista) { this.cpfMotorista = cpfMotorista; }
    public String getNumeroCnh() { return numeroCnh; }
    public void setNumeroCnh(String numeroCnh) { this.numeroCnh = numeroCnh; }
    public String getCavaloAtual() { return cavaloAtual; }
    public void setCavaloAtual(String cavaloAtual) { this.cavaloAtual = cavaloAtual; }
    public TruckHoppingStatus getStatus() { return status; }
    public void setStatus(TruckHoppingStatus status) { this.status = status; }
    public GatePass getGateIn() { return gateIn; }
    public void setGateIn(GatePass gateIn) { this.gateIn = gateIn; }
    public GatePass getGateOut() { return gateOut; }
    public void setGateOut(GatePass gateOut) { this.gateOut = gateOut; }
    public LocalDateTime getEncerradaEm() { return encerradaEm; }
    public void setEncerradaEm(LocalDateTime encerradaEm) { this.encerradaEm = encerradaEm; }
}
