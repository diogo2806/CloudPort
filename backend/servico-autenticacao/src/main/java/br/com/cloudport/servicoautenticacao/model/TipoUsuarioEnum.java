package br.com.cloudport.servicoautenticacao.model;


public enum TipoUsuarioEnum {
    ADMINISTRADOR("Administrador"),
    USUARIO_REGULAR("Usuário Regular"),
    VISITANTE("Visitante");

    private String descricao;

    TipoUsuarioEnum(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
