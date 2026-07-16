package br.com.cloudport.servicoyard.patio.modelo;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;

@Entity
@Table(name = "telemetria_equipamento_patio")
public class TelemetriaEquipamentoPatio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipamento_id", nullable = false, unique = true)
    private EquipamentoPatio equipamento;

    private Double latitude;
    private Double longitude;

    @Column(name = "coordenada_x")
    private Double coordenadaX;

    @Column(name = "coordenada_y")
    private Double coordenadaY;

    private Double heading;

    @Column(name = "posicao_mais_proxima", length = 80)
    private String posicaoMaisProxima;

    @Column(name = "distancia_posicao_centimetros")
    private Integer distanciaPosicaoCentimetros;

    @Column(name = "dentro_da_posicao")
    private Boolean dentroDaPosicao;

    @Column(nullable = false, length = 80)
    private String origem;

    @Column(name = "operador_vmt", length = 120)
    private String operadorVmt;

    @Column(name = "status_vmt", length = 40)
    private String statusVmt;

    @Column(name = "work_instruction_atual_id")
    private Long workInstructionAtualId;

    @Column(nullable = false)
    private Long sequencia;

    @Column(name = "capturado_em", nullable = false)
    private LocalDateTime capturadoEm;

    @Column(name = "recebido_em", nullable = false)
    private LocalDateTime recebidoEm;

    @PrePersist
    @PreUpdate
    void atualizarRecebimento() {
        recebidoEm = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public EquipamentoPatio getEquipamento() { return equipamento; }
    public void setEquipamento(EquipamentoPatio equipamento) { this.equipamento = equipamento; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public Double getCoordenadaX() { return coordenadaX; }
    public void setCoordenadaX(Double coordenadaX) { this.coordenadaX = coordenadaX; }
    public Double getCoordenadaY() { return coordenadaY; }
    public void setCoordenadaY(Double coordenadaY) { this.coordenadaY = coordenadaY; }
    public Double getHeading() { return heading; }
    public void setHeading(Double heading) { this.heading = heading; }
    public String getPosicaoMaisProxima() { return posicaoMaisProxima; }
    public void setPosicaoMaisProxima(String posicaoMaisProxima) { this.posicaoMaisProxima = posicaoMaisProxima; }
    public Integer getDistanciaPosicaoCentimetros() { return distanciaPosicaoCentimetros; }
    public void setDistanciaPosicaoCentimetros(Integer distanciaPosicaoCentimetros) { this.distanciaPosicaoCentimetros = distanciaPosicaoCentimetros; }
    public Boolean getDentroDaPosicao() { return dentroDaPosicao; }
    public void setDentroDaPosicao(Boolean dentroDaPosicao) { this.dentroDaPosicao = dentroDaPosicao; }
    public String getOrigem() { return origem; }
    public void setOrigem(String origem) { this.origem = origem; }
    public String getOperadorVmt() { return operadorVmt; }
    public void setOperadorVmt(String operadorVmt) { this.operadorVmt = operadorVmt; }
    public String getStatusVmt() { return statusVmt; }
    public void setStatusVmt(String statusVmt) { this.statusVmt = statusVmt; }
    public Long getWorkInstructionAtualId() { return workInstructionAtualId; }
    public void setWorkInstructionAtualId(Long workInstructionAtualId) { this.workInstructionAtualId = workInstructionAtualId; }
    public Long getSequencia() { return sequencia; }
    public void setSequencia(Long sequencia) { this.sequencia = sequencia; }
    public LocalDateTime getCapturadoEm() { return capturadoEm; }
    public void setCapturadoEm(LocalDateTime capturadoEm) { this.capturadoEm = capturadoEm; }
    public LocalDateTime getRecebidoEm() { return recebidoEm; }
}
