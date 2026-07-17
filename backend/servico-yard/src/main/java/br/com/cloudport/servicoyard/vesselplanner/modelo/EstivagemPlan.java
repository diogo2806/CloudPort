package br.com.cloudport.servicoyard.vesselplanner.modelo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Version;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "estivagem_plan")
public class EstivagemPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bay_plan_id")
    private Long bayPlanId;

    @Column(name = "codigo_navio", nullable = false, length = 50)
    private String codigoNavio;

    @Column(name = "codigo_viagem", nullable = false, length = 30)
    private String codigoViagem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusEstivagemPlan status;

    @Column(name = "comprimento_lpp")
    private Double comprimentoLpp;

    @Column
    private Double boca;

    @Column
    private Double calado;

    @Column
    private Double deslocamento;

    @Column
    private Double gm;

    @Column
    private Double tpc;

    @Column
    private Double lcb;

    @Column
    private Double km;

    @Column(name = "mct_1cm")
    private Double mct1cm;

    @Column(name = "calado_maximo")
    private Double caladoMaximo;

    @Column(name = "trim_maximo")
    private Double trimMaximo;

    @Column(name = "banda_maxima")
    private Double bandaMaxima;

    @Column(name = "gm_minimo")
    private Double gmMinimo;

    @Column(name = "peso_leve_t")
    private Double pesoLeveToneladas;

    @Column(name = "lcg_peso_leve")
    private Double lcgPesoLeve;

    @Column(name = "tcg_peso_leve")
    private Double tcgPesoLeve;

    @Column(name = "vcg_peso_leve")
    private Double vcgPesoLeve;

    @Column(name = "peso_lastro_t")
    private Double pesoLastroToneladas;

    @Column(name = "lcg_lastro")
    private Double lcgLastro;

    @Column(name = "tcg_lastro")
    private Double tcgLastro;

    @Column(name = "vcg_lastro")
    private Double vcgLastro;

    @Column(name = "versao_dados_hidrostaticos", length = 80)
    private String versaoDadosHidrostaticos;

    @Column(name = "versao_dados_estruturais", length = 80)
    private String versaoDadosEstruturais;

    @Column(name = "posicoes_secoes", columnDefinition = "TEXT")
    private String posicoesSecoes;

    @Column(name = "peso_leve_secoes", columnDefinition = "TEXT")
    private String pesoLeveSecoes;

    @Column(name = "empuxo_secoes", columnDefinition = "TEXT")
    private String empuxoSecoes;

    @Column(name = "limites_sf_secoes", columnDefinition = "TEXT")
    private String limitesSfSecoes;

    @Column(name = "limites_bm_secoes", columnDefinition = "TEXT")
    private String limitesBmSecoes;

    @Column(name = "trim_calculado")
    private Double trimCalculado;

    @Column(name = "list_calculado")
    private Double listCalculado;

    @Column(name = "calado_calculado")
    private Double caladoCalculado;

    @Column(name = "gm_calculado")
    private Double gmCalculado;

    @Column(name = "lcg_calculado")
    private Double lcgCalculado;

    @Column(name = "tcg_calculado")
    private Double tcgCalculado;

    @Column(name = "sf_max_calculado")
    private Double sfMaxCalculado;

    @Column(name = "bm_max_calculado")
    private Double bmMaxCalculado;

    @Column(name = "versao_hidro_aprovacao", length = 80)
    private String versaoHidroAprovacao;

    @Column(name = "versao_estrutural_aprovacao", length = 80)
    private String versaoEstruturalAprovacao;

    @Column(name = "memoria_calculo_aprovacao", columnDefinition = "TEXT")
    private String memoriaCalculoAprovacao;

    @Column(name = "aprovado_em")
    private LocalDateTime aprovadoEm;

    @Version
    private Long versao;

    @OneToMany(mappedBy = "estivagem", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<SlotNavio> slots = new ArrayList<>();

    @Column(name = "criado_em")
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    public EstivagemPlan() {
        this.status = StatusEstivagemPlan.RASCUNHO;
    }

    @PrePersist
    @PreUpdate
    void touch() {
        atualizadoEm = LocalDateTime.now();
        if (criadoEm == null) {
            criadoEm = atualizadoEm;
        }
    }
}
