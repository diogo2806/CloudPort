package br.com.cloudport.servicogate.app.operacional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.AccessRuleRequest;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.BillOfLadingRequest;
import br.com.cloudport.servicogate.exception.BusinessException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@ExtendWith(MockitoExtension.class)
class GateComplementarServiceTest {

    @Mock
    private NamedParameterJdbcTemplate jdbcTemplate;

    private GateComplementarService service;

    @BeforeEach
    void setUp() {
        service = new GateComplementarService(jdbcTemplate);
    }

    @Test
    void deveRejeitarBillOfLadingComValidadeInvalida() {
        LocalDateTime inicio = LocalDateTime.of(2026, 7, 20, 10, 0);
        BillOfLadingRequest request = new BillOfLadingRequest(
                "BL-001",
                "ARMADOR",
                "VIAGEM-01",
                "CLIENTE",
                10,
                inicio,
                inicio.minusMinutes(1),
                null);

        assertThatThrownBy(() -> service.salvarBillOfLading(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("validade final");
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void deveRejeitarRegraComValidadeInvalidaAntesDoBanco() {
        LocalDateTime inicio = LocalDateTime.of(2026, 7, 20, 10, 0);
        AccessRuleRequest request = new AccessRuleRequest(
                1L,
                "MOTORISTA",
                1L,
                "BLOQUEIO",
                "Documento vencido",
                inicio,
                inicio.minusMinutes(1),
                true);

        assertThatThrownBy(() -> service.salvarRegra(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("validade final");
        verifyNoInteractions(jdbcTemplate);
    }
}