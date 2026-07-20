package br.com.cloudport.servicorail.ferrovia.inspecao.modelo;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

@Embeddable
public class DefeitoInspecaoVagao {

    public enum SeveridadeDefeitoVagao {
        BAIXA,
        MEDIA,
        ALTA,
        CRITICA
    }

    @Column(name = "codigo", nullable = false, length = 40)
    private String codigo;

    @Column(name = "descricao", nullable = false, length = 500)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(name = "severidade", nullable = false, length = 20)
    private SeveridadeDefeitoVagao severidade;

    @Column(name = "evidencia", length = 500)
    private String evidencia;

    public DefeitoInspecaoVagao() {
    }

    public DefeitoInspecaoVagao(String codigo,
                                String descricao,
                                SeveridadeDefeitoVagao severidade,
                                String evidencia) {
        this.codigo = codigo;
        this.descricao = descricao;
        this.severidade = severidade;
        this.evidencia = evidencia;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public SeveridadeDefeitoVagao getSeveridade() {
        return severidade;
    }

    public void setSeveridade(SeveridadeDefeitoVagao severidade) {
        this.severidade = severidade;
    }

    public String getEvidencia() {
        return evidencia;
    }

    public void setEvidencia(String evidencia) {
        this.evidencia = evidencia;
    }
}
