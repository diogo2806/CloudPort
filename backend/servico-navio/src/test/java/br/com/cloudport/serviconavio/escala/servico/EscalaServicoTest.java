package br.com.cloudport.serviconavio.escala.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cloudport.serviconavio.comum.validacao.SanitizadorEntrada;
import br.com.cloudport.serviconavio.escala.dto.AtualizacaoEscalaDTO;
import br.com.cloudport.serviconavio.escala.dto.CadastroEscalaDTO;
import br.com.cloudport.serviconavio.escala.dto.EscalaDetalheDTO;
import br.com.cloudport.serviconavio.escala.entidade.Escala;
import br.com.cloudport.serviconavio.escala.entidade.FaseEscala;
import br.com.cloudport.serviconavio.escala.entidade.ProntidaoBerco;
import br.com.cloudport.serviconavio.escala.repositorio.EscalaRepositorio;
import br.com.cloudport.serviconavio.escala.repositorio.ProntidaoBercoRepositorio;
import br.com.cloudport.serviconavio.navio.entidade.Navio;
import br.com.cloudport.serviconavio.navio.repositorio.NavioRepositorio;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class EscalaServicoTest {

    private EscalaRepositorio escalaRepositorio;
    private NavioRepositorio navioRepositorio;
    private ProntidaoBercoRepositorio prontidaoBercoRepositorio;
    private SanitizadorEntrada sanitizadorEntrada;
    private EscalaServico escalaServico;

    @BeforeEach
    void preparar() {
        escalaRepositorio = mock(EscalaRepositorio.class);
        navioRepositorio = mock(NavioRepositorio.class);
        prontidaoBercoRepositorio = mock(ProntidaoBercoRepositorio.class);
        sanitizadorEntrada = mock(SanitizadorEntrada.class);
        when(sanitizadorEntrada.limparTextoObrigatorio(anyString(), anyString()))
                .thenAnswer(invocacao -> invocacao.getArgument(0));
        when(sanitizadorEntrada.limparTexto(anyString()))
                .thenAnswer(invocacao -> invocacao.getArgument(0));
        when(escalaRepositorio.save(any(Escala.class))).thenAnswer(invocacao -> invocacao.getArgument(0));
        escalaServico = new EscalaServico(
                escalaRepositorio,
                navioRepositorio,
                prontidaoBercoRepositorio,
                sanitizadorEntrada);
    }

    private Navio navio() {
        Navio navio = new Navio();
        navio.setIdentificador(10L);
        navio.setNome("Navio Teste");
        navio.setCodigoImo("IMO1234567");
        return navio;
    }

    private Escala escalaComFase(FaseEscala fase) {
        Escala escala = new Escala();
        escala.setId(1L);
        escala.setNavio(navio());
        escala.setViagemEntrada("V001N");
        escala.setFase(fase);
        escala.setChegadaPrevista(LocalDateTime.of(2026, 5, 23, 8, 0));
        return escala;
    }

    private ProntidaoBerco prontidaoCompleta(Escala escala) {
        ProntidaoBerco prontidao = new ProntidaoBerco();
        prontidao.setEscala(escala);
        prontidao.setBercoConfirmado(true);
        prontidao.setCaladoConfirmado(true);
        prontidao.setDefensasConfirmadas(true);
        prontidao.setAmarracaoConfirmada(true);
        prontidao.setAcessoConfirmado(true);
        prontidao.setRecursosConfirmados(true);
        prontidao.setRestricoesAvaliadas(true);
        prontidao.setLiberacoesConfirmadas(true);
        return prontidao;
    }

    @Test
    void registrarIniciaFasePrevistaENormalizaViagem() {
        when(navioRepositorio.findById(10L)).thenReturn(Optional.of(navio()));
        CadastroEscalaDTO dto = new CadastroEscalaDTO();
        dto.setViagemEntrada("v001n");
        dto.setChegadaPrevista(LocalDateTime.of(2026, 5, 23, 8, 0));
        dto.setPartidaPrevista(LocalDateTime.of(2026, 5, 24, 8, 0));

        EscalaDetalheDTO resultado = escalaServico.registrar(10L, dto);

        assertThat(resultado.getFase()).isEqualTo(FaseEscala.PREVISTA);
        assertThat(resultado.getViagemEntrada()).isEqualTo("V001N");
    }

    @Test
    void registrarRejeitaPartidaAnteriorAChegada() {
        when(navioRepositorio.findById(10L)).thenReturn(Optional.of(navio()));
        CadastroEscalaDTO dto = new CadastroEscalaDTO();
        dto.setViagemEntrada("V001N");
        dto.setChegadaPrevista(LocalDateTime.of(2026, 5, 23, 8, 0));
        dto.setPartidaPrevista(LocalDateTime.of(2026, 5, 22, 8, 0));

        assertThatThrownBy(() -> escalaServico.registrar(10L, dto))
                .isInstanceOf(ResponseStatusException.class);

        verify(escalaRepositorio, never()).save(any());
    }

    @Test
    void registrarRejeitaNavioInexistente() {
        when(navioRepositorio.findById(99L)).thenReturn(Optional.empty());
        CadastroEscalaDTO dto = new CadastroEscalaDTO();
        dto.setViagemEntrada("V001N");
        dto.setChegadaPrevista(LocalDateTime.of(2026, 5, 23, 8, 0));

        assertThatThrownBy(() -> escalaServico.registrar(99L, dto))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void avancarFaseParaAtracadoCarimbaTemposEfetivos() {
        when(escalaRepositorio.findLockedById(1L))
                .thenReturn(Optional.of(escalaComFase(FaseEscala.PREVISTA)));

        EscalaDetalheDTO resultado = escalaServico.avancarFase(1L, FaseEscala.ATRACADO);

        assertThat(resultado.getFase()).isEqualTo(FaseEscala.ATRACADO);
        assertThat(resultado.getChegadaEfetiva()).isNotNull();
        assertThat(resultado.getAtracacaoEfetiva()).isNotNull();
        assertThat(resultado.getPartidaEfetiva()).isNull();
    }

    @Test
    void deveBloquearInicioDaOperacaoSemProntidao() {
        when(escalaRepositorio.findLockedById(1L))
                .thenReturn(Optional.of(escalaComFase(FaseEscala.ATRACADO)));
        when(prontidaoBercoRepositorio.findTopByEscalaIdOrderByVersaoChecklistDesc(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> escalaServico.avancarFase(1L, FaseEscala.OPERANDO))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("checklist de prontidão");

        verify(escalaRepositorio, never()).save(any());
    }

    @Test
    void deveIniciarOperacaoQuandoProntidaoEstiverCompleta() {
        Escala escala = escalaComFase(FaseEscala.ATRACADO);
        when(escalaRepositorio.findLockedById(1L)).thenReturn(Optional.of(escala));
        when(prontidaoBercoRepositorio.findTopByEscalaIdOrderByVersaoChecklistDesc(1L))
                .thenReturn(Optional.of(prontidaoCompleta(escala)));

        EscalaDetalheDTO resultado = escalaServico.avancarFase(1L, FaseEscala.OPERANDO);

        assertThat(resultado.getFase()).isEqualTo(FaseEscala.OPERANDO);
    }

    @Test
    void avancarFaseParaPartiuCarimbaPartidaEfetiva() {
        when(escalaRepositorio.findLockedById(1L))
                .thenReturn(Optional.of(escalaComFase(FaseEscala.OPERANDO)));

        EscalaDetalheDTO resultado = escalaServico.avancarFase(1L, FaseEscala.PARTIU);

        assertThat(resultado.getFase()).isEqualTo(FaseEscala.PARTIU);
        assertThat(resultado.getPartidaEfetiva()).isNotNull();
    }

    @Test
    void avancarFaseRejeitaTransicaoInvalida() {
        when(escalaRepositorio.findLockedById(1L))
                .thenReturn(Optional.of(escalaComFase(FaseEscala.PREVISTA)));

        assertThatThrownBy(() -> escalaServico.avancarFase(1L, FaseEscala.OPERANDO))
                .isInstanceOf(ResponseStatusException.class);

        verify(escalaRepositorio, never()).save(any());
    }

    @Test
    void atualizarRejeitaEscalaTerminal() {
        when(escalaRepositorio.findLockedById(1L))
                .thenReturn(Optional.of(escalaComFase(FaseEscala.ENCERRADA)));

        assertThatThrownBy(() -> escalaServico.atualizar(1L, new AtualizacaoEscalaDTO()))
                .isInstanceOf(ResponseStatusException.class);

        verify(escalaRepositorio, never()).save(any());
    }
}
