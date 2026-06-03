package br.com.cloudport.servicoyard.estivagembulk.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DocumentoCargoManifestDto {

    private String tipoDocumento;
    private String codigoViagem;
    private String nomeNavio;
    private String imoNavio;
    private String portoCarga;
    private String portoDescarga;
    private LocalDateTime dataGeracao;
    private double pesoTotalToneladas;
    private int totalItens;
    private List<ItemManifestDto> itens = new ArrayList<>();
    private List<String> observacoesSolas = new ArrayList<>();

    public static class ItemManifestDto {
        private String codigo;
        private String tipoCarga;
        private double pesoKg;
        private String portoDescarga;
        private String heatNumber;
        private String ordemVendaErp;
        private String grauAco;
        private int camada;
        private int poraoNumero;

        public String getCodigo() { return codigo; }
        public void setCodigo(String codigo) { this.codigo = codigo; }
        public String getTipoCarga() { return tipoCarga; }
        public void setTipoCarga(String tipoCarga) { this.tipoCarga = tipoCarga; }
        public double getPesoKg() { return pesoKg; }
        public void setPesoKg(double pesoKg) { this.pesoKg = pesoKg; }
        public String getPortoDescarga() { return portoDescarga; }
        public void setPortoDescarga(String portoDescarga) { this.portoDescarga = portoDescarga; }
        public String getHeatNumber() { return heatNumber; }
        public void setHeatNumber(String heatNumber) { this.heatNumber = heatNumber; }
        public String getOrdemVendaErp() { return ordemVendaErp; }
        public void setOrdemVendaErp(String ordemVendaErp) { this.ordemVendaErp = ordemVendaErp; }
        public String getGrauAco() { return grauAco; }
        public void setGrauAco(String grauAco) { this.grauAco = grauAco; }
        public int getCamada() { return camada; }
        public void setCamada(int camada) { this.camada = camada; }
        public int getPoraoNumero() { return poraoNumero; }
        public void setPoraoNumero(int poraoNumero) { this.poraoNumero = poraoNumero; }
    }

    public String getTipoDocumento() { return tipoDocumento; }
    public void setTipoDocumento(String tipoDocumento) { this.tipoDocumento = tipoDocumento; }
    public String getCodigoViagem() { return codigoViagem; }
    public void setCodigoViagem(String codigoViagem) { this.codigoViagem = codigoViagem; }
    public String getNomeNavio() { return nomeNavio; }
    public void setNomeNavio(String nomeNavio) { this.nomeNavio = nomeNavio; }
    public String getImoNavio() { return imoNavio; }
    public void setImoNavio(String imoNavio) { this.imoNavio = imoNavio; }
    public String getPortoCarga() { return portoCarga; }
    public void setPortoCarga(String portoCarga) { this.portoCarga = portoCarga; }
    public String getPortoDescarga() { return portoDescarga; }
    public void setPortoDescarga(String portoDescarga) { this.portoDescarga = portoDescarga; }
    public LocalDateTime getDataGeracao() { return dataGeracao; }
    public void setDataGeracao(LocalDateTime dataGeracao) { this.dataGeracao = dataGeracao; }
    public double getPesoTotalToneladas() { return pesoTotalToneladas; }
    public void setPesoTotalToneladas(double pesoTotalToneladas) { this.pesoTotalToneladas = pesoTotalToneladas; }
    public int getTotalItens() { return totalItens; }
    public void setTotalItens(int totalItens) { this.totalItens = totalItens; }
    public List<ItemManifestDto> getItens() { return itens; }
    public void setItens(List<ItemManifestDto> itens) { this.itens = itens; }
    public List<String> getObservacoesSolas() { return observacoesSolas; }
    public void setObservacoesSolas(List<String> observacoesSolas) { this.observacoesSolas = observacoesSolas; }
}
