package br.com.cloudport.servicoyard.patio.custodia.dto;

import br.com.cloudport.servicoyard.patio.custodia.modelo.CustodiaExchangeArea;
import br.com.cloudport.servicoyard.patio.custodia.modelo.StatusCustodiaExchangeArea;
import java.time.LocalDateTime;

public class CustodiaExchangeAreaRespostaDto {

    private Long id;
    private String codigoUnidade;
    private String area;
    private String posicao;
    private String equipamentoEntrega;
    private String operadorEntrega;
    private String condicaoEntrega;
    private String lacresEntrega;
    private LocalDateTime entregueEm;
    private String equipamentoRecebimento;
    private String operadorRecebimento;
    private String condicaoRecebimento;
    private String lacresRecebimento;
    private LocalDateTime recebidoEm;
    private StatusCustodiaExchangeArea status;
    private boolean bloqueada;
    private String motivoDivergencia;
    private Long versao;
    private LocalDateTime atualizadoEm;

    public static CustodiaExchangeAreaRespostaDto deEntidade(CustodiaExchangeArea entidade) {
        CustodiaExchangeAreaRespostaDto dto = new CustodiaExchangeAreaRespostaDto();
        dto.id = entidade.getId();
        dto.codigoUnidade = entidade.getCodigoUnidade();
        dto.area = entidade.getArea();
        dto.posicao = entidade.getPosicao();
        dto.equipamentoEntrega = entidade.getEquipamentoEntrega();
        dto.operadorEntrega = entidade.getOperadorEntrega();
        dto.condicaoEntrega = entidade.getCondicaoEntrega();
        dto.lacresEntrega = entidade.getLacresEntrega();
        dto.entregueEm = entidade.getEntregueEm();
        dto.equipamentoRecebimento = entidade.getEquipamentoRecebimento();
        dto.operadorRecebimento = entidade.getOperadorRecebimento();
        dto.condicaoRecebimento = entidade.getCondicaoRecebimento();
        dto.lacresRecebimento = entidade.getLacresRecebimento();
        dto.recebidoEm = entidade.getRecebidoEm();
        dto.status = entidade.getStatus();
        dto.bloqueada = entidade.isBloqueada();
        dto.motivoDivergencia = entidade.getMotivoDivergencia();
        dto.versao = entidade.getVersao();
        dto.atualizadoEm = entidade.getAtualizadoEm();
        return dto;
    }

    public Long getId() { return id; }
    public String getCodigoUnidade() { return codigoUnidade; }
    public String getArea() { return area; }
    public String getPosicao() { return posicao; }
    public String getEquipamentoEntrega() { return equipamentoEntrega; }
    public String getOperadorEntrega() { return operadorEntrega; }
    public String getCondicaoEntrega() { return condicaoEntrega; }
    public String getLacresEntrega() { return lacresEntrega; }
    public LocalDateTime getEntregueEm() { return entregueEm; }
    public String getEquipamentoRecebimento() { return equipamentoRecebimento; }
    public String getOperadorRecebimento() { return operadorRecebimento; }
    public String getCondicaoRecebimento() { return condicaoRecebimento; }
    public String getLacresRecebimento() { return lacresRecebimento; }
    public LocalDateTime getRecebidoEm() { return recebidoEm; }
    public StatusCustodiaExchangeArea getStatus() { return status; }
    public boolean isBloqueada() { return bloqueada; }
    public String getMotivoDivergencia() { return motivoDivergencia; }
    public Long getVersao() { return versao; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
}
