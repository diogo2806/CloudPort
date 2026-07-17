package br.com.cloudport.servicogate.app.gestor.dto;

import java.time.LocalDateTime;

public class RetiradaDiretaNavioDTO {

    private final Long id;
    private final String codigoAutorizacao;
    private final String identificadorCarga;
    private final String tipoCarga;
    private final String visitaNavio;
    private final String clienteNome;
    private final String clienteDocumento;
    private final String status;
    private final String statusDescricao;
    private final LocalDateTime saidaEm;
    private final String operador;
    private final String observacao;
    private final LocalDateTime criadoEm;

    public RetiradaDiretaNavioDTO(Long id,
                                  String codigoAutorizacao,
                                  String identificadorCarga,
                                  String tipoCarga,
                                  String visitaNavio,
                                  String clienteNome,
                                  String clienteDocumento,
                                  String status,
                                  String statusDescricao,
                                  LocalDateTime saidaEm,
                                  String operador,
                                  String observacao,
                                  LocalDateTime criadoEm) {
        this.id = id;
        this.codigoAutorizacao = codigoAutorizacao;
        this.identificadorCarga = identificadorCarga;
        this.tipoCarga = tipoCarga;
        this.visitaNavio = visitaNavio;
        this.clienteNome = clienteNome;
        this.clienteDocumento = clienteDocumento;
        this.status = status;
        this.statusDescricao = statusDescricao;
        this.saidaEm = saidaEm;
        this.operador = operador;
        this.observacao = observacao;
        this.criadoEm = criadoEm;
    }

    public Long getId() {
        return id;
    }

    public String getCodigoAutorizacao() {
        return codigoAutorizacao;
    }

    public String getIdentificadorCarga() {
        return identificadorCarga;
    }

    public String getTipoCarga() {
        return tipoCarga;
    }

    public String getVisitaNavio() {
        return visitaNavio;
    }

    public String getClienteNome() {
        return clienteNome;
    }

    public String getClienteDocumento() {
        return clienteDocumento;
    }

    public String getStatus() {
        return status;
    }

    public String getStatusDescricao() {
        return statusDescricao;
    }

    public LocalDateTime getSaidaEm() {
        return saidaEm;
    }

    public String getOperador() {
        return operador;
    }

    public String getObservacao() {
        return observacao;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }
}
