package br.com.cloudport.serviconavio.linha.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class PortoRotacaoRequest {

    @NotNull(message = "Informe a sequência do porto na rotação.")
    private Integer sequencia;

    @NotBlank(message = "Informe o código UN/LOCODE do porto.")
    @Size(max = 10, message = "O código do porto deve ter no máximo 10 caracteres.")
    private String portoUnloc;

    @Size(max = 80, message = "O nome do porto deve ter no máximo 80 caracteres.")
    private String nomePorto;

    public Integer getSequencia() {
        return sequencia;
    }

    public void setSequencia(Integer sequencia) {
        this.sequencia = sequencia;
    }

    public String getPortoUnloc() {
        return portoUnloc;
    }

    public void setPortoUnloc(String portoUnloc) {
        this.portoUnloc = portoUnloc;
    }

    public String getNomePorto() {
        return nomePorto;
    }

    public void setNomePorto(String nomePorto) {
        this.nomePorto = nomePorto;
    }
}
