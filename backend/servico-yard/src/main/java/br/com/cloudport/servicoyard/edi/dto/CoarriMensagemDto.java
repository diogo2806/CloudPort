package br.com.cloudport.servicoyard.edi.dto;

/**
 * Representa uma mensagem COARRI (Container Arrival/Departure Acknowledgement).
 * COARRI confirma a conclusão das operações com tempo real de execução.
 */
public class CoarriMensagemDto {

    private String codigoNavio;
    private String codigoViagem;
    private String conteudoEdifact;
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
