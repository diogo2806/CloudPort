package br.com.cloudport.servicoyard.scheduler.dto;

import br.com.cloudport.servicoyard.scheduler.modelo.EstadoPlanoPosicaoOperacional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class YardImpactRespostaDto {

    private LocalDateTime geradoEm;
    private LocalDateTime horizonteInicio;
    private LocalDateTime horizonteFim;
    private Integer horizonteHoras;
    private Integer totalEntradas;
    private Integer totalSaidas;
    private Integer totalRehandles;
    private Integer totalReservas;
    private Integer totalWorkInstructions;
    private Integer demandaChe;
    private Integer cheDisponiveis;
    private Integer deficitChe;
    private List<ImpactoBlocoDto> blocos = new ArrayList<>();
    private List<ImpactoPowDto> pows = new ArrayList<>();
    private List<UnidadeImpactoDto> unidades = new ArrayList<>();
    private List<String> alertas = new ArrayList<>();

    public LocalDateTime getGeradoEm() { return geradoEm; }
    public void setGeradoEm(LocalDateTime geradoEm) { this.geradoEm = geradoEm; }
    public LocalDateTime getHorizonteInicio() { return horizonteInicio; }
    public void setHorizonteInicio(LocalDateTime horizonteInicio) { this.horizonteInicio = horizonteInicio; }
    public LocalDateTime getHorizonteFim() { return horizonteFim; }
    public void setHorizonteFim(LocalDateTime horizonteFim) { this.horizonteFim = horizonteFim; }
    public Integer getHorizonteHoras() { return horizonteHoras; }
    public void setHorizonteHoras(Integer horizonteHoras) { this.horizonteHoras = horizonteHoras; }
    public Integer getTotalEntradas() { return totalEntradas; }
    public void setTotalEntradas(Integer totalEntradas) { this.totalEntradas = totalEntradas; }
    public Integer getTotalSaidas() { return totalSaidas; }
    public void setTotalSaidas(Integer totalSaidas) { this.totalSaidas = totalSaidas; }
    public Integer getTotalRehandles() { return totalRehandles; }
    public void setTotalRehandles(Integer totalRehandles) { this.totalRehandles = totalRehandles; }
    public Integer getTotalReservas() { return totalReservas; }
    public void setTotalReservas(Integer totalReservas) { this.totalReservas = totalReservas; }
    public Integer getTotalWorkInstructions() { return totalWorkInstructions; }
    public void setTotalWorkInstructions(Integer totalWorkInstructions) { this.totalWorkInstructions = totalWorkInstructions; }
    public Integer getDemandaChe() { return demandaChe; }
    public void setDemandaChe(Integer demandaChe) { this.demandaChe = demandaChe; }
    public Integer getCheDisponiveis() { return cheDisponiveis; }
    public void setCheDisponiveis(Integer cheDisponiveis) { this.cheDisponiveis = cheDisponiveis; }
    public Integer getDeficitChe() { return deficitChe; }
    public void setDeficitChe(Integer deficitChe) { this.deficitChe = deficitChe; }
    public List<ImpactoBlocoDto> getBlocos() { return blocos; }
    public void setBlocos(List<ImpactoBlocoDto> blocos) { this.blocos = blocos == null ? new ArrayList<>() : blocos; }
    public List<ImpactoPowDto> getPows() { return pows; }
    public void setPows(List<ImpactoPowDto> pows) { this.pows = pows == null ? new ArrayList<>() : pows; }
    public List<UnidadeImpactoDto> getUnidades() { return unidades; }
    public void setUnidades(List<UnidadeImpactoDto> unidades) { this.unidades = unidades == null ? new ArrayList<>() : unidades; }
    public List<String> getAlertas() { return alertas; }
    public void setAlertas(List<String> alertas) { this.alertas = alertas == null ? new ArrayList<>() : alertas; }

    public static class ImpactoBlocoDto {
        private String bloco;
        private Integer capacidadePosicoes;
        private Integer reservasAtivas;
        private Integer movimentosPrevistos;
        private Integer entradas;
        private Integer saidas;
        private Integer rehandles;
        private Double ocupacaoProjetadaPercentual;
        private boolean saturado;

        public String getBloco() { return bloco; }
        public void setBloco(String bloco) { this.bloco = bloco; }
        public Integer getCapacidadePosicoes() { return capacidadePosicoes; }
        public void setCapacidadePosicoes(Integer capacidadePosicoes) { this.capacidadePosicoes = capacidadePosicoes; }
        public Integer getReservasAtivas() { return reservasAtivas; }
        public void setReservasAtivas(Integer reservasAtivas) { this.reservasAtivas = reservasAtivas; }
        public Integer getMovimentosPrevistos() { return movimentosPrevistos; }
        public void setMovimentosPrevistos(Integer movimentosPrevistos) { this.movimentosPrevistos = movimentosPrevistos; }
        public Integer getEntradas() { return entradas; }
        public void setEntradas(Integer entradas) { this.entradas = entradas; }
        public Integer getSaidas() { return saidas; }
        public void setSaidas(Integer saidas) { this.saidas = saidas; }
        public Integer getRehandles() { return rehandles; }
        public void setRehandles(Integer rehandles) { this.rehandles = rehandles; }
        public Double getOcupacaoProjetadaPercentual() { return ocupacaoProjetadaPercentual; }
        public void setOcupacaoProjetadaPercentual(Double ocupacaoProjetadaPercentual) { this.ocupacaoProjetadaPercentual = ocupacaoProjetadaPercentual; }
        public boolean isSaturado() { return saturado; }
        public void setSaturado(boolean saturado) { this.saturado = saturado; }
    }

    public static class ImpactoPowDto {
        private String pow;
        private Integer workQueues;
        private Integer workInstructions;
        private Integer demandaChe;
        private Integer cheAssociados;
        private Integer deficitChe;
        private boolean bloqueado;
        private List<String> motivosBloqueio = new ArrayList<>();

        public String getPow() { return pow; }
        public void setPow(String pow) { this.pow = pow; }
        public Integer getWorkQueues() { return workQueues; }
        public void setWorkQueues(Integer workQueues) { this.workQueues = workQueues; }
        public Integer getWorkInstructions() { return workInstructions; }
        public void setWorkInstructions(Integer workInstructions) { this.workInstructions = workInstructions; }
        public Integer getDemandaChe() { return demandaChe; }
        public void setDemandaChe(Integer demandaChe) { this.demandaChe = demandaChe; }
        public Integer getCheAssociados() { return cheAssociados; }
        public void setCheAssociados(Integer cheAssociados) { this.cheAssociados = cheAssociados; }
        public Integer getDeficitChe() { return deficitChe; }
        public void setDeficitChe(Integer deficitChe) { this.deficitChe = deficitChe; }
        public boolean isBloqueado() { return bloqueado; }
        public void setBloqueado(boolean bloqueado) { this.bloqueado = bloqueado; }
        public List<String> getMotivosBloqueio() { return motivosBloqueio; }
        public void setMotivosBloqueio(List<String> motivosBloqueio) { this.motivosBloqueio = motivosBloqueio == null ? new ArrayList<>() : motivosBloqueio; }
    }

    public static class UnidadeImpactoDto {
        private Long planoId;
        private String codigoContainer;
        private String bloco;
        private Integer linha;
        private Integer coluna;
        private String camada;
        private EstadoPlanoPosicaoOperacional estado;
        private String equipamentoId;
        private LocalDateTime horizonteInicio;
        private LocalDateTime horizonteFim;
        private LocalDateTime validoAte;
        private String origem;
        private String motivo;

        public Long getPlanoId() { return planoId; }
        public void setPlanoId(Long planoId) { this.planoId = planoId; }
        public String getCodigoContainer() { return codigoContainer; }
        public void setCodigoContainer(String codigoContainer) { this.codigoContainer = codigoContainer; }
        public String getBloco() { return bloco; }
        public void setBloco(String bloco) { this.bloco = bloco; }
        public Integer getLinha() { return linha; }
        public void setLinha(Integer linha) { this.linha = linha; }
        public Integer getColuna() { return coluna; }
        public void setColuna(Integer coluna) { this.coluna = coluna; }
        public String getCamada() { return camada; }
        public void setCamada(String camada) { this.camada = camada; }
        public EstadoPlanoPosicaoOperacional getEstado() { return estado; }
        public void setEstado(EstadoPlanoPosicaoOperacional estado) { this.estado = estado; }
        public String getEquipamentoId() { return equipamentoId; }
        public void setEquipamentoId(String equipamentoId) { this.equipamentoId = equipamentoId; }
        public LocalDateTime getHorizonteInicio() { return horizonteInicio; }
        public void setHorizonteInicio(LocalDateTime horizonteInicio) { this.horizonteInicio = horizonteInicio; }
        public LocalDateTime getHorizonteFim() { return horizonteFim; }
        public void setHorizonteFim(LocalDateTime horizonteFim) { this.horizonteFim = horizonteFim; }
        public LocalDateTime getValidoAte() { return validoAte; }
        public void setValidoAte(LocalDateTime validoAte) { this.validoAte = validoAte; }
        public String getOrigem() { return origem; }
        public void setOrigem(String origem) { this.origem = origem; }
        public String getMotivo() { return motivo; }
        public void setMotivo(String motivo) { this.motivo = motivo; }
    }
}
