package br.com.cloudport.servicogate.app.operacional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.BookingRequest;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.PreadviceRequest;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.TruckVisitRequest;
import br.com.cloudport.servicogate.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@ExtendWith(MockitoExtension.class)
class GateOperacionalServiceTest {

    @Mock
    private NamedParameterJdbcTemplate jdbcTemplate;

    private GateOperacionalService service;

    @BeforeEach
    void setUp() {
        service = new GateOperacionalService(jdbcTemplate, new ObjectMapper());
    }

    @Test
    void deveRejeitarTruckVisitSemTransacoesAntesDeAcessarBanco() {
        TruckVisitRequest request = new TruckVisitRequest(
                null,
                1L,
                1L,
                null,
                1L,
                1L,
                1L,
                List.of(),
                "operador",
                "corr-1");

        assertThatThrownBy(() -> service.criarVisita(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ao menos uma transação");
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void deveRejeitarBookingComValidadeFinalAnteriorAoInicio() {
        LocalDateTime inicio = LocalDateTime.of(2026, 7, 20, 10, 0);
        BookingRequest request = new BookingRequest(
                "BKG-001",
                null,
                "ARMADOR",
                "VIAGEM-01",
                10,
                inicio,
                inicio.minusHours(1),
                null);

        assertThatThrownBy(() -> service.salvarBooking(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("validade final");
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void deveExigirBookingOuOrdemNoPreAviso() {
        PreadviceRequest request = new PreadviceRequest(
                "EXPORTACAO",
                "PRE-001",
                null,
                null,
                "ABCD1234567",
                "22G1",
                null);

        assertThatThrownBy(() -> service.salvarPreadvice(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("booking ou ordem");
        verifyNoInteractions(jdbcTemplate);
    }
}