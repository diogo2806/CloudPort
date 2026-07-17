package br.com.cloudport.servicoyard.estivagembulk.dto;

import java.util.ArrayList;
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
    private List<BobinaManifestoDto> bobinas = new ArrayList<>();
    private List<PosicaoBobinaDto> posicoes = new ArrayList<>();
    private EstabilidadeEstrutural estabilidade;
    private List<ViolacaoEstivaDto> violacoes = new ArrayList<>();

    public PlanoEstivaBulkDto() {
    }

    public PlanoEstivaBulkDto(Long id, Long navioId, String nomeNavio, String codigoViagem, String portoCarga,
            String portoDescarga, String status, int totalBobinas, double pesoTotalToneladas,
            List<PosicaoBobinaDto> posicoes, EstabilidadeEstrutural estabilidade,
            List<ViolacaoEstivaDto> violacoes) {
        this.id = id;
        this.navioId = navioId;
        this.nomeNavio = nomeNavio;
        this.codigoViagem = codigoViagem;
        this.portoCarga = portoCarga;
        this.portoDescarga = portoDescarga;
        this.status = status;
        this.totalBobinas = totalBobinas;
        this.pesoTotalToneladas = pesoTotalToneladas;
        this.posicoes = posicoes;
        this.estabilidade = estabilidade;
        this.violacoes = violacoes;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getNavioId() { return navioId; }
    public void setNavioId(Long navioId) { this.navioId = navioId; }
    public Long getNavioCadastroId() { return navioCadastroId; }
    public void setNavioCadastroId(Long navioCadastroId) { this.navioCadastroId = navioCadastroId; }
    public Long getVisitaNavioId() { return visitaNavioId; }
    public void setVisitaNavioId(Long visitaNavioId) { this.visitaNavioId = visitaNavioId; }
    public String getCodigoVisita() { return codigoVisita; }
    public void setCodigoVisita(String codigoVisita) { this.codigoVisita = codigoVisita; }
    public Long getVersaoPerfilNavio() { return versaoPerfilNavio; }
    public void setVersaoPerfilNavio(Long versaoPerfilNavio) { this.versaoPerfilNavio = versaoPerfilNavio; }
    public Long getVersaoNavioCanonico() { return versaoNavioCanonico; }
    public void setVersaoNavioCanonico(Long versaoNavioCanonico) { this.versaoNavioCanonico = versaoNavioCanonico; }
    public Long getVersaoVisita() { return versaoVisita; }
    public void setVersaoVisita(Long versaoVisita) { this.versaoVisita = versaoVisita; }
    public String getNomeNavio() { return nomeNavio; }
    public void setNomeNavio(String nomeNavio) { this.nomeNavio = nomeNavio; }
    public String getCodigoViagem() { return codigoViagem; }
    public void setCodigoViagem(String codigoViagem) { this.codigoViagem = codigoViagem; }
    public String getPortoCarga() { return portoCarga; }
    public void setPortoCarga(String portoCarga) { this.portoCarga = portoCarga; }
    public String getPortoDescarga() { return portoDescarga; }
    public void setPortoDescarga(String portoDescarga) { this.portoDescarga = portoDescarga; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getTotalBobinas() { return totalBobinas; }
    public void setTotalBobinas(int totalBobinas) { this.totalBobinas = totalBobinas; }
    public double getPesoTotalToneladas() { return pesoTotalToneladas; }
    public void setPesoTotalToneladas(double pesoTotalToneladas) { this.pesoTotalToneladas = pesoTotalToneladas; }
    public List<BobinaManifestoDto> getBobinas() { return bobinas; }
    public void setBobinas(List<BobinaManifestoDto> bobinas) { this.bobinas = bobinas; }
    public List<PosicaoBobinaDto> getPosicoes() { return posicoes; }
    public void setPosicoes(List<PosicaoBobinaDto> posicoes) { this.posicoes = posicoes; }
    public EstabilidadeEstrutural getEstabilidade() { return estabilidade; }
    public void setEstabilidade(EstabilidadeEstrutural estabilidade) { this.estabilidade = estabilidade; }
    public List<ViolacaoEstivaDto> getViolacoes() { return violacoes; }
    public void setViolacoes(List<ViolacaoEstivaDto> violacoes) { this.violacoes = violacoes; }
}
