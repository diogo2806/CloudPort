package br.com.cloudport.serviconavio.linha.dto;

public class PortoRotacaoDTO {

    private final Long identificador;
    private final Integer sequencia;
    private final String portoUnloc;
    private final String nomePorto;

    public PortoRotacaoDTO(Long identificador, Integer sequencia, String portoUnloc, String nomePorto) {
        this.identificador = identificador;
        this.sequencia = sequencia;
        this.portoUnloc = portoUnloc;
        this.nomePorto = nomePorto;
    }

    public Long getIdentificador() {
        return identificador;
    }

    public Integer getSequencia() {
        return sequencia;
    }

    public String getPortoUnloc() {
        return portoUnloc;
    }

    public String getNomePorto() {
        return nomePorto;
    }
}
