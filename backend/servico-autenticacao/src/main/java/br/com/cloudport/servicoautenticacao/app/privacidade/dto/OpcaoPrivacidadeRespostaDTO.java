package br.com.cloudport.servicoautenticacao.app.privacidade.dto;

import br.com.cloudport.servicoautenticacao.app.privacidade.ConfiguracaoPrivacidade;
import java.util.UUID;

public class OpcaoPrivacidadeRespostaDTO {

    private UUID id;
    private String descricao;
    private boolean ativo;

    public OpcaoPrivacidadeRespostaDTO() {
    }

    public OpcaoPrivacidadeRespostaDTO(UUID id, String descricao, boolean ativo) {
        this.id = id;
        this.descricao = descricao;
        this.ativo = ativo;
    }

    public static OpcaoPrivacidadeRespostaDTO fromModelo(ConfiguracaoPrivacidade configuracao, String descricaoSanitizada) {
        return new OpcaoPrivacidadeRespostaDTO(
                configuracao.getId(),
                descricaoSanitizada,
                configuracao.isAtivo()
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }
}
