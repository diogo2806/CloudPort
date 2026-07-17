package br.com.cloudport.servicoyard.estivagembulk.dto;

import java.util.List;

public class PlanoEstivaBulkDto {
    private Long id;
    private Long navioId;
    private Long navioCadastroId;
    private Long visitaNavioId;
    private String codigoVisita;
    private Long versaoPerfilNavio;
    private Long versaoNavioCanonico;
    private Long versaoVisita;
    private String nomeNavio;
    private String codigoViagem;
    private String portoCarga;
    private String portoDescarga;
    private String status;
    private int totalBobinas;
    private double pesoTotalToneladas;
    private List<PosicaoBobinaDto> posicoes;
    private EstabilidadeEstrutural estabilidade;
    private ValidacaoPlanoBulkDto validacaoSeguranca;
    private List<ViolacaoEstivaDto> violacoes;
    public PlanoEstivaBulkDto() { }
    public PlanoEstivaBulkDto(Long id, Long navioId, String nomeNavio, String codigoViagem, String portoCarga,
            String portoDescarga, String status, int totalBobinas, double pesoTotalToneladas,
            List<PosicaoBobinaDto> posicoes, EstabilidadeEstrutural estabilidade, List<ViolacaoEstivaDto> violacoes) {
        this.id = id; this.navioId = navioId; this.nomeNavio = nomeNavio; this.codigoViagem = codigoViagem;
        this.portoCarga = portoCarga; this.portoDescarga = portoDescarga; this.status = status;
        this.totalBobinas = totalBobinas; this.pesoTotalToneladas = pesoTotalToneladas;
        this.posicoes = posicoes; this.estabilidade = estabilidade; this.violacoes = violacoes;
    }
    public Long getId() { return id; } public void setId(Long v) { id = v; }
    public Long getNavioId() { return navioId; } public void setNavioId(Long v) { navioId = v; }
    public Long getNavioCadastroId() { return navioCadastroId; } public void setNavioCadastroId(Long v) { navioCadastroId = v; }
    public Long getVisitaNavioId() { return visitaNavioId; } public void setVisitaNavioId(Long v) { visitaNavioId = v; }
    public String getCodigoVisita() { return codigoVisita; } public void setCodigoVisita(String v) { codigoVisita = v; }
    public Long getVersaoPerfilNavio() { return versaoPerfilNavio; } public void setVersaoPerfilNavio(Long v) { versaoPerfilNavio = v; }
    public Long getVersaoNavioCanonico() { return versaoNavioCanonico; } public void setVersaoNavioCanonico(Long v) { versaoNavioCanonico = v; }
    public Long getVersaoVisita() { return versaoVisita; } public void setVersaoVisita(Long v) { versaoVisita = v; }
    public String getNomeNavio() { return nomeNavio; } public void setNomeNavio(String v) { nomeNavio = v; }
    public String getCodigoViagem() { return codigoViagem; } public void setCodigoViagem(String v) { codigoViagem = v; }
    public String getPortoCarga() { return portoCarga; } public void setPortoCarga(String v) { portoCarga = v; }
    public String getPortoDescarga() { return portoDescarga; } public void setPortoDescarga(String v) { portoDescarga = v; }
    public String getStatus() { return status; } public void setStatus(String v) { status = v; }
    public int getTotalBobinas() { return totalBobinas; } public void setTotalBobinas(int v) { totalBobinas = v; }
    public double getPesoTotalToneladas() { return pesoTotalToneladas; } public void setPesoTotalToneladas(double v) { pesoTotalToneladas = v; }
    public List<PosicaoBobinaDto> getPosicoes() { return posicoes; } public void setPosicoes(List<PosicaoBobinaDto> v) { posicoes = v; }
    public EstabilidadeEstrutural getEstabilidade() { return estabilidade; } public void setEstabilidade(EstabilidadeEstrutural v) { estabilidade = v; }
    public ValidacaoPlanoBulkDto getValidacaoSeguranca() { return validacaoSeguranca; }
    public void setValidacaoSeguranca(ValidacaoPlanoBulkDto v) { validacaoSeguranca = v; }
    public List<ViolacaoEstivaDto> getViolacoes() { return violacoes; } public void setViolacoes(List<ViolacaoEstivaDto> v) { violacoes = v; }
}
