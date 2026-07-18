package br.com.cloudport.servicogate.app.billing;

import br.com.cloudport.servicogate.app.billing.dto.BillingCapDtos.FaturaDTO;
import br.com.cloudport.servicogate.app.billing.dto.BillingCapDtos.FaturaGeracaoRequest;
import br.com.cloudport.servicogate.app.billing.dto.BillingCapDtos.PagamentoRequest;
import br.com.cloudport.servicogate.app.configuracoes.TransportadoraRepository;
import br.com.cloudport.servicogate.exception.BusinessException;
import br.com.cloudport.servicogate.exception.ConflictException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Primary
public class BillingCapConcorrenciaService extends BillingCapService {

    private static final String SEM_COBRANCAS_PENDENTES =
            "Não existem cobranças pendentes para gerar a fatura.";
    private static final String COBRANCAS_INDISPONIVEIS =
            "Uma ou mais cobranças não pertencem à transportadora ou já foram faturadas.";
    private static final String FATURA_NAO_ABERTA =
            "Somente faturas abertas podem receber pagamentos.";
    private static final String PAGAMENTO_ACIMA_SALDO =
            "O pagamento não pode ser maior que o saldo da fatura.";
    private static final String CONSTRAINT_COBRANCA_FATURA =
            "billing_fatura_item_cobranca_id_key";

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public BillingCapConcorrenciaService(NamedParameterJdbcTemplate jdbcTemplate,
                                         TransportadoraRepository transportadoraRepository) {
        super(jdbcTemplate, transportadoraRepository);
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public FaturaDTO gerarFatura(FaturaGeracaoRequest request) {
        try {
            bloquearTransportadora(request.transportadoraId());
            bloquearCobrancas(request);
            return gerarFaturaBase(request);
        } catch (ConflictException ex) {
            throw ex;
        } catch (BusinessException ex) {
            if (ehConflitoDeFaturamento(ex.getMessage())) {
                throw new ConflictException(ex.getMessage(), ex);
            }
            throw ex;
        } catch (DataIntegrityViolationException ex) {
            if (ehColisaoDeCobranca(ex)) {
                throw new ConflictException(
                        "Uma ou mais cobranças já foram faturadas por outra operação.", ex);
            }
            throw ex;
        } catch (PessimisticLockingFailureException ex) {
            throw new ConflictException(
                    "As cobranças estão sendo faturadas por outra operação.", ex);
        }
    }

    @Override
    @Transactional
    public FaturaDTO registrarPagamento(Long faturaId, PagamentoRequest request) {
        try {
            bloquearFatura(faturaId);
            return registrarPagamentoBase(faturaId, request);
        } catch (ConflictException ex) {
            throw ex;
        } catch (BusinessException ex) {
            if (ehConflitoDePagamento(ex.getMessage())) {
                throw new ConflictException(ex.getMessage(), ex);
            }
            throw ex;
        } catch (PessimisticLockingFailureException ex) {
            throw new ConflictException(
                    "O saldo da fatura está sendo atualizado por outra operação.", ex);
        }
    }

    protected FaturaDTO gerarFaturaBase(FaturaGeracaoRequest request) {
        return super.gerarFatura(request);
    }

    protected FaturaDTO registrarPagamentoBase(Long faturaId, PagamentoRequest request) {
        return super.registrarPagamento(faturaId, request);
    }

    private void bloquearTransportadora(Long transportadoraId) {
        jdbcTemplate.queryForList("""
                SELECT id
                  FROM transportadora
                 WHERE id = :transportadoraId
                 FOR UPDATE
                """, new MapSqlParameterSource("transportadoraId", transportadoraId));
    }

    private void bloquearCobrancas(FaturaGeracaoRequest request) {
        List<Long> cobrancaIds = request.cobrancaIds() == null
                ? Collections.emptyList()
                : request.cobrancaIds().stream()
                        .filter(id -> id != null && id > 0)
                        .distinct()
                        .collect(Collectors.toList());

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("transportadoraId", request.transportadoraId());
        if (cobrancaIds.isEmpty()) {
            jdbcTemplate.queryForList("""
                    SELECT id
                      FROM billing_cobranca
                     WHERE transportadora_id = :transportadoraId
                       AND status = 'PENDENTE'
                     ORDER BY id
                     FOR UPDATE
                    """, parameters);
            return;
        }

        parameters.addValue("cobrancaIds", cobrancaIds);
        jdbcTemplate.queryForList("""
                SELECT id
                  FROM billing_cobranca
                 WHERE transportadora_id = :transportadoraId
                   AND id IN (:cobrancaIds)
                 ORDER BY id
                 FOR UPDATE
                """, parameters);
    }

    private void bloquearFatura(Long faturaId) {
        jdbcTemplate.queryForList("""
                SELECT id
                  FROM billing_fatura
                 WHERE id = :faturaId
                 FOR UPDATE
                """, new MapSqlParameterSource("faturaId", faturaId));
    }

    private boolean ehConflitoDeFaturamento(String mensagem) {
        return SEM_COBRANCAS_PENDENTES.equals(mensagem)
                || COBRANCAS_INDISPONIVEIS.equals(mensagem);
    }

    private boolean ehConflitoDePagamento(String mensagem) {
        return FATURA_NAO_ABERTA.equals(mensagem)
                || PAGAMENTO_ACIMA_SALDO.equals(mensagem);
    }

    private boolean ehColisaoDeCobranca(Throwable erro) {
        Throwable atual = erro;
        while (atual != null) {
            String mensagem = atual.getMessage();
            if (mensagem != null && mensagem.contains(CONSTRAINT_COBRANCA_FATURA)) {
                return true;
            }
            atual = atual.getCause();
        }
        return false;
    }
}
