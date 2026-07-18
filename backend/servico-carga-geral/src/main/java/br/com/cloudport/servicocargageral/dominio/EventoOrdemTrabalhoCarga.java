package br.com.cloudport.servicocargageral.dominio;

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

@Entity
@Table(name = "evento_ordem_trabalho_carga")
public class EventoOrdemTrabalhoCarga {
    @Id @GeneratedValue(strategy = GenerationType.AUTO) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "ordem_id", nullable = false) private OrdemTrabalhoCarga ordem;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40) private TipoEventoOrdemCarga tipo;
    @Column(nullable = false, length = 1000) private String descricao;
    @Column(nullable = false, length = 120) private String usuario;
    @Column(name = "ocorrido_em", nullable = false) private OffsetDateTime ocorridoEm;
    @PrePersist void prePersist() { ocorridoEm = ocorridoEm == null ? OffsetDateTime.now() : ocorridoEm; }
    public UUID getId() { return id; }
    public OrdemTrabalhoCarga getOrdem() { return ordem; }
    public void setOrdem(OrdemTrabalhoCarga ordem) { this.ordem = ordem; }
    public TipoEventoOrdemCarga getTipo() { return tipo; }
    public void setTipo(TipoEventoOrdemCarga tipo) { this.tipo = tipo; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public OffsetDateTime getOcorridoEm() { return ocorridoEm; }
}
