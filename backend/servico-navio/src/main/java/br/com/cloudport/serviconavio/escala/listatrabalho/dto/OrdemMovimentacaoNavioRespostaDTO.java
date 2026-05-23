package br.com.cloudport.serviconavio.escala.listatrabalho.dto;

import br.com.cloudport.serviconavio.escala.listatrabalho.modelo.OrdemMovimentacaoNavio;
import br.com.cloudport.serviconavio.escala.listatrabalho.modelo.StatusOrdemMovimentacaoNavio;
import br.com.cloudport.serviconavio.escala.listatrabalho.modelo.TipoMovimentacaoOrdemNavio;
import java.time.LocalDateTime;
import org.springframework.web.util.HtmlUtils;

public class OrdemMovimentacaoNavioRespostaDTO {

    private final Long id;
    private final Long idEscala;
    private final String codigoConteiner;
    private final TipoMovimentacaoOrdemNavio tipoMovimentacao;
    private final StatusOrdemMovimentacaoNavio statusMovimentacao;
    private final LocalDateTime criadoEm;
    private final LocalDateTime atualizadoEm;

    public OrdemMovimentacaoNavioRespostaDTO(Long id,
                                             Long idEscala,
                                             String codigoConteiner,
                                             TipoMovimentacaoOrdemNavio tipoMovimentacao,
                                             StatusOrdemMovimentacaoNavio statusMovimentacao,
                                             LocalDateTime criadoEm,
                                             LocalDateTime atualizadoEm) {
        this.id = id;
        this.idEscala = idEscala;
        this.codigoConteiner = codigoConteiner;
        this.tipoMovimentacao = tipoMovimentacao;
        this.statusMovimentacao = statusMovimentacao;
        this.criadoEm = criadoEm;
        this.atualizadoEm = atualizadoEm;
    }

    public static OrdemMovimentacaoNavioRespostaDTO deEntidade(OrdemMovimentacaoNavio entidade) {
        return new OrdemMovimentacaoNavioRespostaDTO(
                entidade.getId(),
                entidade.getEscala() != null ? entidade.getEscala().getId() : null,
                HtmlUtils.htmlEscape(entidade.getCodigoConteiner()),
                entidade.getTipoMovimentacao(),
                entidade.getStatusMovimentacao(),
                entidade.getCriadoEm(),
                entidade.getAtualizadoEm()
        );
    }

    public Long getId() {
        return id;
    }

    public Long getIdEscala() {
        return idEscala;
    }

    public String getCodigoConteiner() {
        return codigoConteiner;
    }

    public TipoMovimentacaoOrdemNavio getTipoMovimentacao() {
        return tipoMovimentacao;
    }

    public StatusOrdemMovimentacaoNavio getStatusMovimentacao() {
        return statusMovimentacao;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }
}
