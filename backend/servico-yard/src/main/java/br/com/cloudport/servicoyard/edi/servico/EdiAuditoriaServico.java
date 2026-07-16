package br.com.cloudport.servicoyard.edi.servico;

import br.com.cloudport.servicoyard.edi.dto.PaginaRespostaDto;
import br.com.cloudport.servicoyard.edi.dto.ProcessamentoEdiRespostaDto;
import br.com.cloudport.servicoyard.edi.modelo.ProcessamentoEdi;
import br.com.cloudport.servicoyard.edi.modelo.StatusProcessamentoEdi;
import br.com.cloudport.servicoyard.edi.modelo.TipoMensagemEdi;
import br.com.cloudport.servicoyard.edi.repositorio.ProcessamentoEdiRepositorio;
import br.com.cloudport.servicoyard.edi.servico.IdentidadeMensagemEdiServico.IdentidadeMensagemEdi;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.ComandoMotivadoDto;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EdiAuditoriaServico {

    private static final int TAMANHO_MAXIMO_PAGINA = 200;

    private final ProcessamentoEdiRepositorio repositorio;
    private final IdentidadeMensagemEdiServico identidadeServico;

    public EdiAuditoriaServico(ProcessamentoEdiRepositorio repositorio,
                                IdentidadeMensagemEdiServico identidadeServico) {
        this.repositorio = repositorio;
        this.identidadeServico = identidadeServico;
    }

    @Transactional
    public ProcessamentoEdiRespostaDto receber(TipoMensagemEdi tipo,
                                                String conteudoOriginal,
                                                String codigoNavio,
                                                String codigoViagem,
                                                String referenciaMensagem,
                                                String correlationId) {
        IdentidadeMensagemEdi identidade = identidadeServico.identificar(
                tipo, conteudoOriginal, referenciaMensagem);
        Optional<ProcessamentoEdi> existente = repositorio.findByChaveIdempotencia(
                identidade.chaveIdempotencia());
        if (existente.isPresent()) {
            ProcessamentoEdi processamento = existente.get();
            validarMesmoConteudo(processamento, identidade.hashConteudo());
            if (!StringUtils.hasText(processamento.getCorrelationId())
                    && StringUtils.hasText(correlationId)) {
                processamento.setCorrelationId(correlationId.trim());
                processamento = repositorio.save(processamento);
            }
            return ProcessamentoEdiRespostaDto.de(processamento);
        }

        ProcessamentoEdi processamento = new ProcessamentoEdi();
        processamento.setTipoMensagem(tipo);
        processamento.setStatus(StatusProcessamentoEdi.RECEBIDO);
        processamento.setConteudoOriginal(conteudoOriginal);
        processamento.setIdentificadorUnb(identidade.identificadorUnb());
        processamento.setIdentificadorUnh(identidade.identificadorUnh());
        processamento.setChaveIdempotencia(identidade.chaveIdempotencia());
        processamento.setHashConteudo(identidade.hashConteudo());
        processamento.setCodigoNavio(normalizar(codigoNavio));
        processamento.setCodigoViagem(normalizar(codigoViagem));
        processamento.setReferenciaMensagem(identidade.referenciaMensagem());
        processamento.setCorrelationId(normalizar(correlationId));
        processamento.setTentativa(1);
        processamento.setProximaTentativaEm(LocalDateTime.now());
        return ProcessamentoEdiRespostaDto.de(repositorio.saveAndFlush(processamento));
    }

    @Transactional
    public ProcessamentoEdiRespostaDto reprocessar(Long id, ComandoMotivadoDto comando) {
        ProcessamentoEdi processamento = repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Processamento EDI nao encontrado: " + id));
        if (processamento.getStatus() != StatusProcessamentoEdi.QUARENTENA
                && processamento.getStatus() != StatusProcessamentoEdi.REJEITADO) {
            throw new IllegalArgumentException(
                    "Somente mensagens EDI em quarentena ou rejeitadas podem ser reprocessadas.");
        }

        processamento.setStatus(StatusProcessamentoEdi.RECEBIDO);
        processamento.setTentativa(1);
        processamento.setProximaTentativaEm(LocalDateTime.now());
        processamento.setProcessandoDesde(null);
        processamento.setMotivoRejeicao(null);
        processamento.setMotivoReprocessamento(comando.getMotivo().trim());
        processamento.setUsuarioReprocessamento(usuarioEfetivo(comando));
        processamento.setCorrelationId(normalizar(comando.getCorrelationId()));
        processamento.setBayPlanId(null);
        return ProcessamentoEdiRespostaDto.de(repositorio.saveAndFlush(processamento));
    }

    @Transactional(readOnly = true)
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

    @Transactional(readOnly = true)
    public ProcessamentoEdiRespostaDto buscar(Long id) {
        return repositorio.findById(id)
                .map(ProcessamentoEdiRespostaDto::de)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Processamento EDI nao encontrado: " + id));
    }

    private void validarMesmoConteudo(ProcessamentoEdi existente, String hashConteudo) {
        if (!hashConteudo.equals(existente.getHashConteudo())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A identidade EDI ja foi recebida com conteudo diferente. processamentoId="
                            + existente.getId());
        }
    }

    private String usuarioEfetivo(ComandoMotivadoDto comando) {
        return StringUtils.hasText(comando.getUsuario()) ? comando.getUsuario().trim() : "sistema";
    }

    private String normalizar(String valor) {
        return StringUtils.hasText(valor) ? valor.trim() : null;
    }
}
