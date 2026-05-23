package br.com.cloudport.serviconavio.linha.dto;

import java.util.ArrayList;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class CadastroServicoLinhaDTO {

    @NotBlank(message = "Informe o código do serviço de linha.")
    @Size(max = 20, message = "O código deve ter no máximo 20 caracteres.")
    private String codigo;

    @NotBlank(message = "Informe o nome do serviço de linha.")
    @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres.")
    private String nome;

    @Size(max = 80, message = "O armador deve ter no máximo 80 caracteres.")
    private String armador;

    @Valid
    private List<PortoRotacaoRequest> rotacao = new ArrayList<>();

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getArmador() {
        return armador;
    }

    public void setArmador(String armador) {
        this.armador = armador;
    }

    public List<PortoRotacaoRequest> getRotacao() {
        return rotacao;
    }

    public void setRotacao(List<PortoRotacaoRequest> rotacao) {
        this.rotacao = rotacao;
    }
}
