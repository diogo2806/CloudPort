package br.com.cloudport.servicoyard.patio.custodia.dto;

import br.com.cloudport.servicoyard.comum.validacao.ValidacaoEntradaUtil;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class CustodiaExchangeAreaComandoDto {

    @NotBlank
    @Size(max = 40)
    private String codigoUnidade;

    @NotBlank
    @Size(max = 80)
    private String area;

    @NotBlank
    @Size(max = 80)
    private String posicao;

    @NotBlank
    @Size(max = 80)
    private String equipamento;

    @NotBlank
    @Size(max = 120)
    private String operador;

    @NotBlank
    @Size(max = 120)
    private String condicao;

    @NotBlank
    @Size(max = 500)
    private String lacres;

    @NotBlank
    @Size(max = 120)
    private String chaveIdempotencia;

    @Size(max = 120)
    private String correlationId;

    public String getCodigoUnidade() { return codigoUnidade; }
    public void setCodigoUnidade(String codigoUnidade) { this.codigoUnidade = limpar(codigoUnidade); }
    public String getArea() { return area; }
    public void setArea(String area) { this.area = limpar(area); }
    public String getPosicao() { return posicao; }
    public void setPosicao(String posicao) { this.posicao = limpar(posicao); }
    public String getEquipamento() { return equipamento; }
    public void setEquipamento(String equipamento) { this.equipamento = limpar(equipamento); }
    public String getOperador() { return operador; }
    public void setOperador(String operador) { this.operador = limpar(operador); }
    public String getCondicao() { return condicao; }
    public void setCondicao(String condicao) { this.condicao = limpar(condicao); }
    public String getLacres() { return lacres; }
    public void setLacres(String lacres) { this.lacres = limpar(lacres); }
    public String getChaveIdempotencia() { return chaveIdempotencia; }
    public void setChaveIdempotencia(String chaveIdempotencia) { this.chaveIdempotencia = limpar(chaveIdempotencia); }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = limpar(correlationId); }

    private String limpar(String valor) {
        return ValidacaoEntradaUtil.limparTexto(valor);
    }
}
