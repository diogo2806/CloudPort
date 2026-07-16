package br.com.cloudport.servicoyard.edi.dto;

import br.com.cloudport.servicoyard.edi.modelo.ProcessamentoEdi;
import br.com.cloudport.servicoyard.edi.modelo.StatusProcessamentoEdi;
import br.com.cloudport.servicoyard.edi.modelo.TipoMensagemEdi;
import java.time.LocalDateTime;

public record ProcessamentoEdiRespostaDto(
        Long id,
        TipoMensagemEdi tipoMensagem,
        StatusProcessamentoEdi status,
        String codigoNavio,
        String codigoViagem,
        String referenciaMensagem,
        String identificadorInterchange,
        String identificadorMensagem,
        String chaveIdempotencia,
        String hashConteudo,
        String correlationId,
        String motivoRejeicao,
        String motivoReprocessamento,
        String usuarioReprocessamento,
        Long reprocessamentoDeId,
        Integer tentativa,
        LocalDateTime proximaTentativaEm,
        Long bayPlanId,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm
) {

    public static ProcessamentoEdiRespostaDto de(ProcessamentoEdi entidade) {
        return new ProcessamentoEdiRespostaDto(
                entidade.getId(),
                entidade.getTipoMensagem(),
                entidade.getStatus(),
                entidade.getCodigoNavio(),
                entidade.getCodigoViagem(),
                entidade.getReferenciaMensagem(),
                entidade.getIdentificadorInterchange(),
                entidade.getIdentificadorMensagem(),
                entidade.getChaveIdempotencia(),
                entidade.getHashConteudo(),
                entidade.getCorrelationId(),
                entidade.getMotivoRejeicao(),
                entidade.getMotivoReprocessamento(),
                entidade.getUsuarioReprocessamento(),
                entidade.getReprocessamentoDeId(),
                entidade.getTentativa(),
                entidade.getProximaTentativaEm(),
                entidade.getBayPlanId(),
                entidade.getCriadoEm(),
                entidade.getAtualizadoEm()
        );
    }
}
