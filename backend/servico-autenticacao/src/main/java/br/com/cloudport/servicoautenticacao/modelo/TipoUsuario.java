package br.com.cloudport.servicoautenticacao.modelo;


public enum TipoUsuario {
    ADMINISTRADOR("Administrador"),
    USUARIO_REGULAR("Usuário Regular"),
    VISITANTE("Visitante");

    private String descricao;

    TipoUsuario(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
