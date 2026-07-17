package br.com.cloudport.servicogate.app.gestor;

import br.com.cloudport.servicogate.app.gestor.dto.RetiradaDiretaNavioDTO;
import br.com.cloudport.servicogate.app.gestor.dto.RetiradaDiretaNavioRequest;
import br.com.cloudport.servicogate.exception.BusinessException;
import br.com.cloudport.servicogate.model.RetiradaDiretaNavio;
import br.com.cloudport.servicogate.model.enums.StatusRetiradaDiretaNavio;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class RetiradaDiretaNavioService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RetiradaDiretaNavioService.class);
    private static final String TIPO_CARGA_PADRAO = "TRATOR";

    private final RetiradaDiretaNavioRepository repository;

    public RetiradaDiretaNavioService(RetiradaDiretaNavioRepository repository) {
        this.repository = repository;
    }

    public RetiradaDiretaNavioDTO processar(RetiradaDiretaNavioRequest request) {
        validarConfirmacoes(request);

        String codigoAutorizacao = normalizarIdentificador(request.getCodigoAutorizacao());
        String identificadorCarga = normalizarIdentificador(request.getIdentificadorCarga());
        String visitaNavio = normalizarIdentificador(request.getVisitaNavio());

        RetiradaDiretaNavio existentePorAutorizacao = repository
                .findByCodigoAutorizacaoIgnoreCase(codigoAutorizacao)
                .orElse(null);
        if (existentePorAutorizacao != null) {
            if (identificadorCarga.equalsIgnoreCase(existentePorAutorizacao.getIdentificadorCarga())) {
                return toDTO(existentePorAutorizacao);
            }
            throw new BusinessException("Código de autorização já utilizado por outra carga");
        }

        repository.findByIdentificadorCargaIgnoreCase(identificadorCarga).ifPresent(existente -> {
            throw new BusinessException("A carga informada já saiu pelo gate na autorização "
                    + existente.getCodigoAutorizacao());
        });

        LocalDateTime saidaEm = resolverTimestamp(request.getTimestamp());
        RetiradaDiretaNavio retirada = new RetiradaDiretaNavio();
        retirada.setCodigoAutorizacao(codigoAutorizacao);
        retirada.setIdentificadorCarga(identificadorCarga);
        retirada.setTipoCarga(resolverTipoCarga(request.getTipoCarga()));
        retirada.setVisitaNavio(visitaNavio);
        retirada.setClienteNome(sanitizarTexto(request.getClienteNome()));
        retirada.setClienteDocumento(normalizarDocumento(request.getClienteDocumento()));
        retirada.setStatus(StatusRetiradaDiretaNavio.FINALIZADA);
        retirada.setSaidaEm(saidaEm);
        retirada.setOperador(obterOperadorAtual());
        retirada.setObservacao(sanitizarTexto(request.getObservacao()));

        RetiradaDiretaNavio salva = repository.saveAndFlush(retirada);
        LOGGER.info("event=gate.direct-vessel-release.completed retiradaId={} autorizacao={} carga={} visitaNavio={} operador={}",
                salva.getId(), salva.getCodigoAutorizacao(), salva.getIdentificadorCarga(),
                salva.getVisitaNavio(), salva.getOperador());
        return toDTO(salva);
    }

    @Transactional(readOnly = true)
    public Page<RetiradaDiretaNavioDTO> listar(Pageable pageable) {
        return repository.findAllByOrderBySaidaEmDesc(pageable).map(this::toDTO);
    }

    private void validarConfirmacoes(RetiradaDiretaNavioRequest request) {
        if (!Boolean.TRUE.equals(request.getDocumentosValidados())) {
            throw new BusinessException("A documentação da carga deve estar validada antes da saída");
        }
        if (!Boolean.TRUE.equals(request.getLiberacaoAduaneiraConfirmada())) {
            throw new BusinessException("A liberação aduaneira deve estar confirmada antes da saída");
        }
        if (!Boolean.TRUE.equals(request.getCargaDescarregada())) {
            throw new BusinessException("A carga ainda não foi confirmada como descarregada do navio");
        }
        if (!Boolean.TRUE.equals(request.getCondutorHabilitado())) {
            throw new BusinessException("O cliente deve possuir habilitação válida para conduzir a carga autopropelida");
        }
    }

    private LocalDateTime resolverTimestamp(LocalDateTime timestamp) {
        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime resolvido = timestamp != null ? timestamp : agora;
        if (resolvido.isAfter(agora.plusMinutes(5))) {
            throw new BusinessException("O horário de saída não pode estar no futuro");
        }
        return resolvido;
    }

    private String resolverTipoCarga(String tipoCarga) {
        if (!StringUtils.hasText(tipoCarga)) {
            return TIPO_CARGA_PADRAO;
        }
        return sanitizarTexto(tipoCarga).toUpperCase(Locale.ROOT);
    }

    private String normalizarIdentificador(String valor) {
        String normalizado = sanitizarTexto(valor).toUpperCase(Locale.ROOT);
        if (!StringUtils.hasText(normalizado)) {
            throw new BusinessException("Identificador obrigatório não foi informado");
        }
        return normalizado;
    }

    private String normalizarDocumento(String valor) {
        String normalizado = sanitizarTexto(valor).replaceAll("[^0-9A-Za-z]", "").toUpperCase(Locale.ROOT);
        if (!StringUtils.hasText(normalizado)) {
            throw new BusinessException("Documento do cliente é obrigatório");
        }
        return normalizado;
    }

    private String sanitizarTexto(String valor) {
        if (!StringUtils.hasText(valor)) {
            return null;
        }
        String normalizado = Normalizer.normalize(valor, Normalizer.Form.NFKC);
        return normalizado.replaceAll("[<>\"'`]", "")
                .replaceAll("[\\p{Cntrl}&&[^\\n\\t\\r]]", "")
                .trim();
    }

    private String obterOperadorAtual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !StringUtils.hasText(authentication.getName())) {
            return "SISTEMA";
        }
        return authentication.getName();
    }

    private RetiradaDiretaNavioDTO toDTO(RetiradaDiretaNavio entity) {
        StatusRetiradaDiretaNavio status = entity.getStatus();
        return new RetiradaDiretaNavioDTO(
                entity.getId(),
                entity.getCodigoAutorizacao(),
                entity.getIdentificadorCarga(),
                entity.getTipoCarga(),
                entity.getVisitaNavio(),
                entity.getClienteNome(),
                entity.getClienteDocumento(),
                status != null ? status.name() : null,
                status != null ? status.getDescricao() : null,
                entity.getSaidaEm(),
                entity.getOperador(),
                entity.getObservacao(),
                entity.getCreatedAt()
        );
    }
}
