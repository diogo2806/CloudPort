package br.com.cloudport.servicoyard.container.entidade;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "conteiner")
public class Conteiner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(name = "identificacao", nullable = false, length = 20, unique = true)
    private String identificacao;

    @Column(name = "posicao_patio", nullable = false, length = 60)
    private String posicaoPatio;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_carga", nullable = false, length = 40)
    private TipoCargaConteiner tipoCarga;

    @Column(name = "peso_toneladas", nullable = false, precision = 10, scale = 3)
    private BigDecimal pesoToneladas;

    @Column(name = "restricoes", length = 255)
    private String restricoes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_operacional", nullable = false, length = 30)
    private StatusOperacionalConteiner statusOperacional;

    @Column(name = "ultima_atualizacao", nullable = false)
    @Setter(AccessLevel.NONE)
    private OffsetDateTime ultimaAtualizacao;

    @Version
    @Column(name = "versao")
    @Setter(AccessLevel.NONE)
    private Long versao;

    @PrePersist
    @PreUpdate
    public void atualizarUltimaAtualizacao() {
        ultimaAtualizacao = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
