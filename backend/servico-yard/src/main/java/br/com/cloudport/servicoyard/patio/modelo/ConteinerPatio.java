package br.com.cloudport.servicoyard.patio.modelo;

import br.com.cloudport.servicoyard.container.entidade.TipoCargaConteiner;
import java.math.BigDecimal;
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
import javax.persistence.Version;

@Entity
@Table(name = "conteiner_patio")
public class ConteinerPatio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo", nullable = false, length = 30, unique = true)
    private String codigo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_conteiner", nullable = false, length = 30)
    private StatusConteiner status;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_carga", length = 40)
    private TipoCargaConteiner tipoCarga;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carga_id")
    private CargaPatio carga;

    @Column(name = "destino", nullable = false, length = 60)
    private String destino;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "posicao_id")
    private PosicaoPatio posicao;

    @Column(name = "peso_toneladas", precision = 10, scale = 3)
    private BigDecimal pesoToneladas;

    @Column(name = "restricoes", length = 255)
    private String restricoes;

    @Version
    @Column(name = "versao")
    private Long versao;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @PrePersist
    @PreUpdate
    public void atualizarTimestamp() {
        atualizadoEm = LocalDateTime.now();
    }

    public ConteinerPatio() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public StatusConteiner getStatus() { return status; }
    public void setStatus(StatusConteiner status) { this.status = status; }

    public TipoCargaConteiner getTipoCarga() { return tipoCarga; }
    public void setTipoCarga(TipoCargaConteiner tipoCarga) { this.tipoCarga = tipoCarga; }

    public CargaPatio getCarga() { return carga; }
    public void setCarga(CargaPatio carga) { this.carga = carga; }

    public String getDestino() { return destino; }
    public void setDestino(String destino) { this.destino = destino; }

    public PosicaoPatio getPosicao() { return posicao; }
    public void setPosicao(PosicaoPatio posicao) { this.posicao = posicao; }

    public BigDecimal getPesoToneladas() { return pesoToneladas; }
    public void setPesoToneladas(BigDecimal pesoToneladas) { this.pesoToneladas = pesoToneladas; }

    public String getRestricoes() { return restricoes; }
    public void setRestricoes(String restricoes) { this.restricoes = restricoes; }

    public Long getVersao() { return versao; }

    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(LocalDateTime atualizadoEm) { this.atualizadoEm = atualizadoEm; }
}
