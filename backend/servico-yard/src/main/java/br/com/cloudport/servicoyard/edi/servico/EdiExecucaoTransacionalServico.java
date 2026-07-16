package br.com.cloudport.servicoyard.edi.servico;

import br.com.cloudport.servicoyard.edi.dto.BayPlanRespostaDto;
import br.com.cloudport.servicoyard.edi.dto.CoarriMensagemDto;
import br.com.cloudport.servicoyard.edi.dto.CoprarMensagemDto;
import br.com.cloudport.servicoyard.edi.dto.VermasMensagemDto;
import br.com.cloudport.servicoyard.edi.modelo.ProcessamentoEdi;
import br.com.cloudport.servicoyard.edi.modelo.StatusProcessamentoEdi;
import br.com.cloudport.servicoyard.edi.repositorio.ProcessamentoEdiRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class EdiExecucaoTransacionalServico {

    private final ProcessamentoEdiRepositorio repositorio;
    private final EdiProcessadorServico processador;

    public EdiExecucaoTransacionalServico(ProcessamentoEdiRepositorio repositorio,
                                           EdiProcessadorServico processador) {
        this.repositorio = repositorio;
        this.processador = processador;
    }

    @Transactional
    public void executar(Long processamentoId) {
        ProcessamentoEdi processamento = repositorio.findById(processamentoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Processamento EDI nao encontrado: " + processamentoId));
        if (processamento.getStatus() != StatusProcessamentoEdi.PROCESSANDO) {
            return;
        }

        BayPlanRespostaDto resultado = switch (processamento.getTipoMensagem()) {
            case BAPLIE -> processador.processarBaplie(processamento.getConteudoOriginal());
            case COPRAR -> processador.processarCoprar(coprar(processamento));
            case COARRI -> processador.processarCoarri(coarri(processamento));
            case VERMAS -> processador.processarVermas(vermas(processamento));
        };

        processamento.setStatus(StatusProcessamentoEdi.CONCLUIDO);
        processamento.setBayPlanId(resultado.getId());
        processamento.setProcessandoDesde(null);
        processamento.setProximaTentativaEm(null);
        processamento.setMotivoRejeicao(null);
        if (!StringUtils.hasText(processamento.getCodigoNavio())) {
            processamento.setCodigoNavio(resultado.getCodigoNavio());
        }
        if (!StringUtils.hasText(processamento.getCodigoViagem())) {
            processamento.setCodigoViagem(resultado.getCodigoViagem());
        }
        repositorio.save(processamento);
    }

    private CoprarMensagemDto coprar(ProcessamentoEdi processamento) {
        CoprarMensagemDto dto = new CoprarMensagemDto();
        dto.setCodigoNavio(processamento.getCodigoNavio());
        dto.setCodigoViagem(processamento.getCodigoViagem());
        dto.setConteudoEdifact(processamento.getConteudoOriginal());
        dto.setReferenciaMensagem(processamento.getReferenciaMensagem());
        return dto;
    }

    private CoarriMensagemDto coarri(ProcessamentoEdi processamento) {
        CoarriMensagemDto dto = new CoarriMensagemDto();
        dto.setCodigoNavio(processamento.getCodigoNavio());
        dto.setCodigoViagem(processamento.getCodigoViagem());
        dto.setConteudoEdifact(processamento.getConteudoOriginal());
        dto.setReferenciaMensagem(processamento.getReferenciaMensagem());
        return dto;
    }

    private VermasMensagemDto vermas(ProcessamentoEdi processamento) {
        VermasMensagemDto dto = new VermasMensagemDto();
        dto.setCodigoNavio(processamento.getCodigoNavio());
        dto.setCodigoViagem(processamento.getCodigoViagem());
        dto.setConteudoEdifact(processamento.getConteudoOriginal());
        dto.setReferenciaMensagem(processamento.getReferenciaMensagem());
        return dto;
    }
}
