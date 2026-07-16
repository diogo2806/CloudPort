package br.com.cloudport.servicoyard.patio.listatrabalho.dto;

import java.util.ArrayList;
import java.util.List;

public class ResultadoAplicacaoPlanoOtimizadoPatioDto {

    private String planoId;
    private Long visitaNavioId;
    private int ordensAtualizadas;
    private List<EstadoAnteriorOrdemDto> estadosAnteriores = new ArrayList<>();

    public String getPlanoId() {
        return planoId;
    }

    public void setPlanoId(String planoId) {
        this.planoId = planoId;
    }

    public Long getVisitaNavioId() {
        return visitaNavioId;
    }

    public void setVisitaNavioId(Long visitaNavioId) {
        this.visitaNavioId = visitaNavioId;
    }

    public int getOrdensAtualizadas() {
        return ordensAtualizadas;
    }

    public void setOrdensAtualizadas(int ordensAtualizadas) {
        this.ordensAtualizadas = ordensAtualizadas;
    }

    public List<EstadoAnteriorOrdemDto> getEstadosAnteriores() {
        return estadosAnteriores;
    }

    public void setEstadosAnteriores(List<EstadoAnteriorOrdemDto> estadosAnteriores) {
        this.estadosAnteriores = estadosAnteriores;
    }

    public static class EstadoAnteriorOrdemDto {

        private Long ordemTrabalhoPatioId;
        private String destino;
        private Integer linhaDestino;
        private Integer colunaDestino;
        private String camadaDestino;
        private Integer prioridadeOperacional;
        private Integer sequenciaNavio;
        private Long workQueueId;

        public Long getOrdemTrabalhoPatioId() {
            return ordemTrabalhoPatioId;
        }

        public void setOrdemTrabalhoPatioId(Long ordemTrabalhoPatioId) {
            this.ordemTrabalhoPatioId = ordemTrabalhoPatioId;
        }

        public String getDestino() {
            return destino;
        }

        public void setDestino(String destino) {
            this.destino = destino;
        }

        public Integer getLinhaDestino() {
            return linhaDestino;
        }

        public void setLinhaDestino(Integer linhaDestino) {
            this.linhaDestino = linhaDestino;
        }

        public Integer getColunaDestino() {
            return colunaDestino;
        }

        public void setColunaDestino(Integer colunaDestino) {
            this.colunaDestino = colunaDestino;
        }

        public String getCamadaDestino() {
            return camadaDestino;
        }

        public void setCamadaDestino(String camadaDestino) {
            this.camadaDestino = camadaDestino;
        }

        public Integer getPrioridadeOperacional() {
            return prioridadeOperacional;
        }

        public void setPrioridadeOperacional(Integer prioridadeOperacional) {
            this.prioridadeOperacional = prioridadeOperacional;
        }

        public Integer getSequenciaNavio() {
            return sequenciaNavio;
        }

        public void setSequenciaNavio(Integer sequenciaNavio) {
            this.sequenciaNavio = sequenciaNavio;
        }

        public Long getWorkQueueId() {
            return workQueueId;
        }

        public void setWorkQueueId(Long workQueueId) {
            this.workQueueId = workQueueId;
        }
    }
}
