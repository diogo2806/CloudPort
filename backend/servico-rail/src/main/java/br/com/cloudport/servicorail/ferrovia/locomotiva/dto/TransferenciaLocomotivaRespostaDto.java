package br.com.cloudport.servicorail.ferrovia.locomotiva.dto;

import br.com.cloudport.servicorail.ferrovia.locomotiva.modelo.ModalidadeEmbarqueLocomotiva;
import br.com.cloudport.servicorail.ferrovia.locomotiva.modelo.StatusTransferenciaLocomotiva;
import br.com.cloudport.servicorail.ferrovia.locomotiva.modelo.TransferenciaLocomotiva;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransferenciaLocomotivaRespostaDto {

    private Long id;
    private Long visitaTremId;
    private String identificadorTrem;
    private String identificadorLocomotiva;
    private String operadoraFerroviaria;
    private String fabricante;
    private String modelo;
    private String numeroSerie;
    private BigDecimal pesoToneladas;
    private BigDecimal comprimentoMetros;
    private BigDecimal larguraMetros;
    private BigDecimal alturaMetros;
    private StatusTransferenciaLocomotiva status;
    private String nomeMaquinista;
    private String documentoEntrega;
    private String responsavelTerminal;
    private LocalDateTime entregueEm;
    private Long visitaNavioId;
    private String codigoVisitaNavio;
    private ModalidadeEmbarqueLocomotiva modalidadeEmbarque;
    private String deckPlanejado;
    private String posicaoPlanejada;
    private boolean freioEstacionamentoAplicado;
    private boolean bateriasIsoladas;
    private boolean combustivelProtegido;
    private boolean calcosInstalados;
    private boolean planoAmarracaoAprovado;
    private LocalDateTime liberadaEm;
    private LocalDateTime embarcadaEm;
    private String posicaoReal;
    private String observacoes;
    private Long versao;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

    public static TransferenciaLocomotivaRespostaDto deEntidade(TransferenciaLocomotiva entidade) {
        TransferenciaLocomotivaRespostaDto dto = new TransferenciaLocomotivaRespostaDto();
        dto.id = entidade.getId();
        dto.visitaTremId = entidade.getVisitaTrem().getId();
        dto.identificadorTrem = entidade.getVisitaTrem().getIdentificadorTrem();
        dto.identificadorLocomotiva = entidade.getIdentificadorLocomotiva();
        dto.operadoraFerroviaria = entidade.getOperadoraFerroviaria();
        dto.fabricante = entidade.getFabricante();
        dto.modelo = entidade.getModelo();
        dto.numeroSerie = entidade.getNumeroSerie();
        dto.pesoToneladas = entidade.getPesoToneladas();
        dto.comprimentoMetros = entidade.getComprimentoMetros();
        dto.larguraMetros = entidade.getLarguraMetros();
        dto.alturaMetros = entidade.getAlturaMetros();
        dto.status = entidade.getStatus();
        dto.nomeMaquinista = entidade.getNomeMaquinista();
        dto.documentoEntrega = entidade.getDocumentoEntrega();
        dto.responsavelTerminal = entidade.getResponsavelTerminal();
        dto.entregueEm = entidade.getEntregueEm();
        dto.visitaNavioId = entidade.getVisitaNavioId();
        dto.codigoVisitaNavio = entidade.getCodigoVisitaNavio();
        dto.modalidadeEmbarque = entidade.getModalidadeEmbarque();
        dto.deckPlanejado = entidade.getDeckPlanejado();
        dto.posicaoPlanejada = entidade.getPosicaoPlanejada();
        dto.freioEstacionamentoAplicado = entidade.isFreioEstacionamentoAplicado();
        dto.bateriasIsoladas = entidade.isBateriasIsoladas();
        dto.combustivelProtegido = entidade.isCombustivelProtegido();
        dto.calcosInstalados = entidade.isCalcosInstalados();
        dto.planoAmarracaoAprovado = entidade.isPlanoAmarracaoAprovado();
        dto.liberadaEm = entidade.getLiberadaEm();
        dto.embarcadaEm = entidade.getEmbarcadaEm();
        dto.posicaoReal = entidade.getPosicaoReal();
        dto.observacoes = entidade.getObservacoes();
        dto.versao = entidade.getVersao();
        dto.criadoEm = entidade.getCriadoEm();
        dto.atualizadoEm = entidade.getAtualizadoEm();
        return dto;
    }

    public Long getId() { return id; }
    public Long getVisitaTremId() { return visitaTremId; }
    public String getIdentificadorTrem() { return identificadorTrem; }
    public String getIdentificadorLocomotiva() { return identificadorLocomotiva; }
    public String getOperadoraFerroviaria() { return operadoraFerroviaria; }
    public String getFabricante() { return fabricante; }
    public String getModelo() { return modelo; }
    public String getNumeroSerie() { return numeroSerie; }
    public BigDecimal getPesoToneladas() { return pesoToneladas; }
    public BigDecimal getComprimentoMetros() { return comprimentoMetros; }
    public BigDecimal getLarguraMetros() { return larguraMetros; }
    public BigDecimal getAlturaMetros() { return alturaMetros; }
    public StatusTransferenciaLocomotiva getStatus() { return status; }
    public String getNomeMaquinista() { return nomeMaquinista; }
    public String getDocumentoEntrega() { return documentoEntrega; }
    public String getResponsavelTerminal() { return responsavelTerminal; }
    public LocalDateTime getEntregueEm() { return entregueEm; }
    public Long getVisitaNavioId() { return visitaNavioId; }
    public String getCodigoVisitaNavio() { return codigoVisitaNavio; }
    public ModalidadeEmbarqueLocomotiva getModalidadeEmbarque() { return modalidadeEmbarque; }
    public String getDeckPlanejado() { return deckPlanejado; }
    public String getPosicaoPlanejada() { return posicaoPlanejada; }
    public boolean isFreioEstacionamentoAplicado() { return freioEstacionamentoAplicado; }
    public boolean isBateriasIsoladas() { return bateriasIsoladas; }
    public boolean isCombustivelProtegido() { return combustivelProtegido; }
    public boolean isCalcosInstalados() { return calcosInstalados; }
    public boolean isPlanoAmarracaoAprovado() { return planoAmarracaoAprovado; }
    public LocalDateTime getLiberadaEm() { return liberadaEm; }
    public LocalDateTime getEmbarcadaEm() { return embarcadaEm; }
    public String getPosicaoReal() { return posicaoReal; }
    public String getObservacoes() { return observacoes; }
    public Long getVersao() { return versao; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
}
