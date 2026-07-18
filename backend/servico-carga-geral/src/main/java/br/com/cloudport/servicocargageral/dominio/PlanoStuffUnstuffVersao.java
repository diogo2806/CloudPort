package br.com.cloudport.servicocargageral.dominio;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OrderColumn;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(
        name = "plano_stuff_unstuff_versao",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_plano_stuff_unstuff_versao",
                columnNames = {"operacao_id", "numero_versao"}))
public class PlanoStuffUnstuffVersao {

    public enum StatusPlano {
        RASCUNHO,
        LIBERADO,
        SUPERADO,
        CANCELADO
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operacao_id", nullable = false)
    private OperacaoStuffUnstuff operacao;

    @Column(name = "numero_versao", nullable = false)
    private int numeroVersao;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusPlano status = StatusPlano.RASCUNHO;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "plano_stuff_unstuff_item", joinColumns = @JoinColumn(name = "plano_id"))
    @OrderColumn(name = "ordem")
    private List<ItemPlano> itens = new ArrayList<>();

    @Column(name = "criado_por", nullable = false, length = 120)
    private String criadoPor;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "liberado_por", length = 120)
    private String liberadoPor;

    @Column(name = "liberado_em")
    private OffsetDateTime liberadoEm;

    @Column(name = "motivo", length = 1000)
    private String motivo;

    @PrePersist
    void prePersist() {
        criadoEm = OffsetDateTime.now();
    }

    public void adicionarItem(UUID loteId, BigDecimal quantidade, BigDecimal volumeM3, BigDecimal pesoKg) {
        itens.add(new ItemPlano(loteId, quantidade, volumeM3, pesoKg));
    }

    public void liberar(String usuario, String motivoLiberacao) {
        if (status != StatusPlano.RASCUNHO) {
            throw new IllegalStateException("Somente plano em rascunho pode ser liberado.");
        }
        status = StatusPlano.LIBERADO;
        liberadoPor = usuario;
        liberadoEm = OffsetDateTime.now();
        motivo = motivoLiberacao;
    }

    public void superar() {
        if (status == StatusPlano.LIBERADO || status == StatusPlano.RASCUNHO) {
            status = StatusPlano.SUPERADO;
        }
    }

    public UUID getId() {
        return id;
    }

    public OperacaoStuffUnstuff getOperacao() {
        return operacao;
    }

    public void setOperacao(OperacaoStuffUnstuff operacao) {
        this.operacao = operacao;
    }

    public int getNumeroVersao() {
        return numeroVersao;
    }

    public void setNumeroVersao(int numeroVersao) {
        this.numeroVersao = numeroVersao;
    }

    public StatusPlano getStatus() {
        return status;
    }

    public List<ItemPlano> getItens() {
        return Collections.unmodifiableList(itens);
    }

    public String getCriadoPor() {
        return criadoPor;
    }

    public void setCriadoPor(String criadoPor) {
        this.criadoPor = criadoPor;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }

    public String getLiberadoPor() {
        return liberadoPor;
    }

    public OffsetDateTime getLiberadoEm() {
        return liberadoEm;
    }

    public String getMotivo() {
        return motivo;
    }

    @Embeddable
    public static class ItemPlano {

        @Column(name = "lote_id", nullable = false)
        private UUID loteId;

        @Column(name = "quantidade_planejada", nullable = false, precision = 19, scale = 3)
        private BigDecimal quantidadePlanejada;

        @Column(name = "volume_planejado_m3", nullable = false, precision = 19, scale = 3)
        private BigDecimal volumePlanejadoM3;

        @Column(name = "peso_planejado_kg", nullable = false, precision = 19, scale = 3)
        private BigDecimal pesoPlanejadoKg;

        protected ItemPlano() {
        }

        public ItemPlano(UUID loteId, BigDecimal quantidadePlanejada, BigDecimal volumePlanejadoM3,
                BigDecimal pesoPlanejadoKg) {
            this.loteId = loteId;
            this.quantidadePlanejada = quantidadePlanejada;
            this.volumePlanejadoM3 = volumePlanejadoM3;
            this.pesoPlanejadoKg = pesoPlanejadoKg;
        }

        public UUID getLoteId() {
            return loteId;
        }

        public BigDecimal getQuantidadePlanejada() {
            return quantidadePlanejada;
        }

        public BigDecimal getVolumePlanejadoM3() {
            return volumePlanejadoM3;
        }

        public BigDecimal getPesoPlanejadoKg() {
            return pesoPlanejadoKg;
        }
    }
}
