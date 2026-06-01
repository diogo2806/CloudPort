package br.com.cloudport.visibilidade.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ConteinerRastreamentoDTO {

    private String containerId;
    private String statusAtual;
    private LocalizacaoDTO localizacaoAtual;
    private ProximoDestinoDTO proximoDestino;
    private List<RotaDTO> rotaCompleta;
    private MetricasDTO metricas;

    // Getters and Setters
    public String getContainerId() { return containerId; }
    public void setContainerId(String containerId) { this.containerId = containerId; }

    public String getStatusAtual() { return statusAtual; }
    public void setStatusAtual(String statusAtual) { this.statusAtual = statusAtual; }

    public LocalizacaoDTO getLocalizacaoAtual() { return localizacaoAtual; }
    public void setLocalizacaoAtual(LocalizacaoDTO localizacaoAtual) { this.localizacaoAtual = localizacaoAtual; }

    public ProximoDestinoDTO getProximoDestino() { return proximoDestino; }
    public void setProximoDestino(ProximoDestinoDTO proximoDestino) { this.proximoDestino = proximoDestino; }

    public List<RotaDTO> getRotaCompleta() { return rotaCompleta; }
    public void setRotaCompleta(List<RotaDTO> rotaCompleta) { this.rotaCompleta = rotaCompleta; }

    public MetricasDTO getMetricas() { return metricas; }
    public void setMetricas(MetricasDTO metricas) { this.metricas = metricas; }

    public static class LocalizacaoDTO {
        private String tipo;
        private String zona;
        private String posicao;
        private CoordenadasDTO coordenadas;
        private LocalDateTime dataAtualizacao;

        // Getters and Setters
        public String getTipo() { return tipo; }
        public void setTipo(String tipo) { this.tipo = tipo; }
        public String getZona() { return zona; }
        public void setZona(String zona) { this.zona = zona; }
        public String getPosicao() { return posicao; }
        public void setPosicao(String posicao) { this.posicao = posicao; }
        public CoordenadasDTO getCoordenadas() { return coordenadas; }
        public void setCoordenadas(CoordenadasDTO coordenadas) { this.coordenadas = coordenadas; }
        public LocalDateTime getDataAtualizacao() { return dataAtualizacao; }
        public void setDataAtualizacao(LocalDateTime dataAtualizacao) { this.dataAtualizacao = dataAtualizacao; }
    }

    public static class CoordenadasDTO {
        private Double latitude;
        private Double longitude;

        public Double getLatitude() { return latitude; }
        public void setLatitude(Double latitude) { this.latitude = latitude; }
        public Double getLongitude() { return longitude; }
        public void setLongitude(Double longitude) { this.longitude = longitude; }
    }

    public static class ProximoDestinoDTO {
        private String tipo;
        private String id;
        private String berco;
        private LocalDateTime estimadoParaida;

        public String getTipo() { return tipo; }
        public void setTipo(String tipo) { this.tipo = tipo; }
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getBerco() { return berco; }
        public void setBerco(String berco) { this.berco = berco; }
        public LocalDateTime getEstimadoParaida() { return estimadoParaida; }
        public void setEstimadoParaida(LocalDateTime estimadoParaida) { this.estimadoParaida = estimadoParaida; }
    }

    public static class RotaDTO {
        private Integer sequencia;
        private String local;
        private LocalDateTime timestamp;
        private String status;

        public Integer getSequencia() { return sequencia; }
        public void setSequencia(Integer sequencia) { this.sequencia = sequencia; }
        public String getLocal() { return local; }
        public void setLocal(String local) { this.local = local; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    public static class MetricasDTO {
        private String tempoNoYard;
        private LocalDateTime dataPrevisaoSaida;

        public String getTempoNoYard() { return tempoNoYard; }
        public void setTempoNoYard(String tempoNoYard) { this.tempoNoYard = tempoNoYard; }
        public LocalDateTime getDataPrevisaoSaida() { return dataPrevisaoSaida; }
        public void setDataPrevisaoSaida(LocalDateTime dataPrevisaoSaida) { this.dataPrevisaoSaida = dataPrevisaoSaida; }
    }
}