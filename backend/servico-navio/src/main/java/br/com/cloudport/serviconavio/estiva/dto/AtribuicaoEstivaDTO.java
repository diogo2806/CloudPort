package br.com.cloudport.serviconavio.estiva.dto;

import br.com.cloudport.serviconavio.estiva.entidade.AtribuicaoEstiva;
import br.com.cloudport.serviconavio.estiva.entidade.TipoCargaConteiner;
import br.com.cloudport.serviconavio.estiva.entidade.TipoOperacaoEstiva;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AtribuicaoEstivaDTO {

    private final Long id;
    private final TipoOperacaoEstiva tipoOperacao;
    private final String codigoConteiner;
    private final TipoCargaConteiner tipoCarga;
    private final BigDecimal pesoToneladas;
    private final int baia;
    private final int fileira;
    private final int camada;
    private final String posicaoPatioOrigem;
    private final String posicaoPatioDestino;
    private final Integer sequenciaEmbarque;
    private final boolean embarcado;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime embarcadoEm;

    public AtribuicaoEstivaDTO(Long id,
                               TipoOperacaoEstiva tipoOperacao,
                               String codigoConteiner,
                               TipoCargaConteiner tipoCarga,
                               BigDecimal pesoToneladas,
                               int baia,
                               int fileira,
                               int camada,
                               String posicaoPatioOrigem,
                               String posicaoPatioDestino,
                               Integer sequenciaEmbarque,
                               boolean embarcado,
                               LocalDateTime embarcadoEm) {
        this.id = id;
        this.tipoOperacao = tipoOperacao;
        this.codigoConteiner = codigoConteiner;
        this.tipoCarga = tipoCarga;
        this.pesoToneladas = pesoToneladas;
        this.baia = baia;
        this.fileira = fileira;
        this.camada = camada;
        this.posicaoPatioOrigem = posicaoPatioOrigem;
        this.posicaoPatioDestino = posicaoPatioDestino;
        this.sequenciaEmbarque = sequenciaEmbarque;
        this.embarcado = embarcado;
        this.embarcadoEm = embarcadoEm;
    }

    public static AtribuicaoEstivaDTO deEntidade(AtribuicaoEstiva atribuicao) {
        return new AtribuicaoEstivaDTO(
                atribuicao.getId(),
                atribuicao.getTipoOperacao(),
                atribuicao.getCodigoConteiner(),
                atribuicao.getTipoCarga(),
                atribuicao.getPesoToneladas(),
                atribuicao.getBaia(),
                atribuicao.getFileira(),
                atribuicao.getCamada(),
                atribuicao.getPosicaoPatioOrigem(),
                atribuicao.getPosicaoPatioDestino(),
                atribuicao.getSequenciaEmbarque(),
                atribuicao.isEmbarcado(),
                atribuicao.getEmbarcadoEm()
        );
    }

    public Long getId() {
        return id;
    }

    public TipoOperacaoEstiva getTipoOperacao() {
        return tipoOperacao;
    }

    public String getCodigoConteiner() {
        return codigoConteiner;
    }

    public TipoCargaConteiner getTipoCarga() {
        return tipoCarga;
    }

    public BigDecimal getPesoToneladas() {
        return pesoToneladas;
    }

    public int getBaia() {
        return baia;
    }

    public int getFileira() {
        return fileira;
    }

    public int getCamada() {
        return camada;
    }

    public String getPosicaoPatioOrigem() {
        return posicaoPatioOrigem;
    }

    public String getPosicaoPatioDestino() {
        return posicaoPatioDestino;
    }

    public Integer getSequenciaEmbarque() {
        return sequenciaEmbarque;
    }

    public boolean isEmbarcado() {
        return embarcado;
    }

    public LocalDateTime getEmbarcadoEm() {
        return embarcadoEm;
    }
}
