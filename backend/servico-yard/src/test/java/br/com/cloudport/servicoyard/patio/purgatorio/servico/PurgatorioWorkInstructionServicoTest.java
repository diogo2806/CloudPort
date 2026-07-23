package br.com.cloudport.servicoyard.patio.purgatorio.servico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.OrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.repositorio.OrdemTrabalhoPatioRepositorio;
import br.com.cloudport.servicoyard.patio.purgatorio.dto.ComandoPurgatorioWorkInstructionDto;
import br.com.cloudport.servicoyard.patio.purgatorio.modelo.CasoPurgatorioWorkInstruction;
import br.com.cloudport.servicoyard.patio.purgatorio.modelo.CausaPurgatorioWorkInstruction;
import br.com.cloudport.servicoyard.patio.purgatorio.modelo.EstadoPurgatorioWorkInstruction;
import br.com.cloudport.servicoyard.patio.purgatorio.modelo.SeveridadePurgatorioWorkInstruction;
import br.com.cloudport.servicoyard.patio.purgatorio.repositorio.CasoPurgatorioWorkInstructionRepositorio;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class PurgatorioWorkInstructionServicoTest {

    @Mock
    private CasoPurgatorioWorkInstructionRepositorio repositorio;
    @Mock
    private OrdemTrabalhoPatioRepositorio ordemRepositorio;

    private PurgatorioWorkInstructionServico servico;

    @BeforeEach
    void configurar() {
        servico = new PurgatorioWorkInstructionServico(repositorio, ordemRepositorio);
    }

    @Test
    void deveBloquearDispatchQuandoExistirCasoAberto() {
        when(repositorio.existsByWorkQueueIdAndEstadoIn(any(), any())).thenReturn(true);
        assertThrows(ResponseStatusException.class, () -> servico.validarDispatch(7L));
    }

    @Test
    void deveReutilizarCasoComMesmaChaveIdempotente() {
        CasoPurgatorioWorkInstruction existente = new CasoPurgatorioWorkInstruction();
        when(repositorio.findByChaveIdempotencia("abc")).thenReturn(Optional.of(existente));
        assertSame(existente, servico.abrir(comando("abc")));
    }

    @Test
    void deveLiberarReentradaSomenteAposRevalidacaoBemSucedida() {
        CasoPurgatorioWorkInstruction caso = new CasoPurgatorioWorkInstruction();
        caso.setId(1L);
        caso.setEstado(EstadoPurgatorioWorkInstruction.AGUARDANDO_REVALIDACAO);
        caso.setHistorico("aberto");
        when(repositorio.findById(1L)).thenReturn(Optional.of(caso));
        when(repositorio.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ComandoPurgatorioWorkInstructionDto dto = comando("revalidar");
        dto.setRevalidacaoBemSucedida(true);
        CasoPurgatorioWorkInstruction resolvido = servico.revalidar(1L, dto);
        assertEquals(EstadoPurgatorioWorkInstruction.RESOLVIDO, resolvido.getEstado());
    }

    private ComandoPurgatorioWorkInstructionDto comando(String chave) {
        ComandoPurgatorioWorkInstructionDto dto = new ComandoPurgatorioWorkInstructionDto();
        dto.setOrdemTrabalhoPatioId(10L);
        dto.setWorkQueueId(7L);
        dto.setCausa(CausaPurgatorioWorkInstruction.INVENTARIO);
        dto.setSeveridade(SeveridadePurgatorioWorkInstruction.ALTA);
        dto.setChaveIdempotencia(chave);
        dto.setMotivo("Divergencia operacional");
        dto.setUsuario("teste");
        return dto;
    }
}
