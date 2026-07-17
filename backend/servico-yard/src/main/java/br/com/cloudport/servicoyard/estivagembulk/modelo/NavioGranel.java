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
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "navio_granel")
public class NavioGranel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "navio_cadastro_id")
    private Long navioCadastroId;

    @Column(name = "versao_perfil", nullable = false)
    private Long versaoPerfil = 1L;

    @Column(name = "versao_navio_canonico")
    private Long versaoNavioCanonico;

    @Column(length = 10)
    private String imo;

    @Column(nullable = false, length = 100)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ClasseNavio classe;

    @Column(name = "lpp")
    private Double lpp;

    private Double boca;
    private Double calado;
    private Double deslocamento;
    private Double gm;
    private Double tpc;
    private Double lcb;
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

    @Column(name = "bm_max_permitido")
    private Double bmMaxPermitido;

    @Column(name = "sf_max_permitido")
    private Double sfMaxPermitido;

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

    @Column(name = "is_template")
    private boolean isTemplate;

    @OneToMany(mappedBy = "navio", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<PoraoNavio> poroes = new ArrayList<>();

    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

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
