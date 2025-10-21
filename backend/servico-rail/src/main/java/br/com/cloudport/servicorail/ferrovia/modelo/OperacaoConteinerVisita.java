package br.com.cloudport.servicorail.ferrovia.modelo;

import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

@Embeddable
public class OperacaoConteinerVisita {

    @Column(name = "codigo_conteiner", nullable = false, length = 20)
    private String codigoConteiner;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_operacao", nullable = false, length = 20)
    private StatusOperacaoConteinerVisita statusOperacao = StatusOperacaoConteinerVisita.PENDENTE;

    public OperacaoConteinerVisita() {
    }

    public OperacaoConteinerVisita(String codigoConteiner, StatusOperacaoConteinerVisita statusOperacao) {
        this.codigoConteiner = codigoConteiner;
        this.statusOperacao = statusOperacao;
    }

    public String getCodigoConteiner() {
        return codigoConteiner;
    }

    public void setCodigoConteiner(String codigoConteiner) {
        this.codigoConteiner = codigoConteiner;
    }

    public StatusOperacaoConteinerVisita getStatusOperacao() {
        return statusOperacao;
    }

    public void setStatusOperacao(StatusOperacaoConteinerVisita statusOperacao) {
        this.statusOperacao = statusOperacao;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OperacaoConteinerVisita that = (OperacaoConteinerVisita) o;
        return Objects.equals(codigoConteiner, that.codigoConteiner)
                && statusOperacao == that.statusOperacao;
    }

    @Override
    public int hashCode() {
        return Objects.hash(codigoConteiner, statusOperacao);
    }
}
