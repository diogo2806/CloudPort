package br.com.cloudport.servicoautenticacao.app.seguranca.dto;

import br.com.cloudport.servicoautenticacao.app.seguranca.PoliticaSeguranca;
import java.util.UUID;

public class DiretrizSegurancaDTO {

    private UUID id;
    private String titulo;
    private String descricao;
    private String versao;
    private Integer ordem;

    public DiretrizSegurancaDTO() {
    }

    public DiretrizSegurancaDTO(UUID id, String titulo, String descricao, String versao, Integer ordem) {
        this.id = id;
        this.titulo = titulo;
        this.descricao = descricao;
        this.versao = versao;
        this.ordem = ordem;
    }

    public static DiretrizSegurancaDTO fromModelo(
            PoliticaSeguranca politica,
            String tituloSanitizado,
            String descricaoSanitizada,
            String versaoSanitizada
    ) {
        return new DiretrizSegurancaDTO(
                politica.getId(),
                tituloSanitizado,
                descricaoSanitizada,
                versaoSanitizada,
                politica.getOrdem()
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getVersao() {
        return versao;
    }

    public void setVersao(String versao) {
        this.versao = versao;
    }

    public Integer getOrdem() {
        return ordem;
    }

    public void setOrdem(Integer ordem) {
        this.ordem = ordem;
    }
}
