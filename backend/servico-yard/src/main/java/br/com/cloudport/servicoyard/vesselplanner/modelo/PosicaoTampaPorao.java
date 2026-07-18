package br.com.cloudport.servicoyard.vesselplanner.modelo;

import br.com.cloudport.servicoyard.vesselplanner.modelo.TampaPoraoTipos.TipoPosicaoTampaPorao;
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
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "posicao_tampa_porao")
public class PosicaoTampaPorao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tampa_porao_id", nullable = false)
    private TampaPorao tampa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoPosicaoTampaPorao tipo;

    @Column(nullable = false, length = 120)
    private String referencia;

    @Column(nullable = false)
    private boolean ativa = true;

    @Column(name = "inicio_em", nullable = false)
    private LocalDateTime inicioEm;

    @Column(name = "fim_em")
    private LocalDateTime fimEm;

    @PrePersist
    void prePersist() {
        if (inicioEm == null) {
            inicioEm = LocalDateTime.now();
        }
        referencia = referencia == null ? null : referencia.trim();
    }

    public void encerrar() {
        ativa = false;
        fimEm = LocalDateTime.now();
    }
}
