package br.com.cloudport.servicogate.model;

import br.com.cloudport.servicogate.model.enums.StatusGate;
import br.com.cloudport.servicogate.model.enums.StatusConfirmacaoBarcode;
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
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name = "gate_pass")
public class GatePass extends AbstractAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo", nullable = false, unique = true, length = 40)
    private String codigo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private StatusGate status;

    @Column(name = "token", nullable = false, length = 120)
    private String token;

    @Column(name = "data_entrada")
    private LocalDateTime dataEntrada;

    @Column(name = "data_saida")
    private LocalDateTime dataSaida;

    @Column(name = "codigo_barcode", length = 50)
    private String codigoBarcode;

    @Column(name = "data_confirmacao_barcode")
    private LocalDateTime dataConfirmacaoBarcode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_confirmacao_barcode", length = 40)
    private StatusConfirmacaoBarcode statusConfirmacaoBarcode;

    @Column(name = "motivo_rejeicao_barcode", length = 500)
    private String motivoRejeicaoBarcode;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agendamento_id", nullable = false, unique = true)
    private Agendamento agendamento;

    @OneToMany(mappedBy = "gatePass", fetch = FetchType.LAZY)
    private List<GateEvent> eventos = new ArrayList<>();

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

    public StatusGate getStatus() {
        return status;
    }

    public void setStatus(StatusGate status) {
        this.status = status;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public LocalDateTime getDataEntrada() {
        return dataEntrada;
    }

    public void setDataEntrada(LocalDateTime dataEntrada) {
        this.dataEntrada = dataEntrada;
    }

    public LocalDateTime getDataSaida() {
        return dataSaida;
    }

    public void setDataSaida(LocalDateTime dataSaida) {
        this.dataSaida = dataSaida;
    }

    public Agendamento getAgendamento() {
        return agendamento;
    }

    public void setAgendamento(Agendamento agendamento) {
        this.agendamento = agendamento;
    }

    public List<GateEvent> getEventos() {
        return eventos;
    }

    public void setEventos(List<GateEvent> eventos) {
        this.eventos = eventos;
    }

    public String getCodigoBarcode() {
        return codigoBarcode;
    }

    public void setCodigoBarcode(String codigoBarcode) {
        this.codigoBarcode = codigoBarcode;
    }

    public LocalDateTime getDataConfirmacaoBarcode() {
        return dataConfirmacaoBarcode;
    }

    public void setDataConfirmacaoBarcode(LocalDateTime dataConfirmacaoBarcode) {
        this.dataConfirmacaoBarcode = dataConfirmacaoBarcode;
    }

    public StatusConfirmacaoBarcode getStatusConfirmacaoBarcode() {
        return statusConfirmacaoBarcode;
    }

    public void setStatusConfirmacaoBarcode(StatusConfirmacaoBarcode statusConfirmacaoBarcode) {
        this.statusConfirmacaoBarcode = statusConfirmacaoBarcode;
    }

    public String getMotivoRejeicaoBarcode() {
        return motivoRejeicaoBarcode;
    }

    public void setMotivoRejeicaoBarcode(String motivoRejeicaoBarcode) {
        this.motivoRejeicaoBarcode = motivoRejeicaoBarcode;
    }
}
