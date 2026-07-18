package br.com.cloudport.servicoyard.patio.listatrabalho.dto;

import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.OrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.StatusConfirmacaoVmt;
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
    private boolean prioridadeBusca;
    private Long workQueueId;
    private String chaveIdempotencia;
    private StatusConfirmacaoVmt statusConfirmacaoVmt;
    private LocalDateTime vmtAceitoEm;
    private LocalDateTime vmtIniciadoEm;
    private LocalDateTime vmtFalhaEm;
    private LocalDateTime vmtConcluidoEm;
    private String ultimoEventoVmtId;
    private String resultadoVmt;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
    private LocalDateTime concluidoEm;

    public static OrdemTrabalhoPatioRespostaDto deEntidade(OrdemTrabalhoPatio ordem) {
        OrdemTrabalhoPatioRespostaDto dto = new OrdemTrabalhoPatioRespostaDto();
        dto.id = ordem.getId();
        dto.codigoConteiner = escapar(ordem.getCodigoConteiner());
        dto.tipoCarga = escapar(ordem.getTipoCarga());
        dto.destino = escapar(ordem.getDestino());
        dto.linhaDestino = ordem.getLinhaDestino();
        dto.colunaDestino = ordem.getColunaDestino();
        dto.camadaDestino = escapar(ordem.getCamadaDestino());
        dto.tipoMovimento = ordem.getTipoMovimento();
        dto.statusOrdem = ordem.getStatusOrdem();
        dto.statusConteinerDestino = ordem.getStatusConteinerDestino();
        dto.visitaNavioId = ordem.getVisitaNavioId();
        dto.itemOperacaoNavioId = ordem.getItemOperacaoNavioId();
        dto.planoEstivaNavioId = ordem.getPlanoEstivaNavioId();
        dto.tipoOrigem = escapar(ordem.getTipoOrigem());
        dto.tipoDestino = escapar(ordem.getTipoDestino());
        dto.sequenciaNavio = ordem.getSequenciaNavio();
        dto.prioridadeOperacional = ordem.getPrioridadeOperacional();
        dto.prioridadeBusca = ordem.isPrioridadeBusca();
        dto.workQueueId = ordem.getWorkQueueId();
        dto.chaveIdempotencia = escapar(ordem.getChaveIdempotencia());
        dto.statusConfirmacaoVmt = ordem.getStatusConfirmacaoVmt();
        dto.vmtAceitoEm = ordem.getVmtAceitoEm();
        dto.vmtIniciadoEm = ordem.getVmtIniciadoEm();
        dto.vmtFalhaEm = ordem.getVmtFalhaEm();
        dto.vmtConcluidoEm = ordem.getVmtConcluidoEm();
        dto.ultimoEventoVmtId = escapar(ordem.getUltimoEventoVmtId());
        dto.resultadoVmt = escapar(ordem.getResultadoVmt());
        dto.criadoEm = ordem.getCriadoEm();
        dto.atualizadoEm = ordem.getAtualizadoEm();
        dto.concluidoEm = ordem.getConcluidoEm();
        return dto;
    }

    private static String escapar(String valor) {
        return valor == null ? null : HtmlUtils.htmlEscape(valor, "UTF-8");
    }

    public Long getId() { return id; }
    public String getCodigoConteiner() { return codigoConteiner; }
    public String getTipoCarga() { return tipoCarga; }
    public String getDestino() { return destino; }
    public Integer getLinhaDestino() { return linhaDestino; }
    public Integer getColunaDestino() { return colunaDestino; }
    public String getCamadaDestino() { return camadaDestino; }
    public TipoMovimentoPatio getTipoMovimento() { return tipoMovimento; }
    public StatusOrdemTrabalhoPatio getStatusOrdem() { return statusOrdem; }
    public StatusConteiner getStatusConteinerDestino() { return statusConteinerDestino; }
    public Long getVisitaNavioId() { return visitaNavioId; }
    public Long getItemOperacaoNavioId() { return itemOperacaoNavioId; }
    public Long getPlanoEstivaNavioId() { return planoEstivaNavioId; }
    public String getTipoOrigem() { return tipoOrigem; }
    public String getTipoDestino() { return tipoDestino; }
    public Integer getSequenciaNavio() { return sequenciaNavio; }
    public Integer getPrioridadeOperacional() { return prioridadeOperacional; }
    public boolean isPrioridadeBusca() { return prioridadeBusca; }
    public Long getWorkQueueId() { return workQueueId; }
    public String getChaveIdempotencia() { return chaveIdempotencia; }
    public StatusConfirmacaoVmt getStatusConfirmacaoVmt() { return statusConfirmacaoVmt; }
    public LocalDateTime getVmtAceitoEm() { return vmtAceitoEm; }
    public LocalDateTime getVmtIniciadoEm() { return vmtIniciadoEm; }
    public LocalDateTime getVmtFalhaEm() { return vmtFalhaEm; }
    public LocalDateTime getVmtConcluidoEm() { return vmtConcluidoEm; }
    public String getUltimoEventoVmtId() { return ultimoEventoVmtId; }
    public String getResultadoVmt() { return resultadoVmt; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public LocalDateTime getConcluidoEm() { return concluidoEm; }
}
