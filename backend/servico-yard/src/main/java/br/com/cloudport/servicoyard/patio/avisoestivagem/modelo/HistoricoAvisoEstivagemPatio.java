package br.com.cloudport.servicoyard.patio.avisoestivagem.modelo;

import br.com.cloudport.servicoyard.patio.avisoestivagem.modelo.AvisoEstivagemPatioEnums.StatusAvisoEstivagemPatio;
import br.com.cloudport.servicoyard.patio.avisoestivagem.modelo.AvisoEstivagemPatioEnums.TipoEventoHistoricoAvisoEstivagemPatio;
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
    @Column(name = "tipo_evento", nullable = false, length = 40)
    private TipoEventoHistoricoAvisoEstivagemPatio tipoEvento;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_anterior", length = 40)
    private StatusAvisoEstivagemPatio statusAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_novo", nullable = false, length = 40)
    private StatusAvisoEstivagemPatio statusNovo;

    @Column(name = "ator", nullable = false, length = 120)
    private String ator;

    @Column(name = "detalhes", length = 2000)
    private String detalhes;

    @Column(name = "evidencia", length = 2000)
    private String evidencia;

    @Column(name = "resultado", length = 2000)
    private String resultado;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    public void prepararInclusao() {
        criadoEm = criadoEm == null ? LocalDateTime.now() : criadoEm;
    }

    public Long getId() { return id; }
    public AvisoEstivagemPatio getAviso() { return aviso; }
    public void setAviso(AvisoEstivagemPatio aviso) { this.aviso = aviso; }
    public TipoEventoHistoricoAvisoEstivagemPatio getTipoEvento() { return tipoEvento; }
    public void setTipoEvento(TipoEventoHistoricoAvisoEstivagemPatio tipoEvento) { this.tipoEvento = tipoEvento; }
    public StatusAvisoEstivagemPatio getStatusAnterior() { return statusAnterior; }
    public void setStatusAnterior(StatusAvisoEstivagemPatio statusAnterior) { this.statusAnterior = statusAnterior; }
    public StatusAvisoEstivagemPatio getStatusNovo() { return statusNovo; }
    public void setStatusNovo(StatusAvisoEstivagemPatio statusNovo) { this.statusNovo = statusNovo; }
    public String getAtor() { return ator; }
    public void setAtor(String ator) { this.ator = ator; }
    public String getDetalhes() { return detalhes; }
    public void setDetalhes(String detalhes) { this.detalhes = detalhes; }
    public String getEvidencia() { return evidencia; }
    public void setEvidencia(String evidencia) { this.evidencia = evidencia; }
    public String getResultado() { return resultado; }
    public void setResultado(String resultado) { this.resultado = resultado; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
}
