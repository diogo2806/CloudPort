package br.com.cloudport.servicogate.app.cidadao.dto;

public class MotoristaDTO {

    private Long id;
    private String nome;
    private String documento;
    private String telefone;
    private Long transportadoraId;
    private String transportadoraNome;

    public MotoristaDTO() {
    }

    public MotoristaDTO(Long id, String nome, String documento, String telefone,
                         Long transportadoraId, String transportadoraNome) {
        this.id = id;
        this.nome = nome;
        this.documento = documento;
        this.telefone = telefone;
        this.transportadoraId = transportadoraId;
        this.transportadoraNome = transportadoraNome;
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

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public Long getTransportadoraId() {
        return transportadoraId;
    }

    public void setTransportadoraId(Long transportadoraId) {
        this.transportadoraId = transportadoraId;
    }

    public String getTransportadoraNome() {
        return transportadoraNome;
    }

    public void setTransportadoraNome(String transportadoraNome) {
        this.transportadoraNome = transportadoraNome;
    }
}
