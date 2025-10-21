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
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
    private LocalDateTime concluidoEm;

    public OrdemTrabalhoPatioRespostaDto() {
    }

    public OrdemTrabalhoPatioRespostaDto(Long id,
                                         String codigoConteiner,
                                         String tipoCarga,
                                         String destino,
                                         Integer linhaDestino,
                                         Integer colunaDestino,
                                         String camadaDestino,
                                         TipoMovimentoPatio tipoMovimento,
                                         StatusOrdemTrabalhoPatio statusOrdem,
                                         StatusConteiner statusConteinerDestino,
                                         LocalDateTime criadoEm,
                                         LocalDateTime atualizadoEm,
                                         LocalDateTime concluidoEm) {
        this.id = id;
        this.codigoConteiner = codigoConteiner;
        this.tipoCarga = tipoCarga;
        this.destino = destino;
        this.linhaDestino = linhaDestino;
        this.colunaDestino = colunaDestino;
        this.camadaDestino = camadaDestino;
        this.tipoMovimento = tipoMovimento;
        this.statusOrdem = statusOrdem;
        this.statusConteinerDestino = statusConteinerDestino;
        this.criadoEm = criadoEm;
        this.atualizadoEm = atualizadoEm;
        this.concluidoEm = concluidoEm;
    }

    public static OrdemTrabalhoPatioRespostaDto deEntidade(OrdemTrabalhoPatio ordem) {
        return new OrdemTrabalhoPatioRespostaDto(
                ordem.getId(),
                escapar(ordem.getCodigoConteiner()),
                escapar(ordem.getTipoCarga()),
                escapar(ordem.getDestino()),
                ordem.getLinhaDestino(),
                ordem.getColunaDestino(),
                escapar(ordem.getCamadaDestino()),
                ordem.getTipoMovimento(),
                ordem.getStatusOrdem(),
                ordem.getStatusConteinerDestino(),
                ordem.getCriadoEm(),
                ordem.getAtualizadoEm(),
                ordem.getConcluidoEm()
        );
    }

    private static String escapar(String valor) {
        if (valor == null) {
            return null;
        }
        return HtmlUtils.htmlEscape(valor, "UTF-8");
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodigoConteiner() {
        return codigoConteiner;
    }

    public void setCodigoConteiner(String codigoConteiner) {
        this.codigoConteiner = codigoConteiner;
    }

    public String getTipoCarga() {
        return tipoCarga;
    }

    public void setTipoCarga(String tipoCarga) {
        this.tipoCarga = tipoCarga;
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

    public TipoMovimentoPatio getTipoMovimento() {
        return tipoMovimento;
    }

    public void setTipoMovimento(TipoMovimentoPatio tipoMovimento) {
        this.tipoMovimento = tipoMovimento;
    }

    public StatusOrdemTrabalhoPatio getStatusOrdem() {
        return statusOrdem;
    }

    public void setStatusOrdem(StatusOrdemTrabalhoPatio statusOrdem) {
        this.statusOrdem = statusOrdem;
    }

    public StatusConteiner getStatusConteinerDestino() {
        return statusConteinerDestino;
    }

    public void setStatusConteinerDestino(StatusConteiner statusConteinerDestino) {
        this.statusConteinerDestino = statusConteinerDestino;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(LocalDateTime atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }

    public LocalDateTime getConcluidoEm() {
        return concluidoEm;
    }

    public void setConcluidoEm(LocalDateTime concluidoEm) {
        this.concluidoEm = concluidoEm;
    }
}
