package br.com.cloudport.servicoautenticacao.model;

public enum TipoPapelUsuarioEnum {
    ADMINISTRADOR_PORTO("ROLE_ADMIN_PORTO"),
    PLANEJADOR("ROLE_PLANEJADOR"),
    OPERADOR_GATE("ROLE_OPERADOR_GATE"),
    TRANSPORTADORA("ROLE_TRANSPORTADORA");

    private final String identificador;

    TipoPapelUsuarioEnum(String identificador) {
        this.identificador = identificador;
    }

    public String getIdentificador() {
        return identificador;
    }
}
