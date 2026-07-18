package br.com.cloudport.servicocargageral.dominio;

import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.TipoEventoStuffUnstuff;
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
@Table(name = "evento_operacao_stuff_unstuff")
public class EventoOperacaoStuffUnstuff {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operacao_id", nullable = false)
    private OperacaoStuffUnstuff operacao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TipoEventoStuffUnstuff tipo;

    @Column(nullable = false, length = 120)
    private String usuario;

    @Column(name = "correlation_id", length = 120)
    private String correlationId;

    @Column(length = 1000)
    private String descricao;

    @Column(name = "ocorrido_em", nullable = false)
    private OffsetDateTime ocorridoEm;

    @PrePersist
    void prePersist() {
        ocorridoEm = ocorridoEm == null ? OffsetDateTime.now() : ocorridoEm;
    }

    public UUID getId() { return id; }
    public void setOperacao(OperacaoStuffUnstuff operacao) { this.operacao = operacao; }
    public TipoEventoStuffUnstuff getTipo() { return tipo; }
    public void setTipo(TipoEventoStuffUnstuff tipo) { this.tipo = tipo; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public OffsetDateTime getOcorridoEm() { return ocorridoEm; }
}
