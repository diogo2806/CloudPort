package br.com.cloudport.servicoyard.vesselplanner.modelo;

import br.com.cloudport.servicoyard.vesselplanner.modelo.TampaPoraoTipos.EstadoTampaPorao;
import br.com.cloudport.servicoyard.vesselplanner.modelo.TampaPoraoTipos.TipoOperacaoTampaPorao;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "tampa_porao",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_tampa_porao_plano_codigo",
                columnNames = {"estivagem_plan_id", "codigo"}))
public class TampaPorao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estivagem_plan_id", nullable = false)
    private EstivagemPlan estivagem;

    @Column(nullable = false, length = 40)
    private String codigo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoTampaPorao estado = EstadoTampaPorao.FECHADA;

    @Version
    private Long versao;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @PrePersist
    @PreUpdate
    void touch() {
        atualizadoEm = LocalDateTime.now();
        if (criadoEm == null) {
            criadoEm = atualizadoEm;
        }
        codigo = codigo == null ? null : codigo.trim().toUpperCase();
        if (estado == null) {
            estado = EstadoTampaPorao.FECHADA;
        }
    }

    public void validarInicio(TipoOperacaoTampaPorao tipo) {
        if (tipo == null) {
            throw new IllegalArgumentException("O tipo da operação da tampa deve ser informado.");
        }
        boolean permitido = tipo == TipoOperacaoTampaPorao.ABRIR && estado == EstadoTampaPorao.FECHADA
                || tipo == TipoOperacaoTampaPorao.REMOVER && estado == EstadoTampaPorao.ABERTA
                || tipo == TipoOperacaoTampaPorao.POSICIONAR && estado == EstadoTampaPorao.REMOVIDA
                || tipo == TipoOperacaoTampaPorao.FECHAR
                && (estado == EstadoTampaPorao.ABERTA || estado == EstadoTampaPorao.POSICIONADA);
        if (!permitido) {
            throw new IllegalStateException(
                    "Operação " + tipo + " incompatível com a tampa " + codigo + " no estado " + estado + ".");
        }
    }

    public void confirmar(TipoOperacaoTampaPorao tipo) {
        validarInicio(tipo);
        if (tipo == TipoOperacaoTampaPorao.ABRIR) {
            estado = EstadoTampaPorao.ABERTA;
        } else if (tipo == TipoOperacaoTampaPorao.REMOVER) {
            estado = EstadoTampaPorao.REMOVIDA;
        } else if (tipo == TipoOperacaoTampaPorao.POSICIONAR) {
            estado = EstadoTampaPorao.POSICIONADA;
        } else if (tipo == TipoOperacaoTampaPorao.FECHAR) {
            estado = EstadoTampaPorao.FECHADA;
        }
    }

    public boolean permiteMovimento(boolean slotSobreTampa) {
        if (slotSobreTampa) {
            return estado == EstadoTampaPorao.FECHADA || estado == EstadoTampaPorao.POSICIONADA;
        }
        return estado == EstadoTampaPorao.REMOVIDA;
    }

    public String motivoBloqueio(boolean slotSobreTampa) {
        if (slotSobreTampa) {
            return "A tampa " + codigo + " deve estar fechada ou posicionada para movimentar contêineres sobre ela.";
        }
        return "A tampa " + codigo + " deve estar removida para movimentar contêineres no porão.";
    }
}
