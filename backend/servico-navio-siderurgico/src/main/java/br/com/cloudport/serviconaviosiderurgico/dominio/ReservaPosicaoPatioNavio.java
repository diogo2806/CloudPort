package br.com.cloudport.serviconaviosiderurgico.dominio;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;

@Entity
@Table(name = "reserva_posicao_patio_navio")
public class ReservaPosicaoPatioNavio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "visita_navio_id", nullable = false)
    private Long visitaNavioId;

    @Column(name = "item_operacao_navio_id", nullable = false)
    private Long itemOperacaoNavioId;

    @Column(name = "posicao_patio_id", nullable = false, length = 120)
    private String posicaoPatioId;

    @Column(name = "bloco", length = 40)
    private String bloco;

    @Column(name = "linha")
    private Integer linha;

    @Column(name = "coluna")
    private Integer coluna;

    @Column(name = "camada", length = 40)
    private String camada;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_reserva", nullable = false, length = 20)
    private TipoReservaPatioNavio tipoReserva = TipoReservaPatioNavio.TENTATIVA;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusReservaPatioNavio status = StatusReservaPatioNavio.ATIVA;

    @Column(name = "motivo_cancelamento", length = 500)
    private String motivoCancelamento;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @PrePersist
    void prePersist() {
        LocalDateTime agora = LocalDateTime.now();
        criadoEm = agora;
        atualizadoEm = agora;
    }

    @PreUpdate
    void preUpdate() {
        atualizadoEm = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getVisitaNavioId() { return visitaNavioId; }
    public void setVisitaNavioId(Long visitaNavioId) { this.visitaNavioId = visitaNavioId; }
    public Long getItemOperacaoNavioId() { return itemOperacaoNavioId; }
    public void setItemOperacaoNavioId(Long itemOperacaoNavioId) { this.itemOperacaoNavioId = itemOperacaoNavioId; }
    public String getPosicaoPatioId() { return posicaoPatioId; }
    public void setPosicaoPatioId(String posicaoPatioId) { this.posicaoPatioId = posicaoPatioId; }
    public String getBloco() { return bloco; }
    public void setBloco(String bloco) { this.bloco = bloco; }
    public Integer getLinha() { return linha; }
    public void setLinha(Integer linha) { this.linha = linha; }
    public Integer getColuna() { return coluna; }
    public void setColuna(Integer coluna) { this.coluna = coluna; }
    public String getCamada() { return camada; }
    public void setCamada(String camada) { this.camada = camada; }
    public TipoReservaPatioNavio getTipoReserva() { return tipoReserva; }
    public void setTipoReserva(TipoReservaPatioNavio tipoReserva) { this.tipoReserva = tipoReserva; }
    public StatusReservaPatioNavio getStatus() { return status; }
    public void setStatus(StatusReservaPatioNavio status) { this.status = status; }
    public String getMotivoCancelamento() { return motivoCancelamento; }
    public void setMotivoCancelamento(String motivoCancelamento) { this.motivoCancelamento = motivoCancelamento; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
}
