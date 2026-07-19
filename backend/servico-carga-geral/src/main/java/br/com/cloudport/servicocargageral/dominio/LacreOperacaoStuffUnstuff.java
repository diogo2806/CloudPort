package br.com.cloudport.servicocargageral.dominio;

import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.StatusLacreStuffUnstuff;
import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.TipoEventoLacreStuffUnstuff;
import java.time.OffsetDateTime;
import java.util.UUID;
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
import javax.persistence.UniqueConstraint;

@Entity
@Table(name = "lacre_operacao_stuff_unstuff", uniqueConstraints = {
    @UniqueConstraint(name = "uk_lacre_operacao_command", columnNames = {"operacao_id", "command_id"})
})
public class LacreOperacaoStuffUnstuff {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operacao_id", nullable = false)
    private OperacaoStuffUnstuff operacao;

    @Column(name = "command_id", nullable = false)
    private UUID commandId;

    @Column(name = "numero_lacre", nullable = false, length = 80)
    private String numeroLacre;

    @Column(name = "numero_lacre_substituido", length = 80)
    private String numeroLacreSubstituido;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_evento", nullable = false, length = 30)
    private TipoEventoLacreStuffUnstuff tipoEvento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusLacreStuffUnstuff status;

    @Column(nullable = false, length = 120)
    private String operador;

    @Column(name = "correlation_id", length = 120)
    private String correlationId;

    @Column(length = 1000)
    private String motivo;

    @Column(name = "divergencia_aberta", nullable = false)
    private boolean divergenciaAberta;

    @Column(name = "override_autorizado", nullable = false)
    private boolean overrideAutorizado;

    @Column(name = "ocorrido_em", nullable = false)
    private OffsetDateTime ocorridoEm;

    @PrePersist
    void prePersist() {
        ocorridoEm = ocorridoEm == null ? OffsetDateTime.now() : ocorridoEm;
        numeroLacre = normalizar(numeroLacre);
        numeroLacreSubstituido = normalizar(numeroLacreSubstituido);
        operador = operador == null ? null : operador.trim();
        correlationId = correlationId == null ? null : correlationId.trim();
        motivo = motivo == null ? null : motivo.trim();
    }

    private String normalizar(String valor) {
        return valor == null ? null : valor.trim().toUpperCase();
    }

    public UUID getId() { return id; }
    public OperacaoStuffUnstuff getOperacao() { return operacao; }
    public void setOperacao(OperacaoStuffUnstuff operacao) { this.operacao = operacao; }
    public UUID getCommandId() { return commandId; }
    public void setCommandId(UUID commandId) { this.commandId = commandId; }
    public String getNumeroLacre() { return numeroLacre; }
    public void setNumeroLacre(String numeroLacre) { this.numeroLacre = numeroLacre; }
    public String getNumeroLacreSubstituido() { return numeroLacreSubstituido; }
    public void setNumeroLacreSubstituido(String numeroLacreSubstituido) { this.numeroLacreSubstituido = numeroLacreSubstituido; }
    public TipoEventoLacreStuffUnstuff getTipoEvento() { return tipoEvento; }
    public void setTipoEvento(TipoEventoLacreStuffUnstuff tipoEvento) { this.tipoEvento = tipoEvento; }
    public StatusLacreStuffUnstuff getStatus() { return status; }
    public void setStatus(StatusLacreStuffUnstuff status) { this.status = status; }
    public String getOperador() { return operador; }
    public void setOperador(String operador) { this.operador = operador; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public boolean isDivergenciaAberta() { return divergenciaAberta; }
    public void setDivergenciaAberta(boolean divergenciaAberta) { this.divergenciaAberta = divergenciaAberta; }
    public boolean isOverrideAutorizado() { return overrideAutorizado; }
    public void setOverrideAutorizado(boolean overrideAutorizado) { this.overrideAutorizado = overrideAutorizado; }
    public OffsetDateTime getOcorridoEm() { return ocorridoEm; }
}