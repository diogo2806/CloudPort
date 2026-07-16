package br.com.cloudport.servicoyard.edi.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class CoprarMensagemDto {

    @Size(max = 50)
    private String codigoNavio;

    @Size(max = 30)
    private String codigoViagem;

    @NotBlank(message = "O conteudo EDIFACT COPRAR e obrigatorio.")
    private String conteudoEdifact;

    @Size(max = 100)
    private String referenciaMensagem;

    public String getCodigoNavio() { return codigoNavio; }
    public void setCodigoNavio(String codigoNavio) { this.codigoNavio = codigoNavio; }
    public String getCodigoViagem() { return codigoViagem; }
    public void setCodigoViagem(String codigoViagem) { this.codigoViagem = codigoViagem; }
    public String getConteudoEdifact() { return conteudoEdifact; }
    public void setConteudoEdifact(String conteudoEdifact) { this.conteudoEdifact = conteudoEdifact; }
    public String getReferenciaMensagem() { return referenciaMensagem; }
    public void setReferenciaMensagem(String referenciaMensagem) { this.referenciaMensagem = referenciaMensagem; }
}
