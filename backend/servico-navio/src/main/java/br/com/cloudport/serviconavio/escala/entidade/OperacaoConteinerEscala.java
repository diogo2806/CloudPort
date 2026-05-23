package br.com.cloudport.serviconavio.escala.entidade;

import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

@Embeddable
public class OperacaoConteinerEscala {

    @Column(name = "codigo_conteiner", nullable = false, length = 20)
    private String codigoConteiner;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_operacao", nullable = false, length = 20)
    private StatusOperacaoConteinerEscala statusOperacao = StatusOperacaoConteinerEscala.PENDENTE;

    public OperacaoConteinerEscala() {
    }

    public OperacaoConteinerEscala(String codigoConteiner, StatusOperacaoConteinerEscala statusOperacao) {
        this.codigoConteiner = codigoConteiner;
        this.statusOperacao = statusOperacao;
    }

    public String getCodigoConteiner() {
        return codigoConteiner;
    }

    public void setCodigoConteiner(String codigoConteiner) {
        this.codigoConteiner = codigoConteiner;
    }

    public StatusOperacaoConteinerEscala getStatusOperacao() {
        return statusOperacao;
    }

    public void setStatusOperacao(StatusOperacaoConteinerEscala statusOperacao) {
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
        OperacaoConteinerEscala that = (OperacaoConteinerEscala) o;
        return Objects.equals(codigoConteiner, that.codigoConteiner)
                && statusOperacao == that.statusOperacao;
    }

    @Override
    public int hashCode() {
        return Objects.hash(codigoConteiner, statusOperacao);
    }
}
