package br.com.cloudport.servicoyard.patio.custodia.modelo;

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
import javax.persistence.Version;

@Entity
@Table(name = "custodia_exchange_area")
public class CustodiaExchangeArea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_unidade", nullable = false, length = 40)
    private String codigoUnidade;

    @Column(name = "area", nullable = false, length = 80)
    private String area;

    @Column(name = "posicao", nullable = false, length = 80)
    private String posicao;

    @Column(name = "equipamento_entrega", nullable = false, length = 80)
    private String equipamentoEntrega;

    @Column(name = "operador_entrega", nullable = false, length = 120)
    private String operadorEntrega;

    @Column(name = "condicao_entrega", nullable = false, length = 120)
    private String condicaoEntrega;

    @Column(name = "lacres_entrega", nullable = false, length = 500)
    private String lacresEntrega;

    @Column(name = "entregue_em", nullable = false)
    private LocalDateTime entregueEm;

    @Column(name = "chave_idempotencia_entrega", nullable = false, unique = true, length = 120)
    private String chaveIdempotenciaEntrega;

    @Column(name = "equipamento_recebimento", length = 80)
    private String equipamentoRecebimento;

    @Column(name = "operador_recebimento", length = 120)
    private String operadorRecebimento;

    @Column(name = "condicao_recebimento", length = 120)
    private String condicaoRecebimento;

    @Column(name = "lacres_recebimento", length = 500)
    private String lacresRecebimento;

    @Column(name = "recebido_em")
    private LocalDateTime recebidoEm;

    @Column(name = "chave_idempotencia_recebimento", unique = true, length = 120)
    private String chaveIdempotenciaRecebimento;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusCustodiaExchangeArea status;

    @Column(name = "bloqueada", nullable = false)
    private boolean bloqueada;

    @Column(name = "motivo_divergencia", length = 1000)
    private String motivoDivergencia;

    @Version
    @Column(name = "versao", nullable = false)
    private Long versao;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @PrePersist
    public void prepararInclusao() {
        LocalDateTime agora = LocalDateTime.now();
        criadoEm = criadoEm == null ? agora : criadoEm;
        atualizadoEm = agora;
    }

    @PreUpdate
    public void prepararAtualizacao() {
        atualizadoEm = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCodigoUnidade() { return codigoUnidade; }
    public void setCodigoUnidade(String codigoUnidade) { this.codigoUnidade = codigoUnidade; }
    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }
    public String getPosicao() { return posicao; }
    public void setPosicao(String posicao) { this.posicao = posicao; }
    public String getEquipamentoEntrega() { return equipamentoEntrega; }
    public void setEquipamentoEntrega(String equipamentoEntrega) { this.equipamentoEntrega = equipamentoEntrega; }
    public String getOperadorEntrega() { return operadorEntrega; }
    public void setOperadorEntrega(String operadorEntrega) { this.operadorEntrega = operadorEntrega; }
    public String getCondicaoEntrega() { return condicaoEntrega; }
    public void setCondicaoEntrega(String condicaoEntrega) { this.condicaoEntrega = condicaoEntrega; }
    public String getLacresEntrega() { return lacresEntrega; }
    public void setLacresEntrega(String lacresEntrega) { this.lacresEntrega = lacresEntrega; }
    public LocalDateTime getEntregueEm() { return entregueEm; }
    public void setEntregueEm(LocalDateTime entregueEm) { this.entregueEm = entregueEm; }
    public String getChaveIdempotenciaEntrega() { return chaveIdempotenciaEntrega; }
    public void setChaveIdempotenciaEntrega(String chaveIdempotenciaEntrega) { this.chaveIdempotenciaEntrega = chaveIdempotenciaEntrega; }
    public String getEquipamentoRecebimento() { return equipamentoRecebimento; }
    public void setEquipamentoRecebimento(String equipamentoRecebimento) { this.equipamentoRecebimento = equipamentoRecebimento; }
    public String getOperadorRecebimento() { return operadorRecebimento; }
    public void setOperadorRecebimento(String operadorRecebimento) { this.operadorRecebimento = operadorRecebimento; }
    public String getCondicaoRecebimento() { return condicaoRecebimento; }
    public void setCondicaoRecebimento(String condicaoRecebimento) { this.condicaoRecebimento = condicaoRecebimento; }
    public String getLacresRecebimento() { return lacresRecebimento; }
    public void setLacresRecebimento(String lacresRecebimento) { this.lacresRecebimento = lacresRecebimento; }
    public LocalDateTime getRecebidoEm() { return recebidoEm; }
    public void setRecebidoEm(LocalDateTime recebidoEm) { this.recebidoEm = recebidoEm; }
    public String getChaveIdempotenciaRecebimento() { return chaveIdempotenciaRecebimento; }
    public void setChaveIdempotenciaRecebimento(String chaveIdempotenciaRecebimento) { this.chaveIdempotenciaRecebimento = chaveIdempotenciaRecebimento; }
    public StatusCustodiaExchangeArea getStatus() { return status; }
    public void setStatus(StatusCustodiaExchangeArea status) { this.status = status; }
    public boolean isBloqueada() { return bloqueada; }
    public void setBloqueada(boolean bloqueada) { this.bloqueada = bloqueada; }
    public String getMotivoDivergencia() { return motivoDivergencia; }
    public void setMotivoDivergencia(String motivoDivergencia) { this.motivoDivergencia = motivoDivergencia; }
    public Long getVersao() { return versao; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(LocalDateTime atualizadoEm) { this.atualizadoEm = atualizadoEm; }
}
