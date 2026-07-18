package br.com.cloudport.servicogate.app.gestor;

import br.com.cloudport.servicogate.app.gestor.dto.EntradaPessoaRequest;
import br.com.cloudport.servicogate.app.gestor.dto.MovimentacaoPessoaDTO;
import br.com.cloudport.servicogate.app.gestor.dto.PessoaPresenteDTO;
import br.com.cloudport.servicogate.app.gestor.dto.ResumoAcessoPessoasDTO;
import br.com.cloudport.servicogate.app.gestor.dto.SaidaPessoaRequest;
import br.com.cloudport.servicogate.model.MovimentacaoPessoaAcesso;
import br.com.cloudport.servicogate.model.PessoaAcesso;
import br.com.cloudport.servicogate.model.enums.DirecaoMovimentacaoPessoa;
import br.com.cloudport.servicogate.model.enums.SituacaoPessoaAcesso;
import br.com.cloudport.servicogate.model.enums.TipoPessoaAcesso;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class ControleAcessoPessoasService {

    private static final int LIMITE_MAXIMO_HISTORICO = 200;
    private static final String CONSTRAINT_DOCUMENTO_UNICO = "uk_pessoa_acesso_documento_normalizado";
    private static final String MENSAGEM_CONFLITO_CONCORRENCIA =
            "A transição de acesso já foi realizada por outra requisição. Atualize os dados e tente novamente.";

    private final PessoaAcessoRepository pessoaAcessoRepository;
    private final MovimentacaoPessoaAcessoRepository movimentacaoRepository;

    public ControleAcessoPessoasService(PessoaAcessoRepository pessoaAcessoRepository,
                                         MovimentacaoPessoaAcessoRepository movimentacaoRepository) {
        this.pessoaAcessoRepository = pessoaAcessoRepository;
        this.movimentacaoRepository = movimentacaoRepository;
    }

    public MovimentacaoPessoaDTO registrarEntrada(EntradaPessoaRequest request) {
        try {
            return registrarEntradaSerializada(request);
        } catch (DataIntegrityViolationException exception) {
            if (violouConstraint(exception, CONSTRAINT_DOCUMENTO_UNICO)) {
                throw conflitoConcorrencia(exception);
            }
            throw exception;
        } catch (OptimisticLockingFailureException | PessimisticLockingFailureException exception) {
            throw conflitoConcorrencia(exception);
        }
    }

    private MovimentacaoPessoaDTO registrarEntradaSerializada(EntradaPessoaRequest request) {
        String documentoNormalizado = normalizarDocumento(request.documento());
        PessoaAcesso pessoa = pessoaAcessoRepository.findByDocumentoNormalizado(documentoNormalizado)
                .orElseGet(PessoaAcesso::new);

        if (SituacaoPessoaAcesso.DENTRO.equals(pessoa.getSituacao())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A pessoa já possui uma entrada aberta no terminal desde " + pessoa.getUltimoAcessoEm());
        }

        LocalDateTime registradoEm = LocalDateTime.now();
        pessoa.setNome(textoObrigatorio(request.nome(), "O nome da pessoa é obrigatório"));
        pessoa.setDocumento(textoObrigatorio(request.documento(), "O documento da pessoa é obrigatório"));
        pessoa.setDocumentoNormalizado(documentoNormalizado);
        pessoa.setTipoPessoa(request.tipoPessoa());
        pessoa.setEmpresa(textoOpcional(request.empresa()));
        pessoa.setCracha(textoOpcional(request.cracha()));
        pessoa.setSituacao(SituacaoPessoaAcesso.DENTRO);
        pessoa.setUltimoAcessoEm(registradoEm);
        pessoa.setUltimoPontoAcesso(textoObrigatorio(request.pontoAcesso(), "O ponto de acesso é obrigatório"));
        PessoaAcesso pessoaSalva = pessoaAcessoRepository.saveAndFlush(pessoa);

        MovimentacaoPessoaAcesso movimentacao = novaMovimentacao(
                pessoaSalva,
                DirecaoMovimentacaoPessoa.ENTRADA,
                request.pontoAcesso(),
                request.motivo(),
                request.origemAcao(),
                request.correlationId(),
                registradoEm,
                null);
        return mapear(movimentacaoRepository.saveAndFlush(movimentacao));
    }

    public MovimentacaoPessoaDTO registrarSaida(SaidaPessoaRequest request) {
        try {
            return registrarSaidaSerializada(request);
        } catch (OptimisticLockingFailureException | PessimisticLockingFailureException exception) {
            throw conflitoConcorrencia(exception);
        }
    }

    private MovimentacaoPessoaDTO registrarSaidaSerializada(SaidaPessoaRequest request) {
        String documentoNormalizado = normalizarDocumento(request.documento());
        PessoaAcesso pessoa = pessoaAcessoRepository.findByDocumentoNormalizado(documentoNormalizado)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Não existe cadastro de acesso para o documento informado"));

        if (!SituacaoPessoaAcesso.DENTRO.equals(pessoa.getSituacao())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A pessoa não possui entrada aberta no terminal");
        }

        LocalDateTime registradoEm = LocalDateTime.now();
        long permanenciaMinutos = calcularPermanencia(pessoa.getUltimoAcessoEm(), registradoEm);
        pessoa.setSituacao(SituacaoPessoaAcesso.FORA);
        pessoa.setUltimoAcessoEm(registradoEm);
        pessoa.setUltimoPontoAcesso(textoObrigatorio(request.pontoAcesso(), "O ponto de acesso é obrigatório"));
        PessoaAcesso pessoaSalva = pessoaAcessoRepository.saveAndFlush(pessoa);

        MovimentacaoPessoaAcesso movimentacao = novaMovimentacao(
                pessoaSalva,
                DirecaoMovimentacaoPessoa.SAIDA,
                request.pontoAcesso(),
                request.motivo(),
                request.origemAcao(),
                request.correlationId(),
                registradoEm,
                permanenciaMinutos);
        return mapear(movimentacaoRepository.saveAndFlush(movimentacao));
    }

    @Transactional(readOnly = true)
    public List<PessoaPresenteDTO> listarPresentes() {
        LocalDateTime agora = LocalDateTime.now();
        return pessoaAcessoRepository.findBySituacaoOrderByUltimoAcessoEmAsc(SituacaoPessoaAcesso.DENTRO)
                .stream()
                .map(pessoa -> new PessoaPresenteDTO(
                        pessoa.getId(),
                        pessoa.getNome(),
                        pessoa.getDocumento(),
                        pessoa.getTipoPessoa(),
                        pessoa.getEmpresa(),
                        pessoa.getCracha(),
                        pessoa.getUltimoAcessoEm(),
                        pessoa.getUltimoPontoAcesso(),
                        calcularPermanencia(pessoa.getUltimoAcessoEm(), agora)))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ResumoAcessoPessoasDTO obterResumo() {
        List<PessoaAcesso> presentes = pessoaAcessoRepository
                .findBySituacaoOrderByUltimoAcessoEmAsc(SituacaoPessoaAcesso.DENTRO);
        Map<TipoPessoaAcesso, Long> presentesPorTipo = new EnumMap<>(TipoPessoaAcesso.class);
        for (PessoaAcesso pessoa : presentes) {
            presentesPorTipo.merge(pessoa.getTipoPessoa(), 1L, Long::sum);
        }
        return new ResumoAcessoPessoasDTO(presentes.size(), presentesPorTipo, LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public List<MovimentacaoPessoaDTO> listarMovimentacoes(String documento, int limite) {
        int limiteSeguro = Math.max(1, Math.min(limite, LIMITE_MAXIMO_HISTORICO));
        Pageable pageable = PageRequest.of(0, limiteSeguro);
        Page<MovimentacaoPessoaAcesso> pagina;
        if (StringUtils.hasText(documento)) {
            pagina = movimentacaoRepository.findByPessoa_DocumentoNormalizadoOrderByRegistradoEmDesc(
                    normalizarDocumento(documento),
                    pageable);
        } else {
            pagina = movimentacaoRepository.findAllByOrderByRegistradoEmDesc(pageable);
        }
        return pagina.getContent().stream().map(this::mapear).collect(Collectors.toList());
    }

    private MovimentacaoPessoaAcesso novaMovimentacao(PessoaAcesso pessoa,
                                                        DirecaoMovimentacaoPessoa direcao,
                                                        String pontoAcesso,
                                                        String motivo,
                                                        String origemAcao,
                                                        String correlationId,
                                                        LocalDateTime registradoEm,
                                                        Long permanenciaMinutos) {
        MovimentacaoPessoaAcesso movimentacao = new MovimentacaoPessoaAcesso();
        movimentacao.setPessoa(pessoa);
        movimentacao.setDirecao(direcao);
        movimentacao.setPontoAcesso(textoObrigatorio(pontoAcesso, "O ponto de acesso é obrigatório"));
        movimentacao.setMotivo(textoOpcional(motivo));
        movimentacao.setRegistradoEm(registradoEm);
        movimentacao.setUsuarioResponsavel(obterOperadorAtual());
        movimentacao.setOrigemAcao(textoOpcional(origemAcao));
        movimentacao.setCorrelationId(textoOpcional(correlationId));
        movimentacao.setPermanenciaMinutos(permanenciaMinutos);
        return movimentacao;
    }

    private MovimentacaoPessoaDTO mapear(MovimentacaoPessoaAcesso movimentacao) {
        PessoaAcesso pessoa = movimentacao.getPessoa();
        return new MovimentacaoPessoaDTO(
                movimentacao.getId(),
                pessoa.getId(),
                pessoa.getNome(),
                pessoa.getDocumento(),
                pessoa.getTipoPessoa(),
                pessoa.getEmpresa(),
                pessoa.getCracha(),
                movimentacao.getDirecao(),
                movimentacao.getPontoAcesso(),
                movimentacao.getMotivo(),
                movimentacao.getRegistradoEm(),
                movimentacao.getUsuarioResponsavel(),
                movimentacao.getOrigemAcao(),
                movimentacao.getCorrelationId(),
                movimentacao.getPermanenciaMinutos());
    }

    private boolean violouConstraint(DataIntegrityViolationException exception, String constraintEsperada) {
        Throwable causa = exception;
        while (causa != null) {
            if (causa instanceof ConstraintViolationException) {
                String constraintEncontrada = ((ConstraintViolationException) causa).getConstraintName();
                if (constraintEncontrada != null && constraintEsperada.equalsIgnoreCase(constraintEncontrada)) {
                    return true;
                }
            }
            String mensagem = causa.getMessage();
            if (mensagem != null
                    && mensagem.toLowerCase(Locale.ROOT).contains(constraintEsperada.toLowerCase(Locale.ROOT))) {
                return true;
            }
            causa = causa.getCause();
        }
        return false;
    }

    private ResponseStatusException conflitoConcorrencia(Throwable causa) {
        return new ResponseStatusException(HttpStatus.CONFLICT, MENSAGEM_CONFLITO_CONCORRENCIA, causa);
    }

    private String normalizarDocumento(String documento) {
        String normalizado = textoObrigatorio(documento, "O documento da pessoa é obrigatório")
                .replaceAll("[^A-Za-z0-9]", "")
                .toUpperCase(Locale.ROOT);
        if (!StringUtils.hasText(normalizado)) {
            throw new IllegalArgumentException("O documento informado não possui caracteres válidos");
        }
        return normalizado;
    }

    private String textoObrigatorio(String valor, String mensagem) {
        if (!StringUtils.hasText(valor)) {
            throw new IllegalArgumentException(mensagem);
        }
        return valor.trim();
    }

    private String textoOpcional(String valor) {
        return StringUtils.hasText(valor) ? valor.trim() : null;
    }

    private long calcularPermanencia(LocalDateTime inicio, LocalDateTime fim) {
        if (inicio == null || fim == null) {
            return 0L;
        }
        return Math.max(0L, Duration.between(inicio, fim).toMinutes());
    }

    private String obterOperadorAtual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !StringUtils.hasText(authentication.getName())) {
            return "SISTEMA";
        }
        return authentication.getName();
    }
}
