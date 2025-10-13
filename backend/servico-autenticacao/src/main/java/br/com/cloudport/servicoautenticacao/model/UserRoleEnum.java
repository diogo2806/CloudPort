package br.com.cloudport.servicoautenticacao.model;

public enum UserRoleEnum {
    ADMIN_PORTO("ROLE_ADMIN_PORTO"),
    PLANEJADOR("ROLE_PLANEJADOR"),
    OPERADOR_GATE("ROLE_OPERADOR_GATE"),
    TRANSPORTADORA("ROLE_TRANSPORTADORA");

    private final String role;

    UserRoleEnum(String role){
        this.role = role;
    }

    public String getRole(){
        return role;
    }
}
