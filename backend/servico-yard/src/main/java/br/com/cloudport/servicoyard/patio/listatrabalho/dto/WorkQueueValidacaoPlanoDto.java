package br.com.cloudport.servicoyard.patio.listatrabalho.dto;

public class WorkQueueValidacaoPlanoDto {

    private Long id;
    private Long visitaNavioId;
    private String identificador;
    private String berco;
    private Integer porao;
    private String status;
    private String pow;
    private String poolOperacional;
    private Long equipamentoPatioId;
    private String equipamentoIdentificador;
    private String equipamentoTipo;
    private String equipamentoStatus;
    private Long planoGuindasteId;
    private Long recursoCaisId;
    private int totalOrdens;
    private int totalOrdensDispatchaveis;
    private boolean coberturaValida;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getVisitaNavioId() { return visitaNavioId; }
    public void setVisitaNavioId(Long visitaNavioId) { this.visitaNavioId = visitaNavioId; }
    public String getIdentificador() { return identificador; }
    public void setIdentificador(String identificador) { this.identificador = identificador; }
    public String getBerco() { return berco; }
    public void setBerco(String berco) { this.berco = berco; }
    public Integer getPorao() { return porao; }
    public void setPorao(Integer porao) { this.porao = porao; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPow() { return pow; }
    public void setPow(String pow) { this.pow = pow; }
    public String getPoolOperacional() { return poolOperacional; }
    public void setPoolOperacional(String poolOperacional) { this.poolOperacional = poolOperacional; }
    public Long getEquipamentoPatioId() { return equipamentoPatioId; }
    public void setEquipamentoPatioId(Long equipamentoPatioId) { this.equipamentoPatioId = equipamentoPatioId; }
    public String getEquipamentoIdentificador() { return equipamentoIdentificador; }
    public void setEquipamentoIdentificador(String equipamentoIdentificador) { this.equipamentoIdentificador = equipamentoIdentificador; }
    public String getEquipamentoTipo() { return equipamentoTipo; }
    public void setEquipamentoTipo(String equipamentoTipo) { this.equipamentoTipo = equipamentoTipo; }
    public String getEquipamentoStatus() { return equipamentoStatus; }
    public void setEquipamentoStatus(String equipamentoStatus) { this.equipamentoStatus = equipamentoStatus; }
    public Long getPlanoGuindasteId() { return planoGuindasteId; }
    public void setPlanoGuindasteId(Long planoGuindasteId) { this.planoGuindasteId = planoGuindasteId; }
    public Long getRecursoCaisId() { return recursoCaisId; }
    public void setRecursoCaisId(Long recursoCaisId) { this.recursoCaisId = recursoCaisId; }
    public int getTotalOrdens() { return totalOrdens; }
    public void setTotalOrdens(int totalOrdens) { this.totalOrdens = totalOrdens; }
    public int getTotalOrdensDispatchaveis() { return totalOrdensDispatchaveis; }
    public void setTotalOrdensDispatchaveis(int totalOrdensDispatchaveis) { this.totalOrdensDispatchaveis = totalOrdensDispatchaveis; }
    public boolean isCoberturaValida() { return coberturaValida; }
    public void setCoberturaValida(boolean coberturaValida) { this.coberturaValida = coberturaValida; }
}
