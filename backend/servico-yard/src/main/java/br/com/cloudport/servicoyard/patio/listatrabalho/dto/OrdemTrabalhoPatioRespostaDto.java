package br.com.cloudport.servicoyard.patio.listatrabalho.dto;

import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.OrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.StatusOrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.modelo.StatusConteiner;
import br.com.cloudport.servicoyard.patio.modelo.TipoMovimentoPatio;
import java.time.LocalDateTime;
import org.springframework.web.util.HtmlUtils;

public class OrdemTrabalhoPatioRespostaDto {

    private Long id;
    private String codigoConteiner;
    private String tipoCarga;
    private String destino;
    private Integer linhaDestino;
    private Integer colunaDestino;
    private String camadaDestino;
    private TipoMovimentoPatio tipoMovimento;
    private StatusOrdemTrabalhoPatio statusOrdem;
    private StatusConteiner statusConteinerDestino;
    private Long visitaNavioId;
    private Long itemOperacaoNavioId;
    private Long planoEstivaNavioId;
    private String tipoOrigem;
    private String tipoDestino;
    private Integer sequenciaNavio;
    private Integer prioridadeOperacional;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
    private LocalDateTime concluidoEm;

    public OrdemTrabalhoPatioRespostaDto() {
    }

    public static OrdemTrabalhoPatioRespostaDto deEntidade(OrdemTrabalhoPatio ordem) {
        OrdemTrabalhoPatioRespostaDto dto = new OrdemTrabalhoPatioRespostaDto();
        dto.setId(ordem.getId());
        dto.setCodigoConteiner(escapar(ordem.getCodigoConteiner()));
        dto.setTipoCarga(escapar(ordem.getTipoCarga()));
        dto.setDestino(escapar(ordem.getDestino()));
        dto.setLinhaDestino(ordem.getLinhaDestino());
        dto.setColunaDestino(ordem.getColunaDestino());
        dto.setCamadaDestino(escapar(ordem.getCamadaDestino()));
        dto.setTipoMovimento(ordem.getTipoMovimento());
        dto.setStatusOrdem(ordem.getStatusOrdem());
        dto.setStatusConteinerDestino(ordem.getStatusConteinerDestino());
        dto.setVisitaNavioId(ordem.getVisitaNavioId());
        dto.setItemOperacaoNavioId(ordem.getItemOperacaoNavioId());
        dto.setPlanoEstivaNavioId(ordem.getPlanoEstivaNavioId());
        dto.setTipoOrigem(escapar(ordem.getTipoOrigem()));
        dto.setTipoDestino(escapar(ordem.getTipoDestino()));
        dto.setSequenciaNavio(ordem.getSequenciaNavio());
        dto.setPrioridadeOperacional(ordem.getPrioridadeOperacional());
        dto.setCriadoEm(ordem.getCriadoEm());
        dto.setAtualizadoEm(ordem.getAtualizadoEm());
        dto.setConcluidoEm(ordem.getConcluidoEm());
        return dto;
    }

    private static String escapar(String valor) {
        if (valor == null) {
            return null;
        }
        return HtmlUtils.htmlEscape(valor, "UTF-8");
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
    public TipoMovimentoPatio getTipoMovimento() { return tipoMovimento; }
    public void setTipoMovimento(TipoMovimentoPatio tipoMovimento) { this.tipoMovimento = tipoMovimento; }
    public StatusOrdemTrabalhoPatio getStatusOrdem() { return statusOrdem; }
    public void setStatusOrdem(StatusOrdemTrabalhoPatio statusOrdem) { this.statusOrdem = statusOrdem; }
    public StatusConteiner getStatusConteinerDestino() { return statusConteinerDestino; }
    public void setStatusConteinerDestino(StatusConteiner statusConteinerDestino) { this.statusConteinerDestino = statusConteinerDestino; }
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
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(LocalDateTime atualizadoEm) { this.atualizadoEm = atualizadoEm; }
    public LocalDateTime getConcluidoEm() { return concluidoEm; }
    public void setConcluidoEm(LocalDateTime concluidoEm) { this.concluidoEm = concluidoEm; }
}
