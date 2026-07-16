package br.com.cloudport.servicoyard.edi.servico;

import br.com.cloudport.servicoyard.edi.dto.BayPlanRespostaDto;
import br.com.cloudport.servicoyard.edi.dto.CoarriMensagemDto;
import br.com.cloudport.servicoyard.edi.dto.CoprarMensagemDto;
import br.com.cloudport.servicoyard.edi.dto.VermasMensagemDto;
import br.com.cloudport.servicoyard.edi.modelo.ProcessamentoEdi;
import br.com.cloudport.servicoyard.edi.modelo.StatusProcessamentoEdi;
import br.com.cloudport.servicoyard.edi.repositorio.ProcessamentoEdiRepositorio;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class EdiProcessamentoWorkerServico {

    private static final List<StatusProcessamentoEdi> STATUS_PENDENTES = List.of(
            StatusProcessamentoEdi.RECEBIDO,
            StatusProcessamentoEdi.AGUARDANDO_REPROCESSAMENTO
    );

    private final ProcessamentoEdiRepositorio repositorio;
    private final EdiProcessadorServico processador;

    public EdiProcessamentoWorkerServico(ProcessamentoEdiRepositorio repositorio,
                                          EdiProcessadorServico processador) {
        this.repositorio = repositorio;
        this.processador = processador;
    }

    @Transactional
    public boolean processarProximo() {
        List<ProcessamentoEdi> pendentes = repositorio.buscarPendentesParaProcessamento(
                STATUS_PENDENTES,
                LocalDateTime.now(),
                PageRequest.of(0, 1)
        );
        if (pendentes.isEmpty()) {
            return false;
        }

        ProcessamentoEdi processamento = pendentes.get(0);
        processamento.setStatus(StatusProcessamentoEdi.PROCESSANDO);
        processamento.setTentativa(
                Math.max(processamento.getTentativa() == null ? 0 : processamento.getTentativa(), 0) + 1);
        processamento.setProximaTentativaEm(null);
        repositorio.saveAndFlush(processamento);

        try {
            BayPlanRespostaDto resultado = executar(processamento);
            processamento.setStatus(StatusProcessamentoEdi.CONCLUIDO);
            processamento.setBayPlanId(resultado.getId());
            processamento.setMotivoRejeicao(null);
            if (!StringUtils.hasText(processamento.getCodigoNavio())) {
                processamento.setCodigoNavio(resultado.getCodigoNavio());
            }
            if (!StringUtils.hasText(processamento.getCodigoViagem())) {
                processamento.setCodigoViagem(resultado.getCodigoViagem());
            }
            repositorio.saveAndFlush(processamento);
            return true;
        } catch (RuntimeException ex) {
            throw new FalhaProcessamentoEdiException(
                    processamento.getId(),
                    ex instanceof IllegalArgumentException,
                    ex
            );
        }
    }

    private BayPlanRespostaDto executar(ProcessamentoEdi processamento) {
        return switch (processamento.getTipoMensagem()) {
            case BAPLIE -> processador.processarBaplie(processamento.getConteudoOriginal());
            case COPRAR -> processador.processarCoprar(coprar(processamento));
            case COARRI -> processador.processarCoarri(coarri(processamento));
            case VERMAS -> processador.processarVermas(vermas(processamento));
        };
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
