package br.com.cloudport.servicogate.app.cidadao.centralacao.dto;

public class UsuarioCentralAcaoDTO {

    private String login;
    private String nome;
    private String perfil;
    private String transportadoraDocumento;
    private String transportadoraNome;

    public UsuarioCentralAcaoDTO() {
    }

    public UsuarioCentralAcaoDTO(String login,
                                  String nome,
                                  String perfil,
                                  String transportadoraDocumento,
                                  String transportadoraNome) {
        this.login = login;
        this.nome = nome;
        this.perfil = perfil;
        this.transportadoraDocumento = transportadoraDocumento;
        this.transportadoraNome = transportadoraNome;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getPerfil() {
        return perfil;
    }

    public void setPerfil(String perfil) {
        this.perfil = perfil;
    }

    public String getTransportadoraDocumento() {
        return transportadoraDocumento;
    }

    public void setTransportadoraDocumento(String transportadoraDocumento) {
        this.transportadoraDocumento = transportadoraDocumento;
    }

    public String getTransportadoraNome() {
        return transportadoraNome;
    }

    public void setTransportadoraNome(String transportadoraNome) {
        this.transportadoraNome = transportadoraNome;
    }
}
