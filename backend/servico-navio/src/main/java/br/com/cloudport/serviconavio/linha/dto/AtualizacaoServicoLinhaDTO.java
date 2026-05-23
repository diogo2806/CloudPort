package br.com.cloudport.serviconavio.linha.dto;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.Size;

public class AtualizacaoServicoLinhaDTO {

    @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres.")
    private String nome;

    @Size(max = 80, message = "O armador deve ter no máximo 80 caracteres.")
    private String armador;

    @Valid
    private List<PortoRotacaoRequest> rotacao;

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
