package br.com.cloudport.servicoyard.operacao2d;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name = "comando_operacao_grafica_2d", uniqueConstraints = {
        @UniqueConstraint(name = "uk_comando_operacao_grafica_2d_command_id", columnNames = "command_id")
})
public class ComandoOperacaoGrafica2D {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "command_id", nullable = false, length = 120)
    private String commandId;

    @Column(name = "tipo", nullable = false, length = 80)
    private String tipo;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "motivo", length = 1000)
    private String motivo;

    @Column(name = "payload_json", nullable = false, columnDefinition = "text")
    private String payloadJson;

    @Column(name = "solicitado_por", nullable = false, length = 120)
    private String solicitadoPor;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    public void prepararInclusao() {
        status = status == null ? "CONFIRMADO" : status;
        criadoEm = criadoEm == null ? LocalDateTime.now() : criadoEm;
    }

    public Long getId() { return id; }
    public String getCommandId() { return commandId; }
    public void setCommandId(String commandId) { this.commandId = commandId; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }
    public String getSolicitadoPor() { return solicitadoPor; }
    public void setSolicitadoPor(String solicitadoPor) { this.solicitadoPor = solicitadoPor; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
}
