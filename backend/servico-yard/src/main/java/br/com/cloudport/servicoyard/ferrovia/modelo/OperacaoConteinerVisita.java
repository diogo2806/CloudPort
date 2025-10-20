package br.com.cloudport.servicoyard.ferrovia.modelo;

import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

@Embeddable
public class OperacaoConteinerVisita {

    @Column(name = "id_conteiner", nullable = false)
    private Long idConteiner;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_operacao", nullable = false, length = 20)
    private StatusOperacaoConteinerVisita statusOperacao = StatusOperacaoConteinerVisita.PENDENTE;

    public OperacaoConteinerVisita() {
    }

    public OperacaoConteinerVisita(Long idConteiner, StatusOperacaoConteinerVisita statusOperacao) {
        this.idConteiner = idConteiner;
        this.statusOperacao = statusOperacao;
    }

    public Long getIdConteiner() {
        return idConteiner;
    }

    public void setIdConteiner(Long idConteiner) {
        this.idConteiner = idConteiner;
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
        return Objects.equals(idConteiner, that.idConteiner)
                && statusOperacao == that.statusOperacao;
    }

    @Override
    public int hashCode() {
        return Objects.hash(idConteiner, statusOperacao);
    }
}
