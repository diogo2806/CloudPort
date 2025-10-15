package br.com.cloudport.servicogate.app.cidadao.dto;

public class TransportadoraDTO {

    private Long id;
    private String nome;
    private String documento;
    private String contato;

    public TransportadoraDTO() {
    }

    public TransportadoraDTO(Long id, String nome, String documento, String contato) {
        this.id = id;
        this.nome = nome;
        this.documento = documento;
        this.contato = contato;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDocumento() {
        return documento;
    }

    public void setDocumento(String documento) {
        this.documento = documento;
    }

    public String getContato() {
        return contato;
    }

    public void setContato(String contato) {
        this.contato = contato;
    }
}
