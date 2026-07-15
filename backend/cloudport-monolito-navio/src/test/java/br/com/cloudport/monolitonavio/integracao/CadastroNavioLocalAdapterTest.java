package br.com.cloudport.monolitonavio.integracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cloudport.serviconavio.navio.dto.NavioDetalheDTO;
import br.com.cloudport.serviconavio.navio.dto.NavioResumoDTO;
import br.com.cloudport.serviconavio.navio.servico.NavioServico;
import br.com.cloudport.serviconaviosiderurgico.porta.NavioCanonico;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CadastroNavioLocalAdapterTest {

    @Mock
    private NavioServico navioServico;

    private CadastroNavioLocalAdapter adapter;

    @BeforeEach
    void configurar() {
        adapter = new CadastroNavioLocalAdapter(navioServico);
    }

    @Test
    void deveBuscarCadastroCanonicoDiretamenteNoModuloNavio() {
        when(navioServico.buscarDetalhe(10L)).thenReturn(detalhe());

        NavioCanonico navio = adapter.buscarPorId(10L);

        assertEquals(10L, navio.identificador());
        assertEquals("IMO1234567", navio.codigoImo());
        verify(navioServico).buscarDetalhe(10L);
    }

    @Test
    void deveResolverImoSemChamadaHttpEntreModulos() {
        when(navioServico.listarResumo()).thenReturn(List.of(
                new NavioResumoDTO(10L, "Cloud Steel", "IMO1234567", "Cloud Shipping", 1000)
        ));
        when(navioServico.buscarDetalhe(10L)).thenReturn(detalhe());

        NavioCanonico navio = adapter.buscarPorImo(" imo1234567 ");

        assertEquals(10L, navio.identificador());
        verify(navioServico).listarResumo();
        verify(navioServico).buscarDetalhe(10L);
    }

    private NavioDetalheDTO detalhe() {
        return new NavioDetalheDTO(
                10L,
                "Cloud Steel",
                "IMO1234567",
                "Brasil",
                "Cloud Shipping",
                1000,
                new BigDecimal("220.50"),
                new BigDecimal("12.20"),
                "CLOUD1"
        );
    }
}
