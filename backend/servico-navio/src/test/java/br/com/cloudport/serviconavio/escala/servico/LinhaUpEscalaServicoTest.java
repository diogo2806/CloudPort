package br.com.cloudport.serviconavio.escala.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.cloudport.serviconavio.comum.validacao.SanitizadorEntrada;
import br.com.cloudport.serviconavio.escala.dto.LinhaUpEscalaDTO;
import br.com.cloudport.serviconavio.escala.entidade.Escala;
import br.com.cloudport.serviconavio.escala.entidade.FaseEscala;
import br.com.cloudport.serviconavio.escala.repositorio.EscalaRepositorio;
import br.com.cloudport.serviconavio.escala.repositorio.ProntidaoBercoRepositorio;
import br.com.cloudport.serviconavio.navio.entidade.Navio;
import br.com.cloudport.serviconavio.navio.repositorio.NavioRepositorio;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class LinhaUpEscalaServicoTest {

    @Test
    void listarLineUpRetornaJanelaOperacionalCompleta() {
        EscalaRepositorio escalaRepositorio = mock(EscalaRepositorio.class);
        NavioRepositorio navioRepositorio = mock(NavioRepositorio.class);
        ProntidaoBercoRepositorio prontidaoBercoRepositorio = mock(ProntidaoBercoRepositorio.class);
        SanitizadorEntrada sanitizadorEntrada = mock(SanitizadorEntrada.class);
        EscalaServico escalaServico = new EscalaServico(
                escalaRepositorio,
                navioRepositorio,
                prontidaoBercoRepositorio,
                sanitizadorEntrada);

        Navio navio = new Navio();
        navio.setIdentificador(15L);
        navio.setNome("CloudPort Express");
        navio.setCodigoImo("IMO9876543");
        navio.setEmpresaArmadora("Cloud Shipping");
        navio.setCapacidadeTeu(8500);
        navio.setLoaMetros(new BigDecimal("300.50"));

        Escala escala = new Escala();
        escala.setId(42L);
        escala.setNavio(navio);
        escala.setViagemEntrada("CP001N");
        escala.setViagemSaida("CP001S");
        escala.setFase(FaseEscala.PREVISTA);
        escala.setChegadaPrevista(LocalDateTime.of(2026, 7, 18, 6, 0));
        escala.setAtracacaoPrevista(LocalDateTime.of(2026, 7, 18, 8, 0));
        escala.setPartidaPrevista(LocalDateTime.of(2026, 7, 19, 2, 0));
        escala.setBercoPrevisto("B01");

        when(escalaRepositorio.buscarCronograma(any(LocalDateTime.class), any(LocalDateTime.class), anyList()))
                .thenReturn(List.of(escala));

        List<LinhaUpEscalaDTO> resultado = escalaServico.listarLineUp(30);

        assertThat(resultado).hasSize(1);
        LinhaUpEscalaDTO linha = resultado.get(0);
        assertThat(linha.getId()).isEqualTo(42L);
        assertThat(linha.getNomeNavio()).isEqualTo("CloudPort Express");
        assertThat(linha.getEmpresaArmadora()).isEqualTo("Cloud Shipping");
        assertThat(linha.getAtracacaoPrevista()).isEqualTo(LocalDateTime.of(2026, 7, 18, 8, 0));
        assertThat(linha.getPartidaPrevista()).isEqualTo(LocalDateTime.of(2026, 7, 19, 2, 0));
        assertThat(linha.getBercoPrevisto()).isEqualTo("B01");
    }
}
