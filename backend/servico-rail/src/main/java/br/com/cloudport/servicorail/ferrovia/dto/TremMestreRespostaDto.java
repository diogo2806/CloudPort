package br.com.cloudport.servicorail.ferrovia.dto;

import br.com.cloudport.servicorail.ferrovia.modelo.TremMestre;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class TremMestreRespostaDto {
    private final Long id;
    private final String identificador;
    private final String operadoraFerroviaria;
    private final String descricao;
    private final boolean ativo;
    private final String observacoes;
    private final LocalDateTime criadoEm;
    private final LocalDateTime atualizadoEm;
    private final String criadoPor;
    private final String atualizadoPor;
    private final List<VagaoVisitaRespostaDto> composicaoPadrao;

    public TremMestreRespostaDto(TremMestre trem) {
        id = trem.getId(); identificador = trem.getIdentificador(); operadoraFerroviaria = trem.getOperadoraFerroviaria();
        descricao = trem.getDescricao(); ativo = trem.isAtivo(); observacoes = trem.getObservacoes(); criadoEm = trem.getCriadoEm();
        atualizadoEm = trem.getAtualizadoEm(); criadoPor = trem.getCriadoPor(); atualizadoPor = trem.getAtualizadoPor();
        composicaoPadrao = trem.getComposicaoPadrao().stream().map(VagaoVisitaRespostaDto::deEmbeddable).collect(Collectors.toList());
    }
    public Long getId() { return id; }
    public String getIdentificador() { return identificador; }
    public String getOperadoraFerroviaria() { return operadoraFerroviaria; }
    public String getDescricao() { return descricao; }
    public boolean isAtivo() { return ativo; }
    public String getObservacoes() { return observacoes; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public String getCriadoPor() { return criadoPor; }
    public String getAtualizadoPor() { return atualizadoPor; }
    public List<VagaoVisitaRespostaDto> getComposicaoPadrao() { return composicaoPadrao; }
}
