package br.com.cloudport.runtime.integracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicogate.porta.navio.EmbarqueDiretoNavioPorta;
import br.com.cloudport.serviconavio.estiva.dto.EmbarqueDiretoGateResultadoDTO;
import br.com.cloudport.serviconavio.estiva.servico.PlanoEstivaServico;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmbarqueDiretoNavioLocalAdapterTest {

    @Mock
    private PlanoEstivaServico planoEstivaServico;

    @Test
    void deveChamarModuloNavioSemHttpNemPatio() {
        LocalDateTime horario = LocalDateTime.of(2026, 7, 17, 19, 30);
        when(planoEstivaServico.embarcarDiretoDoGate(30L, "MSCU1234567", horario))
                .thenReturn(new EmbarqueDiretoGateResultadoDTO(
                        30L, 40L, "MSCU1234567", 2, 4, 6, horario));
        EmbarqueDiretoNavioLocalAdapter adapter = new EmbarqueDiretoNavioLocalAdapter(planoEstivaServico);

        EmbarqueDiretoNavioPorta.Resultado resultado = adapter.embarcar(
                new EmbarqueDiretoNavioPorta.Comando(30L, "MSCU1234567", horario));

        assertEquals(40L, resultado.getPlanoEstivaId());
        assertEquals(6, resultado.getCamada());
        verify(planoEstivaServico).embarcarDiretoDoGate(30L, "MSCU1234567", horario);
    }
}
