package br.com.cloudport.serviconaviosiderurgico.cliente;

import br.com.cloudport.serviconaviosiderurgico.dominio.ItemOperacaoNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.ReservaPosicaoPatioNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.TipoMovimentoNavio;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.util.StringUtils;

/**
 * Porta de integração com o módulo Yard.
 *
 * <p>No runtime monolítico a implementação é local. O adaptador HTTP é mantido
 * apenas para execução isolada e rollback durante a migração.</p>
 */
public interface OrdemPatioYardCliente {

    OrdemPatioYardRespostaDTO criarOuReutilizarOrdem(ItemOperacaoNavio item,
                                                       ReservaPosicaoPatioNavio reserva);

    List<OrdemPatioYardRespostaDTO> listarOrdensDaVisita(Long visitaNavioId);

    List<FilaOrdemPatioYardDTO> listarFilasDaVisita(Long visitaNavioId);

    List<WorkQueuePatioYardDTO> listarWorkQueuesDaVisita(Long visitaNavioId);

    List<OrdemPatioYardRespostaDTO> listarOrdensSemCobertura(Long visitaNavioId);

    OrdemPatioYardRespostaDTO atualizarPrioridade(Long ordemId,
                                                   Integer prioridadeOperacional,
                                                   Boolean prioridadeBusca);

    OrdemPatioYardRespostaDTO suspender(Long ordemId);

    OrdemPatioYardRespostaDTO retomar(Long ordemId);

    class OrdemPatioYardRequisicaoDTO {
        private String codigoConteiner;
        private String tipoCarga;
        private String destino;
        private Integer linhaDestino;
        private Integer colunaDestino;
        private String camadaDestino;
        private String tipoMovimento;
        private String statusConteinerDestino;
        private Long visitaNavioId;
        private Long itemOperacaoNavioId;
        private Long planoEstivaNavioId;
        private String tipoOrigem;
        private String tipoDestino;
        private Integer sequenciaNavio;
        private Integer prioridadeOperacional;

        public static OrdemPatioYardRequisicaoDTO de(ItemOperacaoNavio item,
                                                      ReservaPosicaoPatioNavio reserva) {
            if (reserva == null || reserva.getLinha() == null || reserva.getColuna() == null
                    || !StringUtils.hasText(reserva.getCamada())) {
                throw new IllegalArgumentException(
                        "A ordem real do yard exige reserva ativa com linha, coluna e camada.");
            }
            OrdemPatioYardRequisicaoDTO dto = new OrdemPatioYardRequisicaoDTO();
            dto.codigoConteiner = item.getCodigoLote();
            dto.tipoCarga = null;
            dto.destino = destino(item, reserva);
            dto.linhaDestino = reserva.getLinha();
            dto.colunaDestino = reserva.getColuna();
            dto.camadaDestino = reserva.getCamada();
            dto.tipoMovimento = tipoMovimentoPatio(item.getTipoMovimento());
            dto.statusConteinerDestino = statusDestino(item.getTipoMovimento());
            dto.visitaNavioId = item.getVisitaNavio().getId();
            dto.itemOperacaoNavioId = item.getId();
            dto.planoEstivaNavioId = null;
            dto.tipoOrigem = tipoOrigem(item.getTipoMovimento());
            dto.tipoDestino = tipoDestino(item.getTipoMovimento());
            dto.sequenciaNavio = item.getSequenciaOperacional();
            dto.prioridadeOperacional = item.getSequenciaOperacional();
            return dto;
        }

        private static String destino(ItemOperacaoNavio item, ReservaPosicaoPatioNavio reserva) {
            if (StringUtils.hasText(item.getDestinoPatio())) {
                return item.getDestinoPatio();
            }
            if (StringUtils.hasText(reserva.getBloco())) {
                return reserva.getBloco().toUpperCase(Locale.ROOT);
            }
            return reserva.getPosicaoPatioId();
        }

        private static String tipoMovimentoPatio(TipoMovimentoNavio tipoMovimento) {
            if (tipoMovimento == TipoMovimentoNavio.RESTOW) {
                return "REMANEJAMENTO";
            }
            if (tipoMovimento == TipoMovimentoNavio.EMBARQUE) {
                return "TRANSFERENCIA";
            }
            return "ALOCACAO";
        }

        private static String statusDestino(TipoMovimentoNavio tipoMovimento) {
            return tipoMovimento == TipoMovimentoNavio.EMBARQUE ? "DESPACHADO" : "ARMAZENADO";
        }

        private static String tipoOrigem(TipoMovimentoNavio tipoMovimento) {
            return tipoMovimento == TipoMovimentoNavio.DESCARGA ? "NAVIO" : "PATIO";
        }

        private static String tipoDestino(TipoMovimentoNavio tipoMovimento) {
            return tipoMovimento == TipoMovimentoNavio.EMBARQUE ? "NAVIO" : "PATIO";
        }

        public String getCodigoConteiner() { return codigoConteiner; }
        public void setCodigoConteiner(String codigoConteiner) { this.codigoConteiner = codigoConteiner; }
        public String getTipoCarga() { return tipoCarga; }
        public void setTipoCarga(String tipoCarga) { this.tipoCarga = tipoCarga; }
        public String getDestino() { return destino; }
        public void setDestino(String destino) { this.destino = destino; }
        public Integer getLinhaDestino() { return linhaDestino; }
        public void setLinhaDestino(Integer linhaDestino) { this.linhaDestino = linhaDestino; }
        public Integer getColunaDestino() { return colunaDestino; }
        public void setColunaDestino(Integer colunaDestino) { this.colunaDestino = colunaDestino; }
        public String getCamadaDestino() { return camadaDestino; }
        public void setCamadaDestino(String camadaDestino) { this.camadaDestino = camadaDestino; }
        public String getTipoMovimento() { return tipoMovimento; }
        public void setTipoMovimento(String tipoMovimento) { this.tipoMovimento = tipoMovimento; }
        public String getStatusConteinerDestino() { return statusConteinerDestino; }
        public void setStatusConteinerDestino(String statusConteinerDestino) { this.statusConteinerDestino = statusConteinerDestino; }
        public Long getVisitaNavioId() { return visitaNavioId; }
        public void setVisitaNavioId(Long visitaNavioId) { this.visitaNavioId = visitaNavioId; }
        public Long getItemOperacaoNavioId() { return itemOperacaoNavioId; }
        public void setItemOperacaoNavioId(Long itemOperacaoNavioId) { this.itemOperacaoNavioId = itemOperacaoNavioId; }
        public Long getPlanoEstivaNavioId() { return planoEstivaNavioId; }
        public void setPlanoEstivaNavioId(Long planoEstivaNavioId) { this.planoEstivaNavioId = planoEstivaNavioId; }
        public String getTipoOrigem() { return tipoOrigem; }
        public void setTipoOrigem(String tipoOrigem) { this.tipoOrigem = tipoOrigem; }
        public String getTipoDestino() { return tipoDestino; }
        public void setTipoDestino(String tipoDestino) { this.tipoDestino = tipoDestino; }
        public Integer getSequenciaNavio() { return sequenciaNavio; }
        public void setSequenciaNavio(Integer sequenciaNavio) { this.sequenciaNavio = sequenciaNavio; }
        public Integer getPrioridadeOperacional() { return prioridadeOperacional; }
        public void setPrioridadeOperacional(Integer prioridadeOperacional) { this.prioridadeOperacional = prioridadeOperacional; }
    }

    class AtualizacaoPrioridadeOrdemPatioYardDTO {
        private Integer prioridadeOperacional;
        private Boolean prioridadeBusca;

        public AtualizacaoPrioridadeOrdemPatioYardDTO() {
        }

        public AtualizacaoPrioridadeOrdemPatioYardDTO(Integer prioridadeOperacional,
                                                       Boolean prioridadeBusca) {
            this.prioridadeOperacional = prioridadeOperacional;
            this.prioridadeBusca = prioridadeBusca;
        }

        public Integer getPrioridadeOperacional() { return prioridadeOperacional; }
        public void setPrioridadeOperacional(Integer prioridadeOperacional) { this.prioridadeOperacional = prioridadeOperacional; }
        public Boolean getPrioridadeBusca() { return prioridadeBusca; }
        public void setPrioridadeBusca(Boolean prioridadeBusca) { this.prioridadeBusca = prioridadeBusca; }
    }

    class OrdemPatioYardRespostaDTO {
        private Long id;
        private String codigoConteiner;
        private String destino;
        private Integer linhaDestino;
        private Integer colunaDestino;
        private String camadaDestino;
        private String tipoMovimento;
        private String statusOrdem;
        private Long visitaNavioId;
        private Long itemOperacaoNavioId;
        private Integer sequenciaNavio;
        private Integer prioridadeOperacional;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getCodigoConteiner() { return codigoConteiner; }
        public void setCodigoConteiner(String codigoConteiner) { this.codigoConteiner = codigoConteiner; }
        public String getDestino() { return destino; }
        public void setDestino(String destino) { this.destino = destino; }
        public Integer getLinhaDestino() { return linhaDestino; }
        public void setLinhaDestino(Integer linhaDestino) { this.linhaDestino = linhaDestino; }
        public Integer getColunaDestino() { return colunaDestino; }
        public void setColunaDestino(Integer colunaDestino) { this.colunaDestino = colunaDestino; }
        public String getCamadaDestino() { return camadaDestino; }
        public void setCamadaDestino(String camadaDestino) { this.camadaDestino = camadaDestino; }
        public String getTipoMovimento() { return tipoMovimento; }
        public void setTipoMovimento(String tipoMovimento) { this.tipoMovimento = tipoMovimento; }
        public String getStatusOrdem() { return statusOrdem; }
        public void setStatusOrdem(String statusOrdem) { this.statusOrdem = statusOrdem; }
        public Long getVisitaNavioId() { return visitaNavioId; }
        public void setVisitaNavioId(Long visitaNavioId) { this.visitaNavioId = visitaNavioId; }
        public Long getItemOperacaoNavioId() { return itemOperacaoNavioId; }
        public void setItemOperacaoNavioId(Long itemOperacaoNavioId) { this.itemOperacaoNavioId = itemOperacaoNavioId; }
        public Integer getSequenciaNavio() { return sequenciaNavio; }
        public void setSequenciaNavio(Integer sequenciaNavio) { this.sequenciaNavio = sequenciaNavio; }
        public Integer getPrioridadeOperacional() { return prioridadeOperacional; }
        public void setPrioridadeOperacional(Integer prioridadeOperacional) { this.prioridadeOperacional = prioridadeOperacional; }

        public String posicaoDestinoFormatada() {
            if (linhaDestino == null || colunaDestino == null || !StringUtils.hasText(camadaDestino)) {
                return destino;
            }
            return linhaDestino + "-" + colunaDestino + "-" + camadaDestino;
        }
    }

    class FilaOrdemPatioYardDTO {
        private String identificador;
        private String agrupamento;
        private Long visitaNavioId;
        private String berco;
        private String blocoZona;
        private Integer sequenciaInicial;
        private String status;
        private long totalOrdens;
        private List<OrdemPatioYardRespostaDTO> ordens;

        public String getIdentificador() { return identificador; }
        public void setIdentificador(String identificador) { this.identificador = identificador; }
        public String getAgrupamento() { return agrupamento; }
        public void setAgrupamento(String agrupamento) { this.agrupamento = agrupamento; }
        public Long getVisitaNavioId() { return visitaNavioId; }
        public void setVisitaNavioId(Long visitaNavioId) { this.visitaNavioId = visitaNavioId; }
        public String getBerco() { return berco; }
        public void setBerco(String berco) { this.berco = berco; }
        public String getBlocoZona() { return blocoZona; }
        public void setBlocoZona(String blocoZona) { this.blocoZona = blocoZona; }
        public Integer getSequenciaInicial() { return sequenciaInicial; }
        public void setSequenciaInicial(Integer sequenciaInicial) { this.sequenciaInicial = sequenciaInicial; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public long getTotalOrdens() { return totalOrdens; }
        public void setTotalOrdens(long totalOrdens) { this.totalOrdens = totalOrdens; }
        public List<OrdemPatioYardRespostaDTO> getOrdens() { return ordens; }
        public void setOrdens(List<OrdemPatioYardRespostaDTO> ordens) { this.ordens = ordens; }
    }

    class WorkQueuePatioYardDTO {
        private Long id;
        private String identificador;
        private String agrupamento;
        private Long visitaNavioId;
        private String berco;
        private Integer porao;
        private String blocoZona;
        private Integer sequenciaInicial;
        private String pow;
        private String poolOperacional;
        private String equipamento;
        private String status;
        private Integer prioridadeOperacional;
        private int totalOrdens;
        private List<OrdemPatioYardRespostaDTO> jobList;
        private LocalDateTime criadoEm;
        private LocalDateTime atualizadoEm;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getIdentificador() { return identificador; }
        public void setIdentificador(String identificador) { this.identificador = identificador; }
        public String getAgrupamento() { return agrupamento; }
        public void setAgrupamento(String agrupamento) { this.agrupamento = agrupamento; }
        public Long getVisitaNavioId() { return visitaNavioId; }
        public void setVisitaNavioId(Long visitaNavioId) { this.visitaNavioId = visitaNavioId; }
        public String getBerco() { return berco; }
        public void setBerco(String berco) { this.berco = berco; }
        public Integer getPorao() { return porao; }
        public void setPorao(Integer porao) { this.porao = porao; }
        public String getBlocoZona() { return blocoZona; }
        public void setBlocoZona(String blocoZona) { this.blocoZona = blocoZona; }
        public Integer getSequenciaInicial() { return sequenciaInicial; }
        public void setSequenciaInicial(Integer sequenciaInicial) { this.sequenciaInicial = sequenciaInicial; }
        public String getPow() { return pow; }
        public void setPow(String pow) { this.pow = pow; }
        public String getPoolOperacional() { return poolOperacional; }
        public void setPoolOperacional(String poolOperacional) { this.poolOperacional = poolOperacional; }
        public String getEquipamento() { return equipamento; }
        public void setEquipamento(String equipamento) { this.equipamento = equipamento; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Integer getPrioridadeOperacional() { return prioridadeOperacional; }
        public void setPrioridadeOperacional(Integer prioridadeOperacional) { this.prioridadeOperacional = prioridadeOperacional; }
        public int getTotalOrdens() { return totalOrdens; }
        public void setTotalOrdens(int totalOrdens) { this.totalOrdens = totalOrdens; }
        public List<OrdemPatioYardRespostaDTO> getJobList() { return jobList; }
        public void setJobList(List<OrdemPatioYardRespostaDTO> jobList) { this.jobList = jobList; }
        public LocalDateTime getCriadoEm() { return criadoEm; }
        public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
        public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
        public void setAtualizadoEm(LocalDateTime atualizadoEm) { this.atualizadoEm = atualizadoEm; }
    }
}
