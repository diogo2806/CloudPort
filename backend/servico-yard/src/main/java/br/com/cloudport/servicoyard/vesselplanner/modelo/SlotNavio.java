package br.com.cloudport.servicoyard.vesselplanner.modelo;

import br.com.cloudport.servicoyard.edi.modelo.EstadoCargaContainer;
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
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "slot_navio")
public class SlotNavio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estivagem_plan_id")
    private EstivagemPlan estivagem;

    @Column(nullable = false)
    private int bay;

    @Column(name = "row_bay", nullable = false)
    private int rowBay;

    @Column(nullable = false)
    private int tier;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_slot", length = 30)
    private TipoSlotNavio tipoSlot;

    @Column(name = "codigo_hatch_cover", length = 40)
    private String codigoHatchCover;

    @Column(name = "sobre_hatch_cover", nullable = false)
    private boolean sobreHatchCover;

    @Column(nullable = false)
    private boolean restrito;

    @Column(name = "motivo_restricao", length = 255)
    private String motivoRestricao;

    @Column(name = "tomada_reefer", nullable = false)
    private boolean tomadaReefer;

    @Column(name = "aceita_20_pes", nullable = false)
    private boolean aceita20Pes;

    @Column(name = "aceita_40_pes", nullable = false)
    private boolean aceita40Pes;

    @Column(name = "aceita_45_pes", nullable = false)
    private boolean aceita45Pes;

    @Column(name = "max_peso_kg")
    private Double maxPesoKg;

    @Column(name = "max_peso_pilha_kg")
    private Double maxPesoPilhaKg;

    @Column(name = "pos_longitudinal_m")
    private Double posLongitudinalMetros;

    @Column(name = "pos_transversal_m")
    private Double posTransversalMetros;

    @Column(name = "pos_vertical_m")
    private Double posVerticalMetros;

    @Column(name = "codigo_container", length = 20)
    private String codigoContainer;

    @Column(name = "iso_code", length = 10)
    private String isoCode;

    @Column(name = "peso_kg")
    private Double pesoKg;

    @Column(name = "peso_vgm_kg")
    private Double pesoVgmKg;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_carga", nullable = false, length = 20)
    private EstadoCargaContainer estadoCarga = EstadoCargaContainer.DESCONHECIDO;

    @Column(name = "porto_carga", length = 10)
    private String portoCarga;

    @Column(name = "porto_descarga", length = 10)
    private String portoDescarga;

    @Column(name = "classe_imo", length = 20)
    private String classeImo;

    @Column(name = "numero_onu", length = 20)
    private String numeroOnu;

    @Column(name = "grupo_segregacao", length = 50)
    private String grupoSegregacao;

    @Column(nullable = false)
    private boolean perigoso;

    @Column(nullable = false)
    private boolean reefer;

    @Column(name = "temperatura_requerida_c")
    private Double temperaturaRequeridaC;

    @Column(name = "temperatura_minima_c")
    private Double temperaturaMinimaC;

    @Column(name = "temperatura_maxima_c")
    private Double temperaturaMaximaC;

    @Column(nullable = false)
    private boolean oog;

    @Column(name = "excesso_frontal_cm")
    private Double excessoFrontalCm;

    @Column(name = "excesso_traseiro_cm")
    private Double excessoTraseiroCm;

    @Column(name = "excesso_esquerdo_cm")
    private Double excessoEsquerdoCm;

    @Column(name = "excesso_direito_cm")
    private Double excessoDireitoCm;

    @Column(name = "excesso_altura_cm")
    private Double excessoAlturaCm;

    @Column(name = "status_alertas", length = 20)
    private String statusAlertas;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_reconciliacao", nullable = false, length = 30)
    private StatusReconciliacaoSlot statusReconciliacao = StatusReconciliacaoSlot.NAO_RECONCILIADO;

    @Enumerated(EnumType.STRING)
    @Column(name = "severidade_reconciliacao", length = 20)
    private SeveridadeDivergenciaReconciliacao severidadeReconciliacao;

    @Column(name = "reconciliado_em")
    private LocalDateTime reconciliadoEm;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @PrePersist
    @PreUpdate
    void touch() {
        atualizadoEm = LocalDateTime.now();
        if (criadoEm == null) {
            criadoEm = atualizadoEm;
        }
        if (estadoCarga == null) {
            estadoCarga = EstadoCargaContainer.DESCONHECIDO;
        }
        if (statusReconciliacao == null) {
            statusReconciliacao = StatusReconciliacaoSlot.NAO_RECONCILIADO;
        }
    }

    public boolean aceitaComprimentoPes(int comprimentoPes) {
        if (comprimentoPes == 20) {
            return aceita20Pes;
        }
        if (comprimentoPes == 40) {
            return aceita40Pes;
        }
        if (comprimentoPes == 45) {
            return aceita45Pes;
        }
        return false;
    }
}
