package br.com.cloudport.servicoautenticacao.app.navegacao.dto;

import java.util.List;
import java.util.UUID;

public class AbaNavegacaoDTO {

    private UUID id;
    private String identificador;
    private String rotulo;
    private List<String> rota;
    private boolean desabilitado;
    private String mensagemEmBreve;
    private String grupo;
    private List<String> rolesPermitidos;
    private boolean padrao;

    public AbaNavegacaoDTO(UUID id,
                           String identificador,
                           String rotulo,
                           List<String> rota,
                           boolean desabilitado,
                           String mensagemEmBreve,
                           String grupo,
                           List<String> rolesPermitidos,
                           boolean padrao) {
        this.id = id;
        this.identificador = identificador;
        this.rotulo = rotulo;
        this.rota = rota;
        this.desabilitado = desabilitado;
        this.mensagemEmBreve = mensagemEmBreve;
        this.grupo = grupo;
        this.rolesPermitidos = rolesPermitidos;
        this.padrao = padrao;
    }

    public AbaNavegacaoDTO() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getIdentificador() {
        return identificador;
    }

    public void setIdentificador(String identificador) {
        this.identificador = identificador;
    }

    public String getRotulo() {
        return rotulo;
    }

    public void setRotulo(String rotulo) {
        this.rotulo = rotulo;
    }

    public List<String> getRota() {
        return rota;
    }

    public void setRota(List<String> rota) {
        this.rota = rota;
    }

    public boolean isDesabilitado() {
        return desabilitado;
    }

    public void setDesabilitado(boolean desabilitado) {
        this.desabilitado = desabilitado;
    }

    public String getMensagemEmBreve() {
        return mensagemEmBreve;
    }

    public void setMensagemEmBreve(String mensagemEmBreve) {
        this.mensagemEmBreve = mensagemEmBreve;
    }

    public String getGrupo() {
        return grupo;
    }

    public void setGrupo(String grupo) {
        this.grupo = grupo;
    }

    public List<String> getRolesPermitidos() {
        return rolesPermitidos;
    }

    public void setRolesPermitidos(List<String> rolesPermitidos) {
        this.rolesPermitidos = rolesPermitidos;
    }

    public boolean isPadrao() {
        return padrao;
    }

    public void setPadrao(boolean padrao) {
        this.padrao = padrao;
    }
}
