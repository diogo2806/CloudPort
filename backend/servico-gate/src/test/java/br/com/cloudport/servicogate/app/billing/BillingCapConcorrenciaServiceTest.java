package br.com.cloudport.servicogate.app.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicogate.app.billing.dto.BillingCapDtos.FaturaDTO;
import br.com.cloudport.servicogate.app.billing.dto.BillingCapDtos.FaturaGeracaoRequest;
import br.com.cloudport.servicogate.app.billing.dto.BillingCapDtos.PagamentoRequest;
import br.com.cloudport.servicogate.app.configuracoes.TransportadoraRepository;
import br.com.cloudport.servicogate.exception.BusinessException;
import br.com.cloudport.servicogate.exception.ConflictException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

@ExtendWith(MockitoExtension.class)
class BillingCapConcorrenciaServiceTest {

    @Mock
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Mock
    private TransportadoraRepository transportadoraRepository;

    private BillingCapConcorrenciaServiceTeste service;

    @BeforeEach
    void setUp() {
        service = new BillingCapConcorrenciaServiceTeste(jdbcTemplate, transportadoraRepository);
        when(jdbcTemplate.queryForList(any(String.class), any(SqlParameterSource.class)))
                .thenReturn(List.of(Map.of("id", 1L)));
    }

    @Test
    void deveBloquearTransportadoraECobrancasETraduzirDisputaParaConflito() {
        service.falhaGeracao = new BusinessException(
                "Uma ou mais cobranças não pertencem à transportadora ou já foram faturadas.");
        FaturaGeracaoRequest request = new FaturaGeracaoRequest(
                1L,
                LocalDate.now().plusDays(1),
                List.of(10L));

        assertThatThrownBy(() -> service.gerarFatura(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Uma ou mais cobranças não pertencem à transportadora ou já foram faturadas.");

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, times(2)).queryForList(sql.capture(), any(SqlParameterSource.class));
        assertThat(sql.getAllValues())
                .allSatisfy(comando -> assertThat(comando).containsIgnoringCase("FOR UPDATE"));
    }

    @Test
    void deveBloquearFaturaETraduzirSaldoConsumidoParaConflito() {
        service.falhaPagamento = new BusinessException(
                "O pagamento não pode ser maior que o saldo da fatura.");
        PagamentoRequest request = new PagamentoRequest(
                new BigDecimal("100.00"),
                "PIX",
                "PAG-001",
                null);

        assertThatThrownBy(() -> service.registrarPagamento(20L, request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("O pagamento não pode ser maior que o saldo da fatura.");

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).queryForList(sql.capture(), any(SqlParameterSource.class));
        assertThat(sql.getValue())
                .containsIgnoringCase("billing_fatura")
                .containsIgnoringCase("FOR UPDATE");
    }

    private static final class BillingCapConcorrenciaServiceTeste extends BillingCapConcorrenciaService {

        private BusinessException falhaGeracao;
        private BusinessException falhaPagamento;

        private BillingCapConcorrenciaServiceTeste(NamedParameterJdbcTemplate jdbcTemplate,
                                                    TransportadoraRepository transportadoraRepository) {
            super(jdbcTemplate, transportadoraRepository);
        }

        @Override
        protected FaturaDTO gerarFaturaBase(FaturaGeracaoRequest request) {
            throw falhaGeracao;
        }

        @Override
        protected FaturaDTO registrarPagamentoBase(Long faturaId, PagamentoRequest request) {
            throw falhaPagamento;
        }
    }
}
