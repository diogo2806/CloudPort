package br.com.cloudport.servicoautenticacao.app.navegacao;

import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "configuracoes_navegacao")
public class ConfiguracaoNavegacao {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "identificador", nullable = false, unique = true, length = 160)
    private String identificador;

    @Column(name = "rotulo", nullable = false, length = 180)
    private String rotulo;

    @Column(name = "rota", nullable = false, length = 200)
    private String rota;

    @Column(name = "grupo", nullable = false, length = 80)
    private String grupo;

    @Column(name = "roles_permitidos", nullable = false, length = 400)
    private String rolesPermitidos;

    @Column(name = "desabilitado", nullable = false)
    private boolean desabilitado;

    @Column(name = "mensagem_em_breve", length = 180)
    private String mensagemEmBreve;

    @Column(name = "ordem", nullable = false)
    private int ordem;

    @Column(name = "padrao", nullable = false)
    private boolean padrao;

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

    public String getRota() {
        return rota;
    }

    public void setRota(String rota) {
        this.rota = rota;
    }

    public String getGrupo() {
        return grupo;
    }

    public void setGrupo(String grupo) {
        this.grupo = grupo;
    }

    public String getRolesPermitidos() {
        return rolesPermitidos;
    }

    public void setRolesPermitidos(String rolesPermitidos) {
        this.rolesPermitidos = rolesPermitidos;
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

    public int getOrdem() {
        return ordem;
    }

    public void setOrdem(int ordem) {
        this.ordem = ordem;
    }

    public boolean isPadrao() {
        return padrao;
    }

    public void setPadrao(boolean padrao) {
        this.padrao = padrao;
    }
}
