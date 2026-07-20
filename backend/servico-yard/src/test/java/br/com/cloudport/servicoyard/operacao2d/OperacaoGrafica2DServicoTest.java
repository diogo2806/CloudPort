package br.com.cloudport.servicoyard.operacao2d;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicoyard.operacao2d.OperacaoGrafica2DDtos.ComandoResponse;
import br.com.cloudport.servicoyard.operacao2d.OperacaoGrafica2DDtos.RegistrarComandoRequest;
import br.com.cloudport.servicoyard.operacao2d.OperacaoGrafica2DDtos.SalvarWorkspaceRequest;
import br.com.cloudport.servicoyard.operacao2d.OperacaoGrafica2DDtos.WorkspaceResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OperacaoGrafica2DServicoTest {

    @Mock
    private ComandoOperacaoGrafica2DRepositorio comandoRepositorio;

    @Mock
    private WorkspaceGrafico2DRepositorio workspaceRepositorio;

    private ObjectMapper objectMapper;
    private OperacaoGrafica2DServico servico;

    @BeforeEach
    void preparar() {
        objectMapper = new ObjectMapper();
        servico = new OperacaoGrafica2DServico(comandoRepositorio, workspaceRepositorio, objectMapper);
    }

    @Test
    void deveReutilizarComandoQuandoCommandIdJaFoiProcessado() throws Exception {
        ComandoOperacaoGrafica2D existente = new ComandoOperacaoGrafica2D();
        existente.setCommandId("cmd-001");
        existente.setTipo("APLICAR_FLUXO_PLANEJAMENTO");
        existente.setStatus("CONFIRMADO");
        existente.setMotivo("Planejamento validado");
        existente.setPayloadJson("{\"moves\":[{\"unitId\":\"CONT-1\"}]}");
        existente.setSolicitadoPor("diogo");
        existente.prepararInclusao();
        when(comandoRepositorio.findByCommandId("cmd-001")).thenReturn(Optional.of(existente));

        RegistrarComandoRequest request = new RegistrarComandoRequest(
                "cmd-001",
                "APLICAR_FLUXO_PLANEJAMENTO",
                "Planejamento validado",
                objectMapper.readTree("{\"moves\":[{\"unitId\":\"CONT-1\"}]}"));

        ComandoResponse response = servico.registrarComando(request, "diogo");

        assertEquals("cmd-001", response.commandId());
        assertEquals("CONFIRMADO", response.status());
        verify(comandoRepositorio, never()).saveAndFlush(any());
    }

    @Test
    void deveCriarNovaVersaoDoWorkspaceSemSobrescreverAnterior() throws Exception {
        WorkspaceGrafico2D anterior = new WorkspaceGrafico2D();
        anterior.setNome("Operação turno A");
        anterior.setEscopo("INDIVIDUAL");
        anterior.setProprietario("diogo");
        anterior.setVersao(2L);
        anterior.setConteudoJson("{}");
        anterior.prepararInclusao();
        when(workspaceRepositorio.findTopByNomeAndEscopoAndProprietarioOrderByVersaoDesc(
                "Operação turno A", "INDIVIDUAL", "diogo")).thenReturn(Optional.of(anterior));
        when(workspaceRepositorio.saveAndFlush(any())).thenAnswer(invocation -> {
            WorkspaceGrafico2D workspace = invocation.getArgument(0);
            workspace.prepararInclusao();
            return workspace;
        });

        WorkspaceResponse response = servico.salvarWorkspace(
                new SalvarWorkspaceRequest(
                        "Operação turno A",
                        "INDIVIDUAL",
                        null,
                        objectMapper.readTree("{\"filters\":{\"state\":\"BLOQUEADO\"}}")),
                "diogo",
                Set.of("ROLE_PLANEJADOR"));

        assertEquals(3L, response.version());
        assertEquals("diogo", response.owner());
    }

    @Test
    void deveBloquearWorkspacePadraoSemPermissaoAdministrativa() throws Exception {
        SalvarWorkspaceRequest request = new SalvarWorkspaceRequest(
                "Padrão do terminal",
                "PADRAO",
                null,
                objectMapper.readTree("{}"));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> servico.salvarWorkspace(request, "planejador", Set.of("ROLE_PLANEJADOR")));

        assertEquals("Somente ADMIN_PORTO pode publicar o workspace padrão.", exception.getMessage());
    }

    @Test
    void deveBloquearFluxoSemMovimentos() throws Exception {
        RegistrarComandoRequest request = new RegistrarComandoRequest(
                "cmd-sem-movimento",
                "APLICAR_FLUXO_PLANEJAMENTO",
                "Teste",
                objectMapper.readTree("{\"moves\":[]}"));
        when(comandoRepositorio.findByCommandId("cmd-sem-movimento")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> servico.registrarComando(request, "diogo"));
    }
}
