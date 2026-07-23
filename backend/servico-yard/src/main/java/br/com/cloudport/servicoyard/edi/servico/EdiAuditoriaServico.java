package br.com.cloudport.servicoyard.edi.servico;

import br.com.cloudport.servicoyard.edi.dto.PaginaRespostaDto;
import br.com.cloudport.servicoyard.edi.dto.ProcessamentoEdiRespostaDto;
import br.com.cloudport.servicoyard.edi.modelo.ProcessamentoEdi;
import br.com.cloudport.servicoyard.edi.modelo.StatusProcessamentoEdi;
import br.com.cloudport.servicoyard.edi.modelo.TipoMensagemEdi;
import br.com.cloudport.servicoyard.edi.repositorio.ProcessamentoEdiRepositorio;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.ComandoMotivadoDto;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EdiAuditoriaServico {

    private static final int TAMANHO_MAXIMO_PAGINA = 200;
    private static final long ATRASO_MAXIMO_MILISSEGUNDOS = 300_000L;

    private final ProcessamentoEdiRepositorio repositorio;
    private final EdiIdentificadorExtrator identificadorExtrator;
    private final int maximoTentativas;
    private final long atrasoInicialMilissegundos;

    public EdiAuditoriaServico(ProcessamentoEdiRepositorio repositorio,
                               EdiIdentificadorExtrator identificadorExtrator,
                               @Value("${cloudport.edi.worker.max-tentativas:5}") int maximoTentativas,
                               @Value("${cloudport.edi.worker.atraso-inicial-ms:5000}") long atrasoInicialMilissegundos) {
        this.repositorio = repositorio;
        this.identificadorExtrator = identificadorExtrator;
        this.maximoTentativas = Math.max(maximoTentativas, 1);
        this.atrasoInicialMilissegundos = Math.max(atrasoInicialMilissegundos, 100L);
    }

    @Transactional
    public ProcessamentoEdiRespostaDto registrarRecebimento(TipoMensagemEdi tipo, String conteudoOriginal,
                                                              String codigoNavio, String codigoViagem,
                                                              String referenciaMensagem, String correlationId) {
        if (!StringUtils.hasText(conteudoOriginal)) {
            throw new IllegalArgumentException(tipo + ": conteudo original obrigatorio para auditoria.");
        }
        IdentificadoresEdi identificadores = identificadorExtrator.extrair(conteudoOriginal);
        String referenciaNormalizada = normalizar(referenciaMensagem);
        if (referenciaNormalizada == null) referenciaNormalizada = identificadores.messageReferenceNumber();
        String hashConteudo = sha256(conteudoOriginal);
        String chaveIdempotencia = criarChaveIdempotencia(tipo, identificadores.interchangeControlReference(),
                identificadores.messageReferenceNumber(), referenciaNormalizada, hashConteudo);
        LocalDateTime agora = LocalDateTime.now();
        repositorio.inserirSeAusente(tipo.name(), conteudoOriginal, normalizar(codigoNavio), normalizar(codigoViagem),
                referenciaNormalizada, normalizar(correlationId), identificadores.interchangeControlReference(),
                identificadores.messageReferenceNumber(), chaveIdempotencia, hashConteudo, agora);
        ProcessamentoEdi processamento = repositorio.findByChaveIdempotencia(chaveIdempotencia)
                .orElseThrow(() -> new IllegalStateException("A recepcao EDI foi registrada, mas nao pode ser recuperada pela chave idempotente."));
        if (!hashConteudo.equals(processamento.getHashConteudo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A identidade EDI ja existe com conteudo diferente. processamentoId=" + processamento.getId());
        }
        return ProcessamentoEdiRespostaDto.de(processamento);
    }

    @Transactional
    public ProcessamentoEdiRespostaDto reprocessar(Long id, ComandoMotivadoDto comando) {
        ProcessamentoEdi processamento = buscarEntidade(id);
        if (processamento.getStatus() != StatusProcessamentoEdi.QUARENTENA
                && processamento.getStatus() != StatusProcessamentoEdi.REJEITADO
                && processamento.getStatus() != StatusProcessamentoEdi.AGUARDANDO_REPROCESSAMENTO) {
            throw new IllegalArgumentException("Somente mensagens rejeitadas, em quarentena ou aguardando reprocessamento podem ser reprocessadas.");
        }
        aplicarMotivo(processamento, comando);
        processamento.setStatus(StatusProcessamentoEdi.RECEBIDO);
        processamento.setTentativa(0);
        processamento.setProximaTentativaEm(LocalDateTime.now());
        processamento.setMotivoRejeicao(null);
        return ProcessamentoEdiRespostaDto.de(repositorio.saveAndFlush(processamento));
    }

    @Transactional
    public ProcessamentoEdiRespostaDto colocarEmQuarentena(Long id, ComandoMotivadoDto comando) {
        ProcessamentoEdi processamento = buscarEntidade(id);
        if (processamento.getStatus() == StatusProcessamentoEdi.CONCLUIDO
                || processamento.getStatus() == StatusProcessamentoEdi.CANCELADO) {
            throw new IllegalArgumentException("Mensagens concluidas ou canceladas nao podem ser colocadas em quarentena.");
        }
        aplicarMotivo(processamento, comando);
        processamento.setStatus(StatusProcessamentoEdi.QUARENTENA);
        processamento.setProximaTentativaEm(null);
        processamento.setMotivoRejeicao(limitar(comando.getMotivo().trim(), 2000));
        return ProcessamentoEdiRespostaDto.de(repositorio.saveAndFlush(processamento));
    }

    @Transactional
    public ProcessamentoEdiRespostaDto cancelar(Long id, ComandoMotivadoDto comando) {
        ProcessamentoEdi processamento = buscarEntidade(id);
        if (processamento.getStatus() == StatusProcessamentoEdi.CONCLUIDO) {
            throw new IllegalArgumentException("Uma mensagem concluida nao pode ser cancelada.");
        }
        aplicarMotivo(processamento, comando);
        processamento.setStatus(StatusProcessamentoEdi.CANCELADO);
        processamento.setProximaTentativaEm(null);
        processamento.setMotivoRejeicao(limitar(comando.getMotivo().trim(), 2000));
        return ProcessamentoEdiRespostaDto.de(repositorio.saveAndFlush(processamento));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarFalha(Long id, Throwable erro, boolean irrecuperavel) {
        ProcessamentoEdi processamento = buscarEntidade(id);
        int tentativa = Math.max(processamento.getTentativa() == null ? 0 : processamento.getTentativa(), 0) + 1;
        processamento.setTentativa(tentativa);
        processamento.setMotivoRejeicao(limitar(mensagemRaiz(erro), 2000));
        processamento.setBayPlanId(null);
        if (irrecuperavel || tentativa >= maximoTentativas) {
            processamento.setStatus(StatusProcessamentoEdi.QUARENTENA);
            processamento.setProximaTentativaEm(null);
        } else {
            processamento.setStatus(StatusProcessamentoEdi.AGUARDANDO_REPROCESSAMENTO);
            processamento.setProximaTentativaEm(LocalDateTime.now().plusNanos(calcularAtraso(tentativa) * 1_000_000L));
        }
        repositorio.saveAndFlush(processamento);
    }

    public PaginaRespostaDto<ProcessamentoEdiRespostaDto> listar(TipoMensagemEdi tipo, StatusProcessamentoEdi status,
                                                                   int pagina, int tamanho) {
        PageRequest pageable = PageRequest.of(Math.max(pagina, 0), Math.min(Math.max(tamanho, 1), TAMANHO_MAXIMO_PAGINA),
                Sort.by(Sort.Direction.DESC, "criadoEm"));
        Page<ProcessamentoEdi> resultado;
        if (tipo != null && status != null) resultado = repositorio.findByTipoMensagemAndStatus(tipo, status, pageable);
        else if (tipo != null) resultado = repositorio.findByTipoMensagem(tipo, pageable);
        else if (status != null) resultado = repositorio.findByStatus(status, pageable);
        else resultado = repositorio.findAll(pageable);
        return PaginaRespostaDto.de(resultado.map(ProcessamentoEdiRespostaDto::de));
    }

    public ProcessamentoEdiRespostaDto buscar(Long id) {
        return ProcessamentoEdiRespostaDto.de(buscarEntidade(id));
    }

    private ProcessamentoEdi buscarEntidade(Long id) {
        return repositorio.findById(id).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Processamento EDI nao encontrado: " + id));
    }

    private void aplicarMotivo(ProcessamentoEdi processamento, ComandoMotivadoDto comando) {
        if (comando == null || !StringUtils.hasText(comando.getMotivo())) {
            throw new IllegalArgumentException("O motivo operacional e obrigatorio.");
        }
        processamento.setMotivoReprocessamento(comando.getMotivo().trim());
        processamento.setUsuarioReprocessamento(usuarioEfetivo(comando));
        if (StringUtils.hasText(comando.getCorrelationId())) processamento.setCorrelationId(comando.getCorrelationId().trim());
    }

    private String criarChaveIdempotencia(TipoMensagemEdi tipo, String identificadorInterchange,
                                            String identificadorMensagem, String referenciaMensagem, String hashConteudo) {
        boolean possuiIdentidade = StringUtils.hasText(identificadorInterchange)
                || StringUtils.hasText(identificadorMensagem) || StringUtils.hasText(referenciaMensagem);
        return sha256(String.join("|", tipo.name(), valorOuVazio(identificadorInterchange),
                valorOuVazio(identificadorMensagem), valorOuVazio(referenciaMensagem), possuiIdentidade ? "" : hashConteudo));
    }

    private long calcularAtraso(int tentativa) {
        int expoente = Math.min(Math.max(tentativa - 1, 0), 10);
        return Math.min(atrasoInicialMilissegundos * (1L << expoente), ATRASO_MAXIMO_MILISSEGUNDOS);
    }

    private String usuarioEfetivo(ComandoMotivadoDto comando) {
        return StringUtils.hasText(comando.getUsuario()) ? comando.getUsuario().trim() : "sistema";
    }

    private String normalizar(String valor) { return StringUtils.hasText(valor) ? valor.trim().toUpperCase(Locale.ROOT) : null; }
    private String valorOuVazio(String valor) { return valor == null ? "" : valor; }

    private String sha256(String valor) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Algoritmo SHA-256 indisponivel.", ex);
        }
    }

    private String mensagemRaiz(Throwable erro) {
        Throwable atual = erro;
        while (atual.getCause() != null && atual.getCause() != atual) atual = atual.getCause();
        return StringUtils.hasText(atual.getMessage()) ? atual.getMessage() : erro.getClass().getSimpleName();
    }

    private String limitar(String valor, int limite) { return valor.length() <= limite ? valor : valor.substring(0, limite); }
}
