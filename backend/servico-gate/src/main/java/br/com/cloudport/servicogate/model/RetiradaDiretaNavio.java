package br.com.cloudport.servicogate.model;

import br.com.cloudport.servicogate.model.enums.StatusRetiradaDiretaNavio;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "retirada_direta_navio")
public class RetiradaDiretaNavio extends AbstractAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_autorizacao", nullable = false, length = 80)
    private String codigoAutorizacao;

    @Column(name = "identificador_carga", nullable = false, length = 80)
    private String identificadorCarga;

    @Column(name = "tipo_carga", nullable = false, length = 60)
    private String tipoCarga;

    @Column(name = "visita_navio", nullable = false, length = 80)
    private String visitaNavio;

    @Column(name = "cliente_nome", nullable = false, length = 120)
    private String clienteNome;

    @Column(name = "cliente_documento", nullable = false, length = 30)
    private String clienteDocumento;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private StatusRetiradaDiretaNavio status;

    @Column(name = "saida_em", nullable = false)
    private LocalDateTime saidaEm;

    @Column(name = "operador", nullable = false, length = 80)
    private String operador;

    @Column(name = "observacao", length = 500)
    private String observacao;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodigoAutorizacao() {
        return codigoAutorizacao;
    }

    public void setCodigoAutorizacao(String codigoAutorizacao) {
        this.codigoAutorizacao = codigoAutorizacao;
    }

    public String getIdentificadorCarga() {
        return identificadorCarga;
    }

    public void setIdentificadorCarga(String identificadorCarga) {
        this.identificadorCarga = identificadorCarga;
    }

    public String getTipoCarga() {
        return tipoCarga;
    }

    public void setTipoCarga(String tipoCarga) {
        this.tipoCarga = tipoCarga;
    }

    public String getVisitaNavio() {
        return visitaNavio;
    }

    public void setVisitaNavio(String visitaNavio) {
        this.visitaNavio = visitaNavio;
    }

    public String getClienteNome() {
        return clienteNome;
    }

    public void setClienteNome(String clienteNome) {
        this.clienteNome = clienteNome;
    }

    public String getClienteDocumento() {
        return clienteDocumento;
    }

    public void setClienteDocumento(String clienteDocumento) {
        this.clienteDocumento = clienteDocumento;
    }

    public StatusRetiradaDiretaNavio getStatus() {
        return status;
    }

    public void setStatus(StatusRetiradaDiretaNavio status) {
        this.status = status;
    }

    public LocalDateTime getSaidaEm() {
        return saidaEm;
    }

    public void setSaidaEm(LocalDateTime saidaEm) {
        this.saidaEm = saidaEm;
    }

    public String getOperador() {
        return operador;
    }

    public void setOperador(String operador) {
        this.operador = operador;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }
}
