package br.com.cloudport.serviconavio.atracacao.servico;

import br.com.cloudport.serviconavio.atracacao.dto.CadastroVisitaNavioDTO;
import br.com.cloudport.serviconavio.atracacao.dto.PlanejamentoAtracacaoDTO;
import br.com.cloudport.serviconavio.atracacao.dto.VisitaNavioDetalheDTO;
import br.com.cloudport.serviconavio.atracacao.entidade.Berco;
import br.com.cloudport.serviconavio.atracacao.entidade.StatusBerco;
import br.com.cloudport.serviconavio.atracacao.entidade.StatusVisitaNavio;
import br.com.cloudport.serviconavio.atracacao.entidade.VisitaNavio;
import br.com.cloudport.serviconavio.atracacao.repositorio.OperacaoNavioConteinerRepositorio;
import br.com.cloudport.serviconavio.atracacao.repositorio.VisitaNavioRepositorio;
import br.com.cloudport.serviconavio.comum.validacao.SanitizadorEntrada;
import br.com.cloudport.serviconavio.navio.entidade.Navio;
import br.com.cloudport.serviconavio.navio.repositorio.NavioRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VisitaNavioServicoTest {

    private VisitaNavioRepositorio visitaRepositorio;
    private OperacaoNavioConteinerRepositorio operacaoRepositorio;
    private NavioRepositorio navioRepositorio;
    private BercoServico bercoServico;
    private VisitaNavioServico servico;

    @BeforeEach
    void preparar() {
        visitaRepositorio = mock(VisitaNavioRepositorio.class);
        operacaoRepositorio = mock(OperacaoNavioConteinerRepositorio.class);
        navioRepositorio = mock(NavioRepositorio.class);
        bercoServico = mock(BercoServico.class);
        SanitizadorEntrada sanitizadorEntrada = mock(SanitizadorEntrada.class);
        when(sanitizadorEntrada.limparTextoObrigatorio(anyString(), anyString()))
                .thenAnswer(invocacao -> invocacao.getArgument(0));
        when(sanitizadorEntrada.limparTexto(anyString()))
                .thenAnswer(invocacao -> invocacao.getArgument(0));
        when(visitaRepositorio.save(any(VisitaNavio.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));
        servico = new VisitaNavioServico(visitaRepositorio, operacaoRepositorio, navioRepositorio,
                bercoServico, sanitizadorEntrada);
    }

    private Navio navio() {
        Navio navio = new Navio();
        navio.setIdentificador(1L);
        navio.setNome("Navio Teste");
        navio.setCodigoImo("IMO1234567");
        return navio;
    }

    private CadastroVisitaNavioDTO cadastro(LocalDateTime atracacao, LocalDateTime desatracacao) {
        CadastroVisitaNavioDTO dto = new CadastroVisitaNavioDTO();
        dto.setNavioId(1L);
        dto.setNumeroViagem("V001");
        dto.setObservacoes("Observação");
        dto.setAtracacaoPrevista(atracacao);
        dto.setDesatracacaoPrevista(desatracacao);
        return dto;
    }

    @Test
    void registrarCriaVisitaPlanejada() {
        when(navioRepositorio.findById(1L)).thenReturn(Optional.of(navio()));
        LocalDateTime inicio = LocalDateTime.of(2026, 6, 1, 8, 0);
        LocalDateTime fim = LocalDateTime.of(2026, 6, 2, 8, 0);

        VisitaNavioDetalheDTO resultado = servico.registrar(cadastro(inicio, fim));

        assertThat(resultado.getStatus()).isEqualTo(StatusVisitaNavio.PLANEJADA);
        assertThat(resultado.getNavioNome()).isEqualTo("Navio Teste");
        assertThat(resultado.getNumeroViagem()).isEqualTo("V001");
    }

    @Test
    void registrarRejeitaJanelaInvalida() {
        when(navioRepositorio.findById(1L)).thenReturn(Optional.of(navio()));
        LocalDateTime inicio = LocalDateTime.of(2026, 6, 2, 8, 0);
        LocalDateTime fimAntesDoInicio = LocalDateTime.of(2026, 6, 1, 8, 0);

        assertThatThrownBy(() -> servico.registrar(cadastro(inicio, fimAntesDoInicio)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void planejarAtracacaoRejeitaConflitoNoMesmoBerco() {
        VisitaNavio visita = new VisitaNavio();
        visita.setIdentificador(10L);
        visita.setNavio(navio());
        visita.setStatus(StatusVisitaNavio.PLANEJADA);
        when(visitaRepositorio.findById(10L)).thenReturn(Optional.of(visita));

        Berco berco = new Berco();
        berco.setIdentificador(5L);
        berco.setNome("B1");
        berco.setStatus(StatusBerco.DISPONIVEL);
        when(bercoServico.obter(5L)).thenReturn(berco);

        when(visitaRepositorio.buscarConflitosDeAtracacao(eq(5L), anyLong(),
                any(Collection.class), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(new VisitaNavio()));

        PlanejamentoAtracacaoDTO dto = new PlanejamentoAtracacaoDTO();
        dto.setBercoId(5L);
        dto.setAtracacaoPrevista(LocalDateTime.of(2026, 6, 1, 8, 0));
        dto.setDesatracacaoPrevista(LocalDateTime.of(2026, 6, 2, 8, 0));

        assertThatThrownBy(() -> servico.planejarAtracacao(10L, dto))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void planejarAtracacaoSemConflitoMarcaProgramada() {
        VisitaNavio visita = new VisitaNavio();
        visita.setIdentificador(10L);
        visita.setNavio(navio());
        visita.setStatus(StatusVisitaNavio.PLANEJADA);
        when(visitaRepositorio.findById(10L)).thenReturn(Optional.of(visita));

        Berco berco = new Berco();
        berco.setIdentificador(5L);
        berco.setNome("B1");
        berco.setStatus(StatusBerco.DISPONIVEL);
        when(bercoServico.obter(5L)).thenReturn(berco);

        when(visitaRepositorio.buscarConflitosDeAtracacao(eq(5L), anyLong(),
                any(Collection.class), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());

        PlanejamentoAtracacaoDTO dto = new PlanejamentoAtracacaoDTO();
        dto.setBercoId(5L);
        dto.setAtracacaoPrevista(LocalDateTime.of(2026, 6, 1, 8, 0));
        dto.setDesatracacaoPrevista(LocalDateTime.of(2026, 6, 2, 8, 0));

        VisitaNavioDetalheDTO resultado = servico.planejarAtracacao(10L, dto);

        assertThat(resultado.getStatus()).isEqualTo(StatusVisitaNavio.PROGRAMADA);
        assertThat(resultado.getBercoNome()).isEqualTo("B1");
    }

    @Test
    void atracarExigeStatusProgramada() {
        VisitaNavio visita = new VisitaNavio();
        visita.setIdentificador(10L);
        visita.setNavio(navio());
        visita.setStatus(StatusVisitaNavio.PLANEJADA);
        when(visitaRepositorio.findById(10L)).thenReturn(Optional.of(visita));

        assertThatThrownBy(() -> servico.registrarAtracacao(10L))
                .isInstanceOf(ResponseStatusException.class);
    }
}
