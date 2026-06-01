package br.com.cloudport.visibilidade.dto;

import java.time.LocalDateTime;
import java.util.List;

public class StatusNavioDTO {

    private String navioId;
    private String nomeNavio;
    private String statusOperacional;
    private EtaDTO etaCurrent;
    private BercoDTO bercoAlocado;
    private OperacoesDTO operacoesEmAndamento;
    private List<EquipamentoDTO> equipamentosAlocados;
    private List<AlertaResumoDTO> alertasNavio;
    private List<TimelineDTO> timeline;

    // Getters and Setters
    public String getNavioId() { return navioId; }
    public void setNavioId(String navioId) { this.navioId = navioId; }

    public String getNomeNavio() { return nomeNavio; }
    public void setNomeNavio(String nomeNavio) { this.nomeNavio = nomeNavio; }

    public String getStatusOperacional() { return statusOperacional; }
    public void setStatusOperacional(String statusOperacional) { this.statusOperacional = statusOperacional; }

    public EtaDTO getEtaCurrent() { return etaCurrent; }
    public void setEtaCurrent(EtaDTO etaCurrent) { this.etaCurrent = etaCurrent; }

    public BercoDTO getBercoAlocado() { return bercoAlocado; }
    public void setBercoAlocado(BercoDTO bercoAlocado) { this.bercoAlocado = bercoAlocado; }

    public OperacoesDTO getOperacoesEmAndamento() { return operacoesEmAndamento; }
    public void setOperacoesEmAndamento(OperacoesDTO operacoesEmAndamento) { this.operacoesEmAndamento = operacoesEmAndamento; }

    public List<EquipamentoDTO> getEquipamentosAlocados() { return equipamentosAlocados; }
    public void setEquipamentosAlocados(List<EquipamentoDTO> equipamentosAlocados) { this.equipamentosAlocados = equipamentosAlocados; }

    public List<AlertaResumoDTO> getAlertasNavio() { return alertasNavio; }
    public void setAlertasNavio(List<AlertaResumoDTO> alertasNavio) { this.alertasNavio = alertasNavio; }

    public List<TimelineDTO> getTimeline() { return timeline; }
    public void setTimeline(List<TimelineDTO> timeline) { this.timeline = timeline; }

    public static class EtaDTO {
        private LocalDateTime estimado;
        private LocalDateTime chegadaReal;
        private Integer atraso;

        public LocalDateTime getEstimado() { return estimado; }
        public void setEstimado(LocalDateTime estimado) { this.estimado = estimado; }
        public LocalDateTime getChegadaReal() { return chegadaReal; }
        public void setChegadaReal(LocalDateTime chegadaReal) { this.chegadaReal = chegadaReal; }
        public Integer getAtraso() { return atraso; }
        public void setAtraso(Integer atraso) { this.atraso = atraso; }
    }

    public static class BercoDTO {
        private String numero;
        private LocalDateTime dataInicio;
        private LocalDateTime dataPrevistaSaida;

        public String getNumero() { return numero; }
        public void setNumero(String numero) { this.numero = numero; }
        public LocalDateTime getDataInicio() { return dataInicio; }
        public void setDataInicio(LocalDateTime dataInicio) { this.dataInicio = dataInicio; }
        public LocalDateTime getDataPrevistaSaida() { return dataPrevistaSaida; }
        public void setDataPrevistaSaida(LocalDateTime dataPrevistaSaida) { this.dataPrevistaSaida = dataPrevistaSaida; }
    }

    public static class OperacoesDTO {
        private Integer conteineresADescarregar;
        private Integer conteineresDescarregados;
        private Double porcentagemCompleta;
        private Double velocidadeMov;

        public Integer getConteineresADescarregar() { return conteineresADescarregar; }
        public void setConteineresADescarregar(Integer conteineresADescarregar) { this.conteineresADescarregar = conteineresADescarregar; }
        public Integer getConteineresDescarregados() { return conteineresDescarregados; }
        public void setConteineresDescarregados(Integer conteineresDescarregados) { this.conteineresDescarregados = conteineresDescarregados; }
        public Double getPorcentagemCompleta() { return porcentagemCompleta; }
        public void setPorcentagemCompleta(Double porcentagemCompleta) { this.porcentagemCompleta = porcentagemCompleta; }
        public Double getVelocidadeMov() { return velocidadeMov; }
        public void setVelocidadeMov(Double velocidadeMov) { this.velocidadeMov = velocidadeMov; }
    }

    public static class EquipamentoDTO {
        private String id;
        private String status;
        private Double eficiencia;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Double getEficiencia() { return eficiencia; }
        public void setEficiencia(Double eficiencia) { this.eficiencia = eficiencia; }
    }

    public static class AlertaResumoDTO {
        private Long id;
        private String tipo;
        private String severidade;
        private String descricao;
        private LocalDateTime dataGerada;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getTipo() { return tipo; }
        public void setTipo(String tipo) { this.tipo = tipo; }
        public String getSeveridade() { return severidade; }
        public void setSeveridade(String severidade) { this.severidade = severidade; }
        public String getDescricao() { return descricao; }
        public void setDescricao(String descricao) { this.descricao = descricao; }
        public LocalDateTime getDataGerada() { return dataGerada; }
        public void setDataGerada(LocalDateTime dataGerada) { this.dataGerada = dataGerada; }
    }

    public static class TimelineDTO {
        private String evento;
        private LocalDateTime tempo;

        public String getEvento() { return evento; }
        public void setEvento(String evento) { this.evento = evento; }
        public LocalDateTime getTempo() { return tempo; }
        public void setTempo(LocalDateTime tempo) { this.tempo = tempo; }
    }
}