package br.com.cloudport.serviconavio.linha.dto;

import java.util.List;

public class ServicoLinhaDTO {

    private final Long identificador;
    private final String codigo;
    private final String nome;
    private final String armador;
    private final List<PortoRotacaoDTO> rotacao;

    public ServicoLinhaDTO(Long identificador, String codigo, String nome, String armador,
                           List<PortoRotacaoDTO> rotacao) {
        this.identificador = identificador;
        this.codigo = codigo;
        this.nome = nome;
        this.armador = armador;
        this.rotacao = rotacao;
    }

    public Long getIdentificador() {
        return identificador;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNome() {
        return nome;
    }

    public String getArmador() {
        return armador;
    }

    public List<PortoRotacaoDTO> getRotacao() {
        return rotacao;
    }
}
