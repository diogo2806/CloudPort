package br.com.cloudport.servicogate.model;

import br.com.cloudport.servicogate.model.enums.StatusAgendamento;
import br.com.cloudport.servicogate.model.enums.TipoOperacao;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name = "agendamento")
public class Agendamento extends AbstractAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo", nullable = false, unique = true, length = 40)
    private String codigo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_operacao", nullable = false, length = 40)
    private TipoOperacao tipoOperacao;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private StatusAgendamento status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transportadora_id", nullable = false)
    private Transportadora transportadora;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "motorista_id", nullable = false)
    private Motorista motorista;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "veiculo_id", nullable = false)
    private Veiculo veiculo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "janela_atendimento_id", nullable = false)
    private JanelaAtendimento janelaAtendimento;

    @Column(name = "horario_previsto_chegada")
    private LocalDateTime horarioPrevistoChegada;

    @Column(name = "horario_previsto_saida")
    private LocalDateTime horarioPrevistoSaida;

    @Column(name = "horario_real_chegada")
    private LocalDateTime horarioRealChegada;

    @Column(name = "horario_real_saida")
    private LocalDateTime horarioRealSaida;

    @Column(name = "observacoes", length = 500)
    private String observacoes;

    @OneToMany(mappedBy = "agendamento", fetch = FetchType.LAZY)
    private List<DocumentoAgendamento> documentos = new ArrayList<>();

    @OneToOne(mappedBy = "agendamento", fetch = FetchType.LAZY)
    private GatePass gatePass;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public TipoOperacao getTipoOperacao() {
        return tipoOperacao;
    }

    public void setTipoOperacao(TipoOperacao tipoOperacao) {
        this.tipoOperacao = tipoOperacao;
    }

    public StatusAgendamento getStatus() {
        return status;
    }

    public void setStatus(StatusAgendamento status) {
        this.status = status;
    }

    public Transportadora getTransportadora() {
        return transportadora;
    }

    public void setTransportadora(Transportadora transportadora) {
        this.transportadora = transportadora;
    }

    public Motorista getMotorista() {
        return motorista;
    }

    public void setMotorista(Motorista motorista) {
        this.motorista = motorista;
    }

    public Veiculo getVeiculo() {
        return veiculo;
    }

    public void setVeiculo(Veiculo veiculo) {
        this.veiculo = veiculo;
    }

    public JanelaAtendimento getJanelaAtendimento() {
        return janelaAtendimento;
    }

    public void setJanelaAtendimento(JanelaAtendimento janelaAtendimento) {
        this.janelaAtendimento = janelaAtendimento;
    }

    public LocalDateTime getHorarioPrevistoChegada() {
        return horarioPrevistoChegada;
    }

    public void setHorarioPrevistoChegada(LocalDateTime horarioPrevistoChegada) {
        this.horarioPrevistoChegada = horarioPrevistoChegada;
    }

    public LocalDateTime getHorarioPrevistoSaida() {
        return horarioPrevistoSaida;
    }

    public void setHorarioPrevistoSaida(LocalDateTime horarioPrevistoSaida) {
        this.horarioPrevistoSaida = horarioPrevistoSaida;
    }

    public LocalDateTime getHorarioRealChegada() {
        return horarioRealChegada;
    }

    public void setHorarioRealChegada(LocalDateTime horarioRealChegada) {
        this.horarioRealChegada = horarioRealChegada;
    }

    public LocalDateTime getHorarioRealSaida() {
        return horarioRealSaida;
    }

    public void setHorarioRealSaida(LocalDateTime horarioRealSaida) {
        this.horarioRealSaida = horarioRealSaida;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public List<DocumentoAgendamento> getDocumentos() {
        return documentos;
    }

    public void setDocumentos(List<DocumentoAgendamento> documentos) {
        this.documentos = documentos;
    }

    public GatePass getGatePass() {
        return gatePass;
    }

    public void setGatePass(GatePass gatePass) {
        this.gatePass = gatePass;
    }
}
