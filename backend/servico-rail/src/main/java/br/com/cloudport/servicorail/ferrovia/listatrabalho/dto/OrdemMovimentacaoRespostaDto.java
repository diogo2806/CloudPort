package br.com.cloudport.servicorail.ferrovia.listatrabalho.dto;

import br.com.cloudport.servicorail.ferrovia.listatrabalho.modelo.OrdemMovimentacao;
import br.com.cloudport.servicorail.ferrovia.listatrabalho.modelo.StatusOrdemMovimentacao;
import br.com.cloudport.servicorail.ferrovia.listatrabalho.modelo.TipoMovimentacaoOrdem;
import java.time.LocalDateTime;
import org.springframework.web.util.HtmlUtils;

public class OrdemMovimentacaoRespostaDto {

    private final Long id;
    private final Long idVisitaTrem;
    private final String codigoConteiner;
    private final TipoMovimentacaoOrdem tipoMovimentacao;
    private final StatusOrdemMovimentacao statusMovimentacao;
    private final LocalDateTime criadoEm;
    private final LocalDateTime atualizadoEm;

    public OrdemMovimentacaoRespostaDto(Long id,
                                        Long idVisitaTrem,
                                        String codigoConteiner,
                                        TipoMovimentacaoOrdem tipoMovimentacao,
                                        StatusOrdemMovimentacao statusMovimentacao,
                                        LocalDateTime criadoEm,
                                        LocalDateTime atualizadoEm) {
        this.id = id;
        this.idVisitaTrem = idVisitaTrem;
        this.codigoConteiner = codigoConteiner;
        this.tipoMovimentacao = tipoMovimentacao;
        this.statusMovimentacao = statusMovimentacao;
        this.criadoEm = criadoEm;
        this.atualizadoEm = atualizadoEm;
    }

    public static OrdemMovimentacaoRespostaDto deEntidade(OrdemMovimentacao entidade) {
        return new OrdemMovimentacaoRespostaDto(
                entidade.getId(),
                entidade.getVisitaTrem() != null ? entidade.getVisitaTrem().getId() : null,
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

    public Long getIdVisitaTrem() {
        return idVisitaTrem;
    }

    public String getCodigoConteiner() {
        return codigoConteiner;
    }

    public TipoMovimentacaoOrdem getTipoMovimentacao() {
        return tipoMovimentacao;
    }

    public StatusOrdemMovimentacao getStatusMovimentacao() {
        return statusMovimentacao;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }
}
