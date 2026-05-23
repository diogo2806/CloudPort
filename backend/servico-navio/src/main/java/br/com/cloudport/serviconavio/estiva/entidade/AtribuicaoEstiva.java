package br.com.cloudport.serviconavio.estiva.entidade;

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
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Atribuição de um contêiner a uma célula do navio (bay/row/tier) dentro de um
 * plano de estiva. Registra a posição de origem no pátio e o instante de
 * embarque efetivo, que representa a baixa do estoque do pátio.
 */
@Entity
@Table(name = "atribuicao_estiva", uniqueConstraints = {
        @UniqueConstraint(name = "uk_atribuicao_celula", columnNames = {"plano_id", "baia", "fileira", "camada"}),
        @UniqueConstraint(name = "uk_atribuicao_conteiner", columnNames = {"plano_id", "codigo_conteiner"})
})
public class AtribuicaoEstiva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plano_id", nullable = false)
    private PlanoEstiva plano;

    @Column(name = "codigo_conteiner", nullable = false, length = 20)
    private String codigoConteiner;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_carga", nullable = false, length = 20)
    private TipoCargaConteiner tipoCarga;

    @Column(name = "peso_toneladas", precision = 7, scale = 2)
    private BigDecimal pesoToneladas;

    @Column(name = "baia", nullable = false)
    private int baia;

    @Column(name = "fileira", nullable = false)
    private int fileira;

    @Column(name = "camada", nullable = false)
    private int camada;

    @Column(name = "posicao_patio_origem", length = 40)
    private String posicaoPatioOrigem;

    @Column(name = "sequencia_embarque")
    private Integer sequenciaEmbarque;

    @Column(name = "embarcado", nullable = false)
    private boolean embarcado;

    @Column(name = "embarcado_em")
    private LocalDateTime embarcadoEm;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PlanoEstiva getPlano() {
        return plano;
    }

    public void setPlano(PlanoEstiva plano) {
        this.plano = plano;
    }

    public String getCodigoConteiner() {
        return codigoConteiner;
    }

    public void setCodigoConteiner(String codigoConteiner) {
        this.codigoConteiner = codigoConteiner;
    }

    public TipoCargaConteiner getTipoCarga() {
        return tipoCarga;
    }

    public void setTipoCarga(TipoCargaConteiner tipoCarga) {
        this.tipoCarga = tipoCarga;
    }

    public BigDecimal getPesoToneladas() {
        return pesoToneladas;
    }

    public void setPesoToneladas(BigDecimal pesoToneladas) {
        this.pesoToneladas = pesoToneladas;
    }

    public int getBaia() {
        return baia;
    }

    public void setBaia(int baia) {
        this.baia = baia;
    }

    public int getFileira() {
        return fileira;
    }

    public void setFileira(int fileira) {
        this.fileira = fileira;
    }

    public int getCamada() {
        return camada;
    }

    public void setCamada(int camada) {
        this.camada = camada;
    }

    public String getPosicaoPatioOrigem() {
        return posicaoPatioOrigem;
    }

    public void setPosicaoPatioOrigem(String posicaoPatioOrigem) {
        this.posicaoPatioOrigem = posicaoPatioOrigem;
    }

    public Integer getSequenciaEmbarque() {
        return sequenciaEmbarque;
    }

    public void setSequenciaEmbarque(Integer sequenciaEmbarque) {
        this.sequenciaEmbarque = sequenciaEmbarque;
    }

    public boolean isEmbarcado() {
        return embarcado;
    }

    public void setEmbarcado(boolean embarcado) {
        this.embarcado = embarcado;
    }

    public LocalDateTime getEmbarcadoEm() {
        return embarcadoEm;
    }

    public void setEmbarcadoEm(LocalDateTime embarcadoEm) {
        this.embarcadoEm = embarcadoEm;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(LocalDateTime atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }

    @PrePersist
    public void aoCriar() {
        LocalDateTime agora = LocalDateTime.now();
        this.criadoEm = agora;
        this.atualizadoEm = agora;
    }

    @PreUpdate
    public void aoAtualizar() {
        this.atualizadoEm = LocalDateTime.now();
    }
}
