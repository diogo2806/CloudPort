package br.com.cloudport.servicorail.ferrovia.movimento.modelo;

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
@Table(name = "reserva_recurso_ferroviario")
public class ReservaRecursoFerroviario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "movimento_id", nullable = false)
    private MovimentoFerroviarioInterno movimento;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_recurso", nullable = false, length = 20)
    private TipoRecursoFerroviario tipoRecurso;

    @Column(name = "codigo_recurso", nullable = false, length = 80)
    private String codigoRecurso;

    @Column(name = "inicio_reserva", nullable = false)
    private LocalDateTime inicioReserva;

    @Column(name = "fim_reserva", nullable = false)
    private LocalDateTime fimReserva;

    @Column(nullable = false)
    private boolean ativo;

    protected ReservaRecursoFerroviario() {
    }

    public ReservaRecursoFerroviario(TipoRecursoFerroviario tipoRecurso,
                                     String codigoRecurso,
                                     LocalDateTime inicioReserva,
                                     LocalDateTime fimReserva) {
        this.tipoRecurso = tipoRecurso;
        this.codigoRecurso = codigoRecurso;
        this.inicioReserva = inicioReserva;
        this.fimReserva = fimReserva;
        this.ativo = false;
    }

    void vincular(MovimentoFerroviarioInterno movimento) {
        this.movimento = movimento;
    }

    void ativar() {
        this.ativo = true;
    }

    void desativar() {
        this.ativo = false;
    }

    public Long getId() {
        return id;
    }

    public TipoRecursoFerroviario getTipoRecurso() {
        return tipoRecurso;
    }

    public String getCodigoRecurso() {
        return codigoRecurso;
    }

    public LocalDateTime getInicioReserva() {
        return inicioReserva;
    }

    public LocalDateTime getFimReserva() {
        return fimReserva;
    }

    public boolean isAtivo() {
        return ativo;
    }
}
