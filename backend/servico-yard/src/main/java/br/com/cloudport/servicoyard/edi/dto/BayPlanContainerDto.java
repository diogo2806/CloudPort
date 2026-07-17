package br.com.cloudport.servicoyard.edi.dto;

import br.com.cloudport.servicoyard.edi.modelo.BayPlanContainer;
import br.com.cloudport.servicoyard.edi.modelo.EstadoCargaContainer;
import br.com.cloudport.servicoyard.edi.modelo.TipoOperacaoBayPlan;
import java.time.LocalDateTime;

public class BayPlanContainerDto {

    private Long id;
    private String codigoContainer;
    private String isoCode;
    private Integer bay;
    private Integer row;
    private Integer tier;
    private String posicaoBayEdifact;
    private String portoCarga;
    private String portoDescarga;
    private Double pesoKg;
    private String unidadePesoOriginal;
    private Double pesoVgmKg;
    private String unidadeVgmOriginal;
    private String origemVgm;
    private String statusVgm;
    private EstadoCargaContainer estadoCarga;
    private boolean reefer;
    private Double temperaturaRequeridaC;
    private Double temperaturaMinimaC;
    private Double temperaturaMaximaC;
    private boolean perigoso;
    private String classeImo;
    private String numeroOnu;
    private String grupoEmbalagem;
    private String grupoSegregacao;
    private String codigoEmergencia;
    private boolean oog;
    private Double excessoFrontalCm;
    private Double excessoTraseiroCm;
    private Double excessoEsquerdoCm;
    private Double excessoDireitoCm;
    private Double excessoAlturaCm;
    private String instrucaoManuseio;
    private String referenciaBl;
    private TipoOperacaoBayPlan tipoOperacao;
    private String statusOperacao;
    private LocalDateTime horarioOperacao;
    private Integer linhaYard;
    private Integer colunaYard;

    public static BayPlanContainerDto deEntidade(BayPlanContainer entidade) {
        BayPlanContainerDto dto = new BayPlanContainerDto();
        dto.id = entidade.getId();
        dto.codigoContainer = entidade.getCodigoContainer();
        dto.isoCode = entidade.getIsoCode();
        if (entidade.getPosicaoBay() != null) {
            dto.bay = entidade.getPosicaoBay().getBay();
            dto.row = entidade.getPosicaoBay().getRow();
            dto.tier = entidade.getPosicaoBay().getTier();
            dto.posicaoBayEdifact = entidade.getPosicaoBay().toCodigoEdifact();
        }
        dto.portoCarga = entidade.getPortoCarga();
        dto.portoDescarga = entidade.getPortoDescarga();
        dto.pesoKg = entidade.getPesoKg();
        dto.unidadePesoOriginal = entidade.getUnidadePesoOriginal();
        dto.pesoVgmKg = entidade.getPesoVgmKg();
        dto.unidadeVgmOriginal = entidade.getUnidadeVgmOriginal();
        dto.origemVgm = entidade.getOrigemVgm();
        dto.statusVgm = entidade.getStatusVgm();
        dto.estadoCarga = entidade.getEstadoCarga();
        dto.reefer = entidade.isReefer();
        dto.temperaturaRequeridaC = entidade.getTemperaturaRequeridaC();
        dto.temperaturaMinimaC = entidade.getTemperaturaMinimaC();
        dto.temperaturaMaximaC = entidade.getTemperaturaMaximaC();
        dto.perigoso = entidade.isPerigoso();
        dto.classeImo = entidade.getClasseImo();
        dto.numeroOnu = entidade.getNumeroOnu();
        dto.grupoEmbalagem = entidade.getGrupoEmbalagem();
        dto.grupoSegregacao = entidade.getGrupoSegregacao();
        dto.codigoEmergencia = entidade.getCodigoEmergencia();
        dto.oog = entidade.isOog();
        dto.excessoFrontalCm = entidade.getExcessoFrontalCm();
        dto.excessoTraseiroCm = entidade.getExcessoTraseiroCm();
        dto.excessoEsquerdoCm = entidade.getExcessoEsquerdoCm();
        dto.excessoDireitoCm = entidade.getExcessoDireitoCm();
        dto.excessoAlturaCm = entidade.getExcessoAlturaCm();
        dto.instrucaoManuseio = entidade.getInstrucaoManuseio();
        dto.referenciaBl = entidade.getReferenciaBl();
        dto.tipoOperacao = entidade.getTipoOperacao();
        dto.statusOperacao = entidade.getStatusOperacao();
        dto.horarioOperacao = entidade.getHorarioOperacao();
        dto.linhaYard = entidade.getLinhaYard();
        dto.colunaYard = entidade.getColunaYard();
        return dto;
    }

    public Long getId() { return id; }
    public String getCodigoContainer() { return codigoContainer; }
    public String getIsoCode() { return isoCode; }
    public Integer getBay() { return bay; }
    public Integer getRow() { return row; }
    public Integer getTier() { return tier; }
    public String getPosicaoBayEdifact() { return posicaoBayEdifact; }
    public String getPortoCarga() { return portoCarga; }
    public String getPortoDescarga() { return portoDescarga; }
    public Double getPesoKg() { return pesoKg; }
    public String getUnidadePesoOriginal() { return unidadePesoOriginal; }
    public Double getPesoVgmKg() { return pesoVgmKg; }
    public String getUnidadeVgmOriginal() { return unidadeVgmOriginal; }
    public String getOrigemVgm() { return origemVgm; }
    public String getStatusVgm() { return statusVgm; }
    public EstadoCargaContainer getEstadoCarga() { return estadoCarga; }
    public boolean isReefer() { return reefer; }
    public Double getTemperaturaRequeridaC() { return temperaturaRequeridaC; }
    public Double getTemperaturaMinimaC() { return temperaturaMinimaC; }
    public Double getTemperaturaMaximaC() { return temperaturaMaximaC; }
    public boolean isPerigoso() { return perigoso; }
    public String getClasseImo() { return classeImo; }
    public String getNumeroOnu() { return numeroOnu; }
    public String getGrupoEmbalagem() { return grupoEmbalagem; }
    public String getGrupoSegregacao() { return grupoSegregacao; }
    public String getCodigoEmergencia() { return codigoEmergencia; }
    public boolean isOog() { return oog; }
    public Double getExcessoFrontalCm() { return excessoFrontalCm; }
    public Double getExcessoTraseiroCm() { return excessoTraseiroCm; }
    public Double getExcessoEsquerdoCm() { return excessoEsquerdoCm; }
    public Double getExcessoDireitoCm() { return excessoDireitoCm; }
    public Double getExcessoAlturaCm() { return excessoAlturaCm; }
    public String getInstrucaoManuseio() { return instrucaoManuseio; }
    public String getReferenciaBl() { return referenciaBl; }
    public TipoOperacaoBayPlan getTipoOperacao() { return tipoOperacao; }
    public String getStatusOperacao() { return statusOperacao; }
    public LocalDateTime getHorarioOperacao() { return horarioOperacao; }
    public Integer getLinhaYard() { return linhaYard; }
    public Integer getColunaYard() { return colunaYard; }
}
