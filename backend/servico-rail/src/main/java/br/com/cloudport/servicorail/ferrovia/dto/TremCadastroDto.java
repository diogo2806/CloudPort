package br.com.cloudport.servicorail.ferrovia.dto;

import br.com.cloudport.servicorail.ferrovia.modelo.TremCadastro;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class TremCadastroDto {
    private Long id;
    private Long versao;

    @NotBlank
    @Size(max = 40)
    private String identificador;

    @NotBlank
    @Size(max = 80)
    private String operadoraFerroviaria;

    @Size(max = 120)
    private String descricao;

    private boolean ativo = true;

    @Size(max = 500)
    private String observacoes;

    @Valid
    private List<VagaoVisitaRequisicaoDto> composicaoPadrao = new ArrayList<>();

    private String criadoPor;
    private String alteradoPor;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

    public static TremCadastroDto deEntidade(TremCadastro entidade) {
        TremCadastroDto dto = new TremCadastroDto();
        dto.id = entidade.getId();
        dto.versao = entidade.getVersao();
        dto.identificador = entidade.getIdentificador();
        dto.operadoraFerroviaria = entidade.getOperadoraFerroviaria();
        dto.descricao = entidade.getDescricao();
        dto.ativo = entidade.isAtivo();
        dto.observacoes = entidade.getObservacoes();
        dto.criadoPor = entidade.getCriadoPor();
        dto.alteradoPor = entidade.getAlteradoPor();
        dto.criadoEm = entidade.getCriadoEm();
        dto.atualizadoEm = entidade.getAtualizadoEm();
        dto.composicaoPadrao = entidade.getComposicaoPadrao().stream().map(vagao -> {
            VagaoVisitaRequisicaoDto item = new VagaoVisitaRequisicaoDto();
            item.setIdentificadorVagao(vagao.getIdentificadorVagao());
            item.setTipoVagao(vagao.getTipoVagao());
            item.setPosicaoNoTrem(vagao.getPosicaoNoTrem());
            return item;
        }).collect(Collectors.toList());
        return dto;
    }

    public Long getId() { return id; }
    public Long getVersao() { return versao; }
    public String getIdentificador() { return identificador; }
    public void setIdentificador(String identificador) { this.identificador = identificador; }
    public String getOperadoraFerroviaria() { return operadoraFerroviaria; }
    public void setOperadoraFerroviaria(String operadoraFerroviaria) { this.operadoraFerroviaria = operadoraFerroviaria; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
    public List<VagaoVisitaRequisicaoDto> getComposicaoPadrao() { return composicaoPadrao; }
    public void setComposicaoPadrao(List<VagaoVisitaRequisicaoDto> composicaoPadrao) { this.composicaoPadrao = composicaoPadrao; }
    public String getCriadoPor() { return criadoPor; }
    public String getAlteradoPor() { return alteradoPor; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
}
