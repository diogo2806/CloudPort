package br.com.cloudport.servicogate.model;

import br.com.cloudport.servicogate.model.enums.NivelEvento;
import br.com.cloudport.servicogate.model.enums.TipoOcorrenciaOperador;
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
@Table(name = "gate_ocorrencia")
public class GateOcorrencia extends AbstractAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 60)
    private TipoOcorrenciaOperador tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel", nullable = false, length = 40)
    private NivelEvento nivel;

    @Column(name = "descricao", nullable = false, length = 500)
    private String descricao;

    @Column(name = "registrado_em", nullable = false)
    private LocalDateTime registradoEm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "veiculo_id")
    private Veiculo veiculo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transportadora_id")
    private Transportadora transportadora;

    @Column(name = "usuario_responsavel", length = 80)
    private String usuarioResponsavel;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TipoOcorrenciaOperador getTipo() {
        return tipo;
    }

    public void setTipo(TipoOcorrenciaOperador tipo) {
        this.tipo = tipo;
    }

    public NivelEvento getNivel() {
        return nivel;
    }

    public void setNivel(NivelEvento nivel) {
        this.nivel = nivel;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public LocalDateTime getRegistradoEm() {
        return registradoEm;
    }

    public void setRegistradoEm(LocalDateTime registradoEm) {
        this.registradoEm = registradoEm;
    }

    public Veiculo getVeiculo() {
        return veiculo;
    }

    public void setVeiculo(Veiculo veiculo) {
        this.veiculo = veiculo;
    }

    public Transportadora getTransportadora() {
        return transportadora;
    }

    public void setTransportadora(Transportadora transportadora) {
        this.transportadora = transportadora;
    }

    public String getUsuarioResponsavel() {
        return usuarioResponsavel;
    }

    public void setUsuarioResponsavel(String usuarioResponsavel) {
        this.usuarioResponsavel = usuarioResponsavel;
    }
}
