package br.com.cloudport.servicoyard.container.dto;

import br.com.cloudport.servicoyard.container.entidade.TipoOperacaoConteiner;

import java.time.OffsetDateTime;

public class HistoricoOperacaoDTO {
    private TipoOperacaoConteiner tipoOperacao;
    private String descricao;
    private String posicaoAnterior;
    private String posicaoAtual;
    private String responsavel;
    private OffsetDateTime dataRegistro;

    public HistoricoOperacaoDTO(TipoOperacaoConteiner tipoOperacao, String descricao,
                                String posicaoAnterior, String posicaoAtual,
                                String responsavel, OffsetDateTime dataRegistro) {
        this.tipoOperacao = tipoOperacao;
        this.descricao = descricao;
        this.posicaoAnterior = posicaoAnterior;
        this.posicaoAtual = posicaoAtual;
        this.responsavel = responsavel;
        this.dataRegistro = dataRegistro;
    }

    public TipoOperacaoConteiner getTipoOperacao() {
        return tipoOperacao;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getPosicaoAnterior() {
        return posicaoAnterior;
    }

    public String getPosicaoAtual() {
        return posicaoAtual;
    }

    public String getResponsavel() {
        return responsavel;
    }

    public OffsetDateTime getDataRegistro() {
        return dataRegistro;
    }
}
