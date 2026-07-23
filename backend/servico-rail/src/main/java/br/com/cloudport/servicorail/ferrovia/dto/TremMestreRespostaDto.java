package br.com.cloudport.servicorail.ferrovia.dto;

import br.com.cloudport.servicorail.ferrovia.modelo.TremMestre;
import br.com.cloudport.servicorail.ferrovia.modelo.VagaoVisita;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class TremMestreRespostaDto {

    private final Long id;
    private final Long versao;
    private final String identificador;
    private final String operadoraFerroviaria;
    private final String nomeOperacional;
    private final boolean ativo;
    private final String observacoes;
    private final String criadoPor;
    private final String alteradoPor;
    private final LocalDateTime criadoEm;
    private final LocalDateTime alteradoEm;
    private final List<VagaoResposta> composicaoPadrao;

    public TremMestreRespostaDto(TremMestre trem) {
        this.id = trem.getId();
        this.versao = trem.getVersao();
        this.identificador = trem.getIdentificador();
        this.operadoraFerroviaria = trem.getOperadoraFerroviaria();
        this.nomeOperacional = trem.getNomeOperacional();
        this.ativo = trem.isAtivo();
        this.observacoes = trem.getObservacoes();
        this.criadoPor = trem.getCriadoPor();
        this.alteradoPor = trem.getAlteradoPor();
        this.criadoEm = trem.getCriadoEm();
        this.alteradoEm = trem.getAlteradoEm();
        this.composicaoPadrao = trem.getComposicaoPadrao().stream().map(VagaoResposta::new).collect(Collectors.toList());
    }

    public Long getId() { return id; }
    public Long getVersao() { return versao; }
    public String getIdentificador() { return identificador; }
    public String getOperadoraFerroviaria() { return operadoraFerroviaria; }
    public String getNomeOperacional() { return nomeOperacional; }
    public boolean isAtivo() { return ativo; }
    public String getObservacoes() { return observacoes; }
    public String getCriadoPor() { return criadoPor; }
    public String getAlteradoPor() { return alteradoPor; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getAlteradoEm() { return alteradoEm; }
    public List<VagaoResposta> getComposicaoPadrao() { return composicaoPadrao; }

    public static class VagaoResposta {
        private final Integer posicaoNoTrem;
        private final String identificadorVagao;
        private final String tipoVagao;
        private final Integer capacidadeConteineres;

        public VagaoResposta(VagaoVisita vagao) {
            this.posicaoNoTrem = vagao.getPosicaoNoTrem();
            this.identificadorVagao = vagao.getIdentificadorVagao();
            this.tipoVagao = vagao.getTipoVagao();
            this.capacidadeConteineres = vagao.getCapacidadeConteineres();
        }

        public Integer getPosicaoNoTrem() { return posicaoNoTrem; }
        public String getIdentificadorVagao() { return identificadorVagao; }
        public String getTipoVagao() { return tipoVagao; }
        public Integer getCapacidadeConteineres() { return capacidadeConteineres; }
    }
}
