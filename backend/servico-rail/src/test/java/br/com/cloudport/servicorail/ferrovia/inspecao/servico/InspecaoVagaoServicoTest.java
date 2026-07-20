package br.com.cloudport.servicorail.ferrovia.inspecao.servico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicorail.ferrovia.inspecao.dto.InspecaoVagaoDto.Registro;
import br.com.cloudport.servicorail.ferrovia.inspecao.dto.InspecaoVagaoDto.Resposta;
import br.com.cloudport.servicorail.ferrovia.inspecao.modelo.InspecaoVagao;
import br.com.cloudport.servicorail.ferrovia.inspecao.modelo.InspecaoVagao.StatusInspecaoVagao;
import br.com.cloudport.servicorail.ferrovia.inspecao.repositorio.InspecaoVagaoRepositorio;
import br.com.cloudport.servicorail.ferrovia.listatrabalho.dto.OrdemMovimentacaoRespostaDto;
import br.com.cloudport.servicorail.ferrovia.listatrabalho.modelo.OrdemMovimentacao;
import br.com.cloudport.servicorail.ferrovia.listatrabalho.modelo.StatusOrdemMovimentacao;
import br.com.cloudport.servicorail.ferrovia.listatrabalho.modelo.TipoMovimentacaoOrdem;
import br.com.cloudport.servicorail.ferrovia.listatrabalho.repositorio.OrdemMovimentacaoRepositorio;
import br.com.cloudport.servicorail.ferrovia.modelo.OperacaoConteinerVisita;
import br.com.cloudport.servicorail.ferrovia.modelo.StatusOperacaoConteinerVisita;
import br.com.cloudport.servicorail.ferrovia.modelo.VagaoVisita;
import br.com.cloudport.servicorail.ferrovia.modelo.VisitaTrem;
import br.com.cloudport.servicorail.ferrovia.repositorio.VisitaTremRepositorio;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class InspecaoVagaoServicoTest {

    @Mock
    private InspecaoVagaoRepositorio inspecaoRepositorio;

    @Mock
    private VisitaTremRepositorio visitaTremRepositorio;

    @Mock
    private OrdemMovimentacaoRepositorio ordemMovimentacaoRepositorio;

    private InspecaoVagaoServico servico;

    @BeforeEach
    void configurar() {
        servico = new InspecaoVagaoServico(inspecaoRepositorio,
                visitaTremRepositorio,
                ordemMovimentacaoRepositorio);
    }

    @Test
    void deveAprovarVagaoQuandoChecklistEstaConforme() {
        VisitaTrem visita = visita();
        when(visitaTremRepositorio.findOneById(19L)).thenReturn(Optional.of(visita));
        when(inspecaoRepositorio.save(any(InspecaoVagao.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Resposta resposta = servico.registrar(19L, registroAprovado());

        assertEquals(StatusInspecaoVagao.APROVADA, resposta.getStatus());
        assertEquals("VAG-001", resposta.getIdentificadorVagao());
    }

    @Test
    void deveRetirarOrdemDaListaQuandoUltimaInspecaoFoiReprovada() {
        VisitaTrem visita = visita();
        OrdemMovimentacao ordem = new OrdemMovimentacao(visita,
                "MSCU0000001",
                TipoMovimentacaoOrdem.DESCARGA_TREM,
                StatusOrdemMovimentacao.PENDENTE);
        ordem.setIdentificadorVagao("VAG-001");
        InspecaoVagao inspecao = new InspecaoVagao();
        inspecao.setStatus(StatusInspecaoVagao.REPROVADA);
        when(visitaTremRepositorio.findOneById(19L)).thenReturn(Optional.of(visita));
        when(inspecaoRepositorio
                .findFirstByVisitaTremIdAndIdentificadorVagaoIgnoreCaseOrderByInspecionadoEmDesc(
                        19L, "VAG-001"))
                .thenReturn(Optional.of(inspecao));

        List<OrdemMovimentacaoRespostaDto> elegiveis = servico.filtrarOrdensElegiveis(
                19L,
                List.of(OrdemMovimentacaoRespostaDto.deEntidade(ordem)));

        assertEquals(Collections.emptyList(), elegiveis);
    }

    @Test
    void deveBloquearInicioQuandoVagaoNaoPossuiInspecao() {
        VisitaTrem visita = visita();
        OrdemMovimentacao ordem = new OrdemMovimentacao(visita,
                "MSCU0000001",
                TipoMovimentacaoOrdem.DESCARGA_TREM,
                StatusOrdemMovimentacao.PENDENTE);
        ordem.setIdentificadorVagao("VAG-001");
        when(ordemMovimentacaoRepositorio.findByIdAndVisitaTremId(8L, 19L))
                .thenReturn(Optional.of(ordem));
        when(visitaTremRepositorio.findOneById(19L)).thenReturn(Optional.of(visita));
        when(inspecaoRepositorio
                .findFirstByVisitaTremIdAndIdentificadorVagaoIgnoreCaseOrderByInspecionadoEmDesc(
                        19L, "VAG-001"))
                .thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
                () -> servico.validarElegibilidadeOrdem(19L, 8L));
    }

    private Registro registroAprovado() {
        Registro dto = new Registro();
        dto.setIdentificadorVagao("VAG-001");
        dto.setRodasAprovadas(true);
        dto.setFreiosAprovados(true);
        dto.setEngatesAprovados(true);
        dto.setEstruturaAprovada(true);
        dto.setLacresAprovados(true);
        dto.setResponsavel("Inspetor ferroviário");
        dto.setDefeitos(Collections.emptyList());
        return dto;
    }

    private VisitaTrem visita() {
        VisitaTrem visita = new VisitaTrem();
        visita.setId(19L);
        visita.setIdentificadorTrem("MRS-019");
        visita.setOperadoraFerroviaria("MRS Logística");
        visita.definirListaVagoes(List.of(new VagaoVisita(1, "VAG-001", "PRANCHA", 2)));
        visita.definirListaDescarga(List.of(new OperacaoConteinerVisita(
                "MSCU0000001",
                StatusOperacaoConteinerVisita.PENDENTE,
                "VAG-001")));
        return visita;
    }
}
