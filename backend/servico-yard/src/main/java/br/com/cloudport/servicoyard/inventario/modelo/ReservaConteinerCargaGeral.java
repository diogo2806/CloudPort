package br.com.cloudport.servicoyard.inventario.modelo;

import br.com.cloudport.servicoyard.inventario.dto.ReservaConteinerCargaGeralDTOs.ResultadoReserva;
import java.time.LocalDateTime;
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
@Table(
        name = "reserva_conteiner_carga_geral",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_reserva_conteiner_carga_geral_operacao",
                columnNames = "operacao_id"))
public class ReservaConteinerCargaGeral {

    public enum StatusReserva {
        ATIVA,
        CONCLUIDA,
        CANCELADA
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unidade_id", nullable = false)
    private UnidadeInventario unidade;

    @Column(name = "operacao_id", nullable = false)
    private UUID operacaoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusReserva status = StatusReserva.ATIVA;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", nullable = false, length = 30)
    private UnidadeInventario.EstadoUnidade estadoAnterior;

    @Column(name = "usuario_reserva", nullable = false, length = 120)
    private String usuarioReserva;

    @Column(name = "reservado_em", nullable = false)
    private LocalDateTime reservadoEm;

    @Column(name = "usuario_liberacao", length = 120)
    private String usuarioLiberacao;

    @Column(name = "motivo_liberacao", length = 1000)
    private String motivoLiberacao;

    @Column(name = "liberado_em")
    private LocalDateTime liberadoEm;

    @PrePersist
    void prePersist() {
        reservadoEm = LocalDateTime.now();
    }

    public void liberar(String usuario, String motivo, ResultadoReserva resultado) {
        if (status != StatusReserva.ATIVA) {
            return;
        }
        status = resultado == ResultadoReserva.CANCELADA
                ? StatusReserva.CANCELADA
                : StatusReserva.CONCLUIDA;
        usuarioLiberacao = usuario == null ? null : usuario.trim();
        motivoLiberacao = motivo;
        liberadoEm = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public UnidadeInventario getUnidade() {
        return unidade;
    }

    public void setUnidade(UnidadeInventario unidade) {
        this.unidade = unidade;
    }

    public UUID getOperacaoId() {
        return operacaoId;
    }

    public void setOperacaoId(UUID operacaoId) {
        this.operacaoId = operacaoId;
    }

    public StatusReserva getStatus() {
        return status;
    }

    public UnidadeInventario.EstadoUnidade getEstadoAnterior() {
        return estadoAnterior;
    }

    public void setEstadoAnterior(UnidadeInventario.EstadoUnidade estadoAnterior) {
        this.estadoAnterior = estadoAnterior;
    }

    public String getUsuarioReserva() {
        return usuarioReserva;
    }

    public void setUsuarioReserva(String usuarioReserva) {
        this.usuarioReserva = usuarioReserva == null ? null : usuarioReserva.trim();
    }

    public LocalDateTime getReservadoEm() {
        return reservadoEm;
    }

    public String getUsuarioLiberacao() {
        return usuarioLiberacao;
    }

    public String getMotivoLiberacao() {
        return motivoLiberacao;
    }

    public LocalDateTime getLiberadoEm() {
        return liberadoEm;
    }
}
