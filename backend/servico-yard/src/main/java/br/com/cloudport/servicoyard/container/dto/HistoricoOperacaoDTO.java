package br.com.cloudport.servicoyard.container.dto;

import br.com.cloudport.servicoyard.patio.modelo.TipoMovimentoPatio;
import java.time.LocalDateTime;

public class HistoricoOperacaoDTO {
    private TipoMovimentoPatio tipoOperacao;
    private String descricao;
    private String posicaoAnterior;
    private String posicaoAtual;
    private String responsavel;
    private LocalDateTime dataRegistro;

    public HistoricoOperacaoDTO(TipoMovimentoPatio tipoOperacao, String descricao,
                                String posicaoAnterior, String posicaoAtual,
                                String responsavel, LocalDateTime dataRegistro) {
        this.tipoOperacao = tipoOperacao;
        this.descricao = descricao;
        this.posicaoAnterior = posicaoAnterior;
        this.posicaoAtual = posicaoAtual;
        this.responsavel = responsavel;
        this.dataRegistro = dataRegistro;
    }

    public TipoMovimentoPatio getTipoOperacao() { return tipoOperacao; }
    public String getDescricao() { return descricao; }
    public String getPosicaoAnterior() { return posicaoAnterior; }
    public String getPosicaoAtual() { return posicaoAtual; }
    public String getResponsavel() { return responsavel; }
    public LocalDateTime getDataRegistro() { return dataRegistro; }
}
