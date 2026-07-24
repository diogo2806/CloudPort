package br.com.cloudport.servicoyard.patio.modelo;

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
import javax.persistence.PrePersist;
import javax.persistence.Table;

@Entity
@Table(name = "historico_aviso_estivagem_patio")
public class HistoricoAvisoEstivagemPatio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aviso_id", nullable = false)
    private AvisoEstivagemPatio aviso;

    @Enumerated(EnumType.STRING)
    @Column(name = "evento", nullable = false, length = 40)
    private TipoEventoAvisoEstivagemPatio evento;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", length = 40)
    private EstadoAvisoEstivagemPatio estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_novo", nullable = false, length = 40)
    private EstadoAvisoEstivagemPatio estadoNovo;

    @Column(name = "ator", nullable = false, length = 120)
    private String ator;

    @Column(name = "detalhes", length = 2000)
    private String detalhes;

    @Column(name = "evidencia", length = 2000)
    private String evidencia;

    @Column(name = "resultado", length = 2000)
    private String resultado;

    @Column(name = "ocorrido_em", nullable = false)
    private LocalDateTime ocorridoEm;

    @PrePersist
    public void criarAuditoria() {
        if (ocorridoEm == null) {
            ocorridoEm = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public AvisoEstivagemPatio getAviso() { return aviso; }
    public void setAviso(AvisoEstivagemPatio aviso) { this.aviso = aviso; }
    public TipoEventoAvisoEstivagemPatio getEvento() { return evento; }
    public void setEvento(TipoEventoAvisoEstivagemPatio evento) { this.evento = evento; }
    public EstadoAvisoEstivagemPatio getEstadoAnterior() { return estadoAnterior; }
    public void setEstadoAnterior(EstadoAvisoEstivagemPatio estadoAnterior) { this.estadoAnterior = estadoAnterior; }
    public EstadoAvisoEstivagemPatio getEstadoNovo() { return estadoNovo; }
    public void setEstadoNovo(EstadoAvisoEstivagemPatio estadoNovo) { this.estadoNovo = estadoNovo; }
    public String getAtor() { return ator; }
    public void setAtor(String ator) { this.ator = ator; }
    public String getDetalhes() { return detalhes; }
    public void setDetalhes(String detalhes) { this.detalhes = detalhes; }
    public String getEvidencia() { return evidencia; }
    public void setEvidencia(String evidencia) { this.evidencia = evidencia; }
    public String getResultado() { return resultado; }
    public void setResultado(String resultado) { this.resultado = resultado; }
    public LocalDateTime getOcorridoEm() { return ocorridoEm; }
}
