package br.com.cloudport.serviconavio.escala.servico;

import br.com.cloudport.serviconavio.comum.validacao.SanitizadorEntrada;
import br.com.cloudport.serviconavio.escala.dto.AtualizacaoEscalaDTO;
import br.com.cloudport.serviconavio.escala.dto.CadastroEscalaDTO;
import br.com.cloudport.serviconavio.escala.dto.EscalaDetalheDTO;
import br.com.cloudport.serviconavio.escala.dto.OperacaoConteinerEscalaDTO;
import br.com.cloudport.serviconavio.escala.entidade.Escala;
import br.com.cloudport.serviconavio.escala.entidade.FaseEscala;
import br.com.cloudport.serviconavio.escala.listatrabalho.modelo.TipoMovimentacaoOrdemNavio;
import br.com.cloudport.serviconavio.escala.listatrabalho.servico.OrdemMovimentacaoNavioServico;
import br.com.cloudport.serviconavio.escala.repositorio.EscalaRepositorio;
import br.com.cloudport.serviconavio.navio.entidade.Navio;
import br.com.cloudport.serviconavio.navio.repositorio.NavioRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EscalaServicoTest {

    private EscalaRepositorio escalaRepositorio;
    private NavioRepositorio navioRepositorio;
    private SanitizadorEntrada sanitizadorEntrada;
    private OrdemMovimentacaoNavioServico ordemMovimentacaoServico;
    private EscalaServico escalaServico;

    @BeforeEach
    void preparar() {
        escalaRepositorio = mock(EscalaRepositorio.class);
        navioRepositorio = mock(NavioRepositorio.class);
        sanitizadorEntrada = mock(SanitizadorEntrada.class);
        ordemMovimentacaoServico = mock(OrdemMovimentacaoNavioServico.class);
        when(sanitizadorEntrada.limparTextoObrigatorio(anyString(), anyString()))
                .thenAnswer(invocacao -> invocacao.getArgument(0));
        when(sanitizadorEntrada.limparTexto(anyString()))
                .thenAnswer(invocacao -> invocacao.getArgument(0));
        when(escalaRepositorio.save(any(Escala.class))).thenAnswer(invocacao -> invocacao.getArgument(0));
        escalaServico = new EscalaServico(escalaRepositorio, navioRepositorio, sanitizadorEntrada,
                ordemMovimentacaoServico);
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
        when(escalaRepositorio.findById(1L)).thenReturn(Optional.of(escalaComFase(FaseEscala.PREVISTA)));

        EscalaDetalheDTO resultado = escalaServico.avancarFase(1L, FaseEscala.ATRACADO);

        assertThat(resultado.getFase()).isEqualTo(FaseEscala.ATRACADO);
        assertThat(resultado.getChegadaEfetiva()).isNotNull();
        assertThat(resultado.getAtracacaoEfetiva()).isNotNull();
        assertThat(resultado.getPartidaEfetiva()).isNull();
    }

    @Test
    void avancarFaseParaPartiuCarimbaPartidaEfetiva() {
        when(escalaRepositorio.findById(1L)).thenReturn(Optional.of(escalaComFase(FaseEscala.OPERANDO)));

        EscalaDetalheDTO resultado = escalaServico.avancarFase(1L, FaseEscala.PARTIU);

        assertThat(resultado.getFase()).isEqualTo(FaseEscala.PARTIU);
        assertThat(resultado.getPartidaEfetiva()).isNotNull();
    }

    @Test
    void avancarFaseRejeitaTransicaoInvalida() {
        when(escalaRepositorio.findById(1L)).thenReturn(Optional.of(escalaComFase(FaseEscala.PREVISTA)));

        assertThatThrownBy(() -> escalaServico.avancarFase(1L, FaseEscala.OPERANDO))
                .isInstanceOf(ResponseStatusException.class);

        verify(escalaRepositorio, never()).save(any());
    }

    @Test
    void atualizarRejeitaEscalaTerminal() {
        when(escalaRepositorio.findById(1L)).thenReturn(Optional.of(escalaComFase(FaseEscala.ENCERRADA)));

        assertThatThrownBy(() -> escalaServico.atualizar(1L, new AtualizacaoEscalaDTO()))
                .isInstanceOf(ResponseStatusException.class);

        verify(escalaRepositorio, never()).save(any());
    }

    @Test
    void avancarFaseParaAtracadoGeraOrdensDeMovimentacao() {
        when(escalaRepositorio.findById(1L)).thenReturn(Optional.of(escalaComFase(FaseEscala.PREVISTA)));

        escalaServico.avancarFase(1L, FaseEscala.ATRACADO);

        verify(ordemMovimentacaoServico).gerarOrdensPendentesParaEscala(any(Escala.class));
    }

    @Test
    void adicionarConteinerDescargaRegistraOrdemQuandoEmOperacao() {
        when(escalaRepositorio.findById(1L)).thenReturn(Optional.of(escalaComFase(FaseEscala.OPERANDO)));
        OperacaoConteinerEscalaDTO dto = new OperacaoConteinerEscalaDTO();
        dto.setCodigoConteiner("msku1234567");

        escalaServico.adicionarConteinerDescarga(1L, dto);

        verify(ordemMovimentacaoServico)
                .registrarOrdemSeNecessario(any(Escala.class), org.mockito.ArgumentMatchers.eq("MSKU1234567"),
                        org.mockito.ArgumentMatchers.eq(TipoMovimentacaoOrdemNavio.DESCARGA_NAVIO));
    }

    @Test
    void adicionarConteinerRejeitaEscalaTerminal() {
        when(escalaRepositorio.findById(1L)).thenReturn(Optional.of(escalaComFase(FaseEscala.ENCERRADA)));
        OperacaoConteinerEscalaDTO dto = new OperacaoConteinerEscalaDTO();
        dto.setCodigoConteiner("MSKU1234567");

        assertThatThrownBy(() -> escalaServico.adicionarConteinerDescarga(1L, dto))
                .isInstanceOf(ResponseStatusException.class);

        verify(escalaRepositorio, never()).save(any());
    }
}
