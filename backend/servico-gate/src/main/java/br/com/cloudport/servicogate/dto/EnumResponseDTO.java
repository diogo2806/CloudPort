package br.com.cloudport.servicogate.dto;

public class EnumResponseDTO {

    private String codigo;
    private String descricao;

    public EnumResponseDTO() {
    }

    public EnumResponseDTO(String codigo, String descricao) {
        this.codigo = codigo;
        this.descricao = descricao;
    }

    public static EnumResponseDTO fromEnum(Enum<?> valor, String descricao) {
        if (valor == null) {
            return new EnumResponseDTO(null, descricao);
        }
        return new EnumResponseDTO(valor.name(), descricao);
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }
}
