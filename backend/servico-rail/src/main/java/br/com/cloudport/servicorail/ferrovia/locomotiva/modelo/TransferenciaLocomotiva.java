package br.com.cloudport.servicorail.ferrovia.locomotiva.modelo;

import br.com.cloudport.servicorail.ferrovia.modelo.VisitaTrem;
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
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;

@Entity
@Table(name = "transferencia_locomotiva",
        uniqueConstraints = @UniqueConstraint(name = "uk_transferencia_locomotiva_visita_identificador",
                columnNames = {"visita_trem_id", "identificador_locomotiva"}))
public class TransferenciaLocomotiva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visita_trem_id", nullable = false)
    private VisitaTrem visitaTrem;

    @Column(name = "identificador_locomotiva", nullable = false, length = 60)
    private String identificadorLocomotiva;

    @Column(name = "operadora_ferroviaria", nullable = false, length = 80)
    private String operadoraFerroviaria;

    @Column(name = "fabricante", length = 80)
    private String fabricante;

    @Column(name = "modelo", length = 80)
    private String modelo;

    @Column(name = "numero_serie", length = 80)
    private String numeroSerie;

    @Column(name = "peso_toneladas", nullable = false, precision = 12, scale = 3)
    private BigDecimal pesoToneladas;

    @Column(name = "comprimento_metros", nullable = false, precision = 10, scale = 3)
    private BigDecimal comprimentoMetros;

    @Column(name = "largura_metros", nullable = false, precision = 10, scale = 3)
    private BigDecimal larguraMetros;

    @Column(name = "altura_metros", nullable = false, precision = 10, scale = 3)
    private BigDecimal alturaMetros;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private StatusTransferenciaLocomotiva status = StatusTransferenciaLocomotiva.AGUARDANDO_ENTREGA;

    @Column(name = "nome_maquinista", length = 120)
    private String nomeMaquinista;

    @Column(name = "documento_entrega", length = 80)
    private String documentoEntrega;

    @Column(name = "responsavel_terminal", length = 120)
    private String responsavelTerminal;

    @Column(name = "entregue_em")
    private LocalDateTime entregueEm;

    @Column(name = "visita_navio_id")
    private Long visitaNavioId;

    @Column(name = "codigo_visita_navio", length = 60)
    private String codigoVisitaNavio;

    @Enumerated(EnumType.STRING)
    @Column(name = "modalidade_embarque", length = 40)
    private ModalidadeEmbarqueLocomotiva modalidadeEmbarque;

    @Column(name = "deck_planejado", length = 80)
    private String deckPlanejado;

    @Column(name = "posicao_planejada", length = 120)
    private String posicaoPlanejada;

    @Column(name = "freio_estacionamento_aplicado", nullable = false)
    private boolean freioEstacionamentoAplicado;

    @Column(name = "baterias_isoladas", nullable = false)
    private boolean bateriasIsoladas;

    @Column(name = "combustivel_protegido", nullable = false)
    private boolean combustivelProtegido;

    @Column(name = "calcos_instalados", nullable = false)
    private boolean calcosInstalados;

    @Column(name = "plano_amarracao_aprovado", nullable = false)
    private boolean planoAmarracaoAprovado;

    @Column(name = "liberada_em")
    private LocalDateTime liberadaEm;

    @Column(name = "embarcada_em")
    private LocalDateTime embarcadaEm;

    @Column(name = "posicao_real", length = 120)
    private String posicaoReal;

    @Column(length = 1000)
    private String observacoes;

    @Version
    @Column(nullable = false)
    private Long versao;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @PrePersist
    public void aoCriar() {
        LocalDateTime agora = LocalDateTime.now();
        criadoEm = agora;
        atualizadoEm = agora;
        if (status == null) {
            status = StatusTransferenciaLocomotiva.AGUARDANDO_ENTREGA;
        }
    }

    @PreUpdate
    public void aoAtualizar() {
        atualizadoEm = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public VisitaTrem getVisitaTrem() { return visitaTrem; }
    public void setVisitaTrem(VisitaTrem visitaTrem) { this.visitaTrem = visitaTrem; }
    public String getIdentificadorLocomotiva() { return identificadorLocomotiva; }
    public void setIdentificadorLocomotiva(String identificadorLocomotiva) { this.identificadorLocomotiva = identificadorLocomotiva; }
    public String getOperadoraFerroviaria() { return operadoraFerroviaria; }
    public void setOperadoraFerroviaria(String operadoraFerroviaria) { this.operadoraFerroviaria = operadoraFerroviaria; }
    public String getFabricante() { return fabricante; }
    public void setFabricante(String fabricante) { this.fabricante = fabricante; }
    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }
    public String getNumeroSerie() { return numeroSerie; }
    public void setNumeroSerie(String numeroSerie) { this.numeroSerie = numeroSerie; }
    public BigDecimal getPesoToneladas() { return pesoToneladas; }
    public void setPesoToneladas(BigDecimal pesoToneladas) { this.pesoToneladas = pesoToneladas; }
    public BigDecimal getComprimentoMetros() { return comprimentoMetros; }
    public void setComprimentoMetros(BigDecimal comprimentoMetros) { this.comprimentoMetros = comprimentoMetros; }
    public BigDecimal getLarguraMetros() { return larguraMetros; }
    public void setLarguraMetros(BigDecimal larguraMetros) { this.larguraMetros = larguraMetros; }
    public BigDecimal getAlturaMetros() { return alturaMetros; }
    public void setAlturaMetros(BigDecimal alturaMetros) { this.alturaMetros = alturaMetros; }
    public StatusTransferenciaLocomotiva getStatus() { return status; }
    public void setStatus(StatusTransferenciaLocomotiva status) { this.status = status; }
    public String getNomeMaquinista() { return nomeMaquinista; }
    public void setNomeMaquinista(String nomeMaquinista) { this.nomeMaquinista = nomeMaquinista; }
    public String getDocumentoEntrega() { return documentoEntrega; }
    public void setDocumentoEntrega(String documentoEntrega) { this.documentoEntrega = documentoEntrega; }
    public String getResponsavelTerminal() { return responsavelTerminal; }
    public void setResponsavelTerminal(String responsavelTerminal) { this.responsavelTerminal = responsavelTerminal; }
    public LocalDateTime getEntregueEm() { return entregueEm; }
    public void setEntregueEm(LocalDateTime entregueEm) { this.entregueEm = entregueEm; }
    public Long getVisitaNavioId() { return visitaNavioId; }
    public void setVisitaNavioId(Long visitaNavioId) { this.visitaNavioId = visitaNavioId; }
    public String getCodigoVisitaNavio() { return codigoVisitaNavio; }
    public void setCodigoVisitaNavio(String codigoVisitaNavio) { this.codigoVisitaNavio = codigoVisitaNavio; }
    public ModalidadeEmbarqueLocomotiva getModalidadeEmbarque() { return modalidadeEmbarque; }
    public void setModalidadeEmbarque(ModalidadeEmbarqueLocomotiva modalidadeEmbarque) { this.modalidadeEmbarque = modalidadeEmbarque; }
    public String getDeckPlanejado() { return deckPlanejado; }
    public void setDeckPlanejado(String deckPlanejado) { this.deckPlanejado = deckPlanejado; }
    public String getPosicaoPlanejada() { return posicaoPlanejada; }
    public void setPosicaoPlanejada(String posicaoPlanejada) { this.posicaoPlanejada = posicaoPlanejada; }
    public boolean isFreioEstacionamentoAplicado() { return freioEstacionamentoAplicado; }
    public void setFreioEstacionamentoAplicado(boolean freioEstacionamentoAplicado) { this.freioEstacionamentoAplicado = freioEstacionamentoAplicado; }
    public boolean isBateriasIsoladas() { return bateriasIsoladas; }
    public void setBateriasIsoladas(boolean bateriasIsoladas) { this.bateriasIsoladas = bateriasIsoladas; }
    public boolean isCombustivelProtegido() { return combustivelProtegido; }
    public void setCombustivelProtegido(boolean combustivelProtegido) { this.combustivelProtegido = combustivelProtegido; }
    public boolean isCalcosInstalados() { return calcosInstalados; }
    public void setCalcosInstalados(boolean calcosInstalados) { this.calcosInstalados = calcosInstalados; }
    public boolean isPlanoAmarracaoAprovado() { return planoAmarracaoAprovado; }
    public void setPlanoAmarracaoAprovado(boolean planoAmarracaoAprovado) { this.planoAmarracaoAprovado = planoAmarracaoAprovado; }
    public LocalDateTime getLiberadaEm() { return liberadaEm; }
    public void setLiberadaEm(LocalDateTime liberadaEm) { this.liberadaEm = liberadaEm; }
    public LocalDateTime getEmbarcadaEm() { return embarcadaEm; }
    public void setEmbarcadaEm(LocalDateTime embarcadaEm) { this.embarcadaEm = embarcadaEm; }
    public String getPosicaoReal() { return posicaoReal; }
    public void setPosicaoReal(String posicaoReal) { this.posicaoReal = posicaoReal; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
    public Long getVersao() { return versao; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
}
