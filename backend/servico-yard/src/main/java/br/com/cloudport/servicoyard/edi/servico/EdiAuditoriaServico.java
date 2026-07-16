package br.com.cloudport.servicoyard.edi.servico;

import br.com.cloudport.servicoyard.edi.dto.BayPlanRespostaDto;
import br.com.cloudport.servicoyard.edi.dto.CoarriMensagemDto;
import br.com.cloudport.servicoyard.edi.dto.CoprarMensagemDto;
import br.com.cloudport.servicoyard.edi.dto.PaginaRespostaDto;
import br.com.cloudport.servicoyard.edi.dto.ProcessamentoEdiRespostaDto;
import br.com.cloudport.servicoyard.edi.dto.VermasMensagemDto;
import br.com.cloudport.servicoyard.edi.modelo.ProcessamentoEdi;
import br.com.cloudport.servicoyard.edi.modelo.StatusProcessamentoEdi;
import br.com.cloudport.servicoyard.edi.modelo.TipoMensagemEdi;
import br.com.cloudport.servicoyard.edi.repositorio.ProcessamentoEdiRepositorio;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.ComandoMotivadoDto;
import java.util.function.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EdiAuditoriaServico {

    private static final int MAXIMO_TENTATIVAS = 5;
    private static final int TAMANHO_MAXIMO_PAGINA = 200;

    private final ProcessamentoEdiRepositorio repositorio;
    private final EdiProcessadorServico processador;

    public EdiAuditoriaServico(ProcessamentoEdiRepositorio repositorio,
                                EdiProcessadorServico processador) {
        this.repositorio = repositorio;
        this.processador = processador;
    }

    public ResultadoProcessamentoEdi processar(TipoMensagemEdi tipo,
                                                String conteudoOriginal,
                                                String codigoNavio,
                                                String codigoViagem,
                                                String referenciaMensagem,
                                                String correlationId,
                                                Supplier<BayPlanRespostaDto> operacao) {
        return processarInterno(
                tipo,
                conteudoOriginal,
                codigoNavio,
                codigoViagem,
                referenciaMensagem,
                correlationId,
                null,
                1,
                null,
                null,
                operacao
        );
    }

    public ResultadoProcessamentoEdi reprocessar(Long id, ComandoMotivadoDto comando) {
        ProcessamentoEdi solicitado = repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Processamento EDI nao encontrado: " + id));
        if (solicitado.getStatus() != StatusProcessamentoEdi.REJEITADO) {
            throw new IllegalArgumentException("Somente mensagens EDI rejeitadas podem ser reprocessadas.");
        }

        Long raizId = solicitado.getReprocessamentoDeId() == null
                ? solicitado.getId()
                : solicitado.getReprocessamentoDeId();
        ProcessamentoEdi ultimaTentativa = repositorio
                .findTopByReprocessamentoDeIdOrderByTentativaDesc(raizId)
                .orElse(solicitado);
        int proximaTentativa = Math.max(solicitado.getTentativa(), ultimaTentativa.getTentativa()) + 1;
        if (proximaTentativa > MAXIMO_TENTATIVAS) {
            throw new IllegalArgumentException(
                    "Limite de " + MAXIMO_TENTATIVAS + " tentativas de reprocessamento EDI atingido."
            );
        }

        Supplier<BayPlanRespostaDto> operacao = criarOperacao(solicitado);
        return processarInterno(
                solicitado.getTipoMensagem(),
                solicitado.getConteudoOriginal(),
                solicitado.getCodigoNavio(),
                solicitado.getCodigoViagem(),
                solicitado.getReferenciaMensagem(),
                comando.getCorrelationId(),
                raizId,
                proximaTentativa,
                comando.getMotivo().trim(),
                usuarioEfetivo(comando),
                operacao
        );
    }

    public PaginaRespostaDto<ProcessamentoEdiRespostaDto> listar(TipoMensagemEdi tipo,
                                                                  StatusProcessamentoEdi status,
                                                                  int pagina,
                                                                  int tamanho) {
        PageRequest pageable = PageRequest.of(
                Math.max(pagina, 0),
                Math.min(Math.max(tamanho, 1), TAMANHO_MAXIMO_PAGINA),
                Sort.by(Sort.Direction.DESC, "criadoEm")
        );
        Page<ProcessamentoEdi> resultado;
        if (tipo != null && status != null) {
            resultado = repositorio.findByTipoMensagemAndStatus(tipo, status, pageable);
        } else if (tipo != null) {
            resultado = repositorio.findByTipoMensagem(tipo, pageable);
        } else if (status != null) {
            resultado = repositorio.findByStatus(status, pageable);
        } else {
            resultado = repositorio.findAll(pageable);
        }
        return PaginaRespostaDto.de(resultado.map(ProcessamentoEdiRespostaDto::de));
    }

    public ProcessamentoEdiRespostaDto buscar(Long id) {
        return repositorio.findById(id)
                .map(ProcessamentoEdiRespostaDto::de)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Processamento EDI nao encontrado: " + id));
    }

    private ResultadoProcessamentoEdi processarInterno(TipoMensagemEdi tipo,
                                                        String conteudoOriginal,
                                                        String codigoNavio,
                                                        String codigoViagem,
                                                        String referenciaMensagem,
                                                        String correlationId,
                                                        Long reprocessamentoDeId,
                                                        int tentativa,
                                                        String motivoReprocessamento,
                                                        String usuarioReprocessamento,
                                                        Supplier<BayPlanRespostaDto> operacao) {
        if (!StringUtils.hasText(conteudoOriginal)) {
            throw new IllegalArgumentException(tipo + ": conteudo original obrigatorio para auditoria.");
        }
        ProcessamentoEdi auditoria = new ProcessamentoEdi();
        auditoria.setTipoMensagem(tipo);
        auditoria.setStatus(StatusProcessamentoEdi.RECEBIDO);
        auditoria.setConteudoOriginal(conteudoOriginal);
        auditoria.setCodigoNavio(normalizar(codigoNavio));
        auditoria.setCodigoViagem(normalizar(codigoViagem));
        auditoria.setReferenciaMensagem(normalizar(referenciaMensagem));
        auditoria.setCorrelationId(normalizar(correlationId));
        auditoria.setReprocessamentoDeId(reprocessamentoDeId);
        auditoria.setTentativa(tentativa);
        auditoria.setMotivoReprocessamento(motivoReprocessamento);
        auditoria.setUsuarioReprocessamento(usuarioReprocessamento);
        auditoria = repositorio.saveAndFlush(auditoria);

        try {
            auditoria.setStatus(StatusProcessamentoEdi.PROCESSANDO);
            repositorio.saveAndFlush(auditoria);
            BayPlanRespostaDto resultado = operacao.get();
            auditoria.setStatus(StatusProcessamentoEdi.CONCLUIDO);
            auditoria.setBayPlanId(resultado.getId());
            if (!StringUtils.hasText(auditoria.getCodigoNavio())) {
                auditoria.setCodigoNavio(resultado.getCodigoNavio());
            }
            if (!StringUtils.hasText(auditoria.getCodigoViagem())) {
                auditoria.setCodigoViagem(resultado.getCodigoViagem());
            }
            ProcessamentoEdi concluido = repositorio.saveAndFlush(auditoria);
            return new ResultadoProcessamentoEdi(ProcessamentoEdiRespostaDto.de(concluido), resultado);
        } catch (RuntimeException ex) {
            auditoria.setStatus(StatusProcessamentoEdi.REJEITADO);
            auditoria.setMotivoRejeicao(limitar(mensagemRaiz(ex), 2000));
            ProcessamentoEdi rejeitado = repositorio.saveAndFlush(auditoria);
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    tipo + " rejeitado. processamentoId=" + rejeitado.getId()
                            + ". Motivo: " + rejeitado.getMotivoRejeicao(),
                    ex
            );
        }
    }

    private Supplier<BayPlanRespostaDto> criarOperacao(ProcessamentoEdi processamento) {
        return switch (processamento.getTipoMensagem()) {
            case BAPLIE -> () -> processador.processarBaplie(processamento.getConteudoOriginal());
            case COPRAR -> () -> processador.processarCoprar(coprar(processamento));
            case COARRI -> () -> processador.processarCoarri(coarri(processamento));
            case VERMAS -> () -> processador.processarVermas(vermas(processamento));
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

    private String usuarioEfetivo(ComandoMotivadoDto comando) {
        return StringUtils.hasText(comando.getUsuario()) ? comando.getUsuario().trim() : "sistema";
    }

    private String normalizar(String valor) {
        return StringUtils.hasText(valor) ? valor.trim() : null;
    }

    private String mensagemRaiz(Throwable erro) {
        Throwable atual = erro;
        while (atual.getCause() != null && atual.getCause() != atual) {
            atual = atual.getCause();
        }
        return StringUtils.hasText(atual.getMessage()) ? atual.getMessage() : erro.getClass().getSimpleName();
    }

    private String limitar(String valor, int limite) {
        return valor.length() <= limite ? valor : valor.substring(0, limite);
    }
}
