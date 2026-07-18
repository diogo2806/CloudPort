package br.com.cloudport.servicoyard.vesselplanner.dto;

import br.com.cloudport.servicoyard.vesselplanner.modelo.DecisaoResolucaoReconciliacao;
import br.com.cloudport.servicoyard.vesselplanner.modelo.DivergenciaReconciliacaoSlot;
import br.com.cloudport.servicoyard.vesselplanner.modelo.SeveridadeDivergenciaReconciliacao;
import br.com.cloudport.servicoyard.vesselplanner.modelo.StatusDivergenciaReconciliacao;
import br.com.cloudport.servicoyard.vesselplanner.modelo.TipoDivergenciaReconciliacao;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DivergenciaReconciliacaoDto {

    private Long id;
    private Long slotId;
    private String codigoContainer;
    private TipoDivergenciaReconciliacao tipo;
    private SeveridadeDivergenciaReconciliacao severidade;
    private StatusDivergenciaReconciliacao status;
    private String valorBaplie;
    private String valorPlano;
    private String valorInventario;
    private String valorExecucao;
    private DecisaoResolucaoReconciliacao decisao;
    private String justificativa;
    private String resolvidoPor;
    private LocalDateTime resolvidoEm;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

    public static DivergenciaReconciliacaoDto deEntidade(DivergenciaReconciliacaoSlot entidade) {
        DivergenciaReconciliacaoDto dto = new DivergenciaReconciliacaoDto();
        dto.setId(entidade.getId());
        dto.setSlotId(entidade.getSlot() != null ? entidade.getSlot().getId() : null);
        dto.setCodigoContainer(entidade.getCodigoContainer());
        dto.setTipo(entidade.getTipo());
        dto.setSeveridade(entidade.getSeveridade());
        dto.setStatus(entidade.getStatus());
        dto.setValorBaplie(entidade.getValorBaplie());
        dto.setValorPlano(entidade.getValorPlano());
        dto.setValorInventario(entidade.getValorInventario());
        dto.setValorExecucao(entidade.getValorExecucao());
        dto.setDecisao(entidade.getDecisao());
        dto.setJustificativa(entidade.getJustificativa());
        dto.setResolvidoPor(entidade.getResolvidoPor());
        dto.setResolvidoEm(entidade.getResolvidoEm());
        dto.setCriadoEm(entidade.getCriadoEm());
        dto.setAtualizadoEm(entidade.getAtualizadoEm());
        return dto;
    }
}
