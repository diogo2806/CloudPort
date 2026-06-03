package br.com.cloudport.servicoyard.container.dto;

import br.com.cloudport.servicoyard.patio.modelo.StatusConteiner;

public class ConteinerResumoDTO {
    private Long identificador;
    private String identificacao;
    private String posicaoPatio;
    private StatusConteiner statusOperacional;

    public ConteinerResumoDTO(Long identificador, String identificacao, String posicaoPatio,
                              StatusConteiner statusOperacional) {
        this.identificador = identificador;
        this.identificacao = identificacao;
        this.posicaoPatio = posicaoPatio;
        this.statusOperacional = statusOperacional;
    }

    public Long getIdentificador() { return identificador; }
    public String getIdentificacao() { return identificacao; }
    public String getPosicaoPatio() { return posicaoPatio; }
    public StatusConteiner getStatusOperacional() { return statusOperacional; }
}
