package br.com.cloudport.servicoyard.estivagembulk.modelo;

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
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
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
@Table(name = "plano_estiva_bulk")
public class PlanoEstivaBulk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "navio_granel_id")
    private NavioGranel navio;

    @Column(name = "codigo_viagem", length = 30)
    private String codigoViagem;

    @Column(name = "porto_carga", length = 10)
    private String portoCarga;

    @Column(name = "porto_descarga", length = 10)
    private String portoDescarga;

    @Enumerated(EnumType.STRING)
    @Column(length = 25)
    private StatusPlanoEstiva status;

    @Column(name = "bm_max_calculado")
    private Double bmMaxCalculado;

    @Column(name = "sf_max_calculado")
    private Double sfMaxCalculado;

    @Column(name = "trim_calculado")
    private Double trimCalculado;

    @Column(name = "list_calculado")
    private Double listCalculado;

    @Column(name = "gm_calculado")
    private Double gmCalculado;

    @Column(name = "calado_saida")
    private Double calado_saida;

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

    @OneToMany(mappedBy = "plano", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<BobinaManifesto> bobinas = new ArrayList<>();

    @OneToMany(mappedBy = "plano", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<PosicaoBobina> posicoes = new ArrayList<>();

    @OneToMany(mappedBy = "plano", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<MaterialLashingBulk> materiais = new ArrayList<>();

    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

    public PlanoEstivaBulk() {
        this.status = StatusPlanoEstiva.RASCUNHO;
    }

    @PrePersist
    @PreUpdate
    void touch() {
        LocalDateTime now = LocalDateTime.now();
        if (criadoEm == null) {
            criadoEm = now;
        }
        atualizadoEm = now;
    }
}
