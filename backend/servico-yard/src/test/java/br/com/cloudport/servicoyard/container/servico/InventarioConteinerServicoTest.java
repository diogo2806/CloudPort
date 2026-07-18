package br.com.cloudport.servicoyard.container.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicoyard.container.dto.InventarioConteinerRespostaDTO;
import br.com.cloudport.servicoyard.container.entidade.TipoCargaConteiner;
import br.com.cloudport.servicoyard.patio.modelo.ConteinerPatio;
import br.com.cloudport.servicoyard.patio.modelo.PosicaoPatio;
import br.com.cloudport.servicoyard.patio.modelo.StatusConteiner;
import br.com.cloudport.servicoyard.patio.repositorio.ConteinerPatioRepositorio;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

class InventarioConteinerServicoTest {

    private final ConteinerPatioRepositorio repositorio = mock(ConteinerPatioRepositorio.class);
    private final InventarioConteinerServico servico = new InventarioConteinerServico(repositorio);

    @Test
    void deveConsolidarInventarioOperacional() {
        ConteinerPatio refrigerado = criarConteiner(
                1L,
                "ABCD1234567",
                StatusConteiner.ARMAZENADO,
                TipoCargaConteiner.REFRIGERADO,
                new BigDecimal("18.500"),
                new PosicaoPatio(10L, 1, 2, "T1"));
        ConteinerPatio retido = criarConteiner(
                2L,
                "EFGH7654321",
                StatusConteiner.RETIDO,
                TipoCargaConteiner.PERIGOSO,
                new BigDecimal("22.000"),
                null);

        when(repositorio.findAll(any(Sort.class))).thenReturn(List.of(refrigerado, retido));

        InventarioConteinerRespostaDTO resposta = servico.consultar(null, null, null);

        assertThat(resposta.getConteineres()).hasSize(2);
        assertThat(resposta.getResumo().getTotalConteiners()).isEqualTo(2);
        assertThat(resposta.getResumo().getTotalOperacionais()).isEqualTo(2);
        assertThat(resposta.getResumo().getTotalRetidos()).isEqualTo(1);
        assertThat(resposta.getResumo().getTotalRefrigerados()).isEqualTo(1);
        assertThat(resposta.getResumo().getTotalPerigosos()).isEqualTo(1);
        assertThat(resposta.getResumo().getTotalSemPosicao()).isEqualTo(1);
        assertThat(resposta.getResumo().getPesoTotalToneladas()).isEqualByComparingTo("40.500");
    }

    @Test
    void deveAplicarFiltrosDeCodigoStatusETipo() {
        ConteinerPatio alvo = criarConteiner(
                1L,
                "ABCD1234567",
                StatusConteiner.ARMAZENADO,
                TipoCargaConteiner.REFRIGERADO,
                new BigDecimal("18.500"),
                new PosicaoPatio(10L, 1, 2, "T1"));
        ConteinerPatio outro = criarConteiner(
                2L,
                "EFGH7654321",
                StatusConteiner.RETIDO,
                TipoCargaConteiner.PERIGOSO,
                new BigDecimal("22.000"),
                null);

        when(repositorio.findAll(any(Sort.class))).thenReturn(List.of(alvo, outro));

        InventarioConteinerRespostaDTO resposta = servico.consultar(
                "abcd",
                StatusConteiner.ARMAZENADO,
                TipoCargaConteiner.REFRIGERADO);

        assertThat(resposta.getConteineres()).singleElement()
                .extracting("identificacao")
                .isEqualTo("ABCD1234567");
        assertThat(resposta.getResumo().getTotalConteiners()).isEqualTo(1);
    }

    private ConteinerPatio criarConteiner(Long id,
                                          String codigo,
                                          StatusConteiner status,
                                          TipoCargaConteiner tipoCarga,
                                          BigDecimal peso,
                                          PosicaoPatio posicao) {
        ConteinerPatio conteiner = new ConteinerPatio();
        conteiner.setId(id);
        conteiner.setCodigo(codigo);
        conteiner.setStatus(status);
        conteiner.setTipoCarga(tipoCarga);
        conteiner.setPesoToneladas(peso);
        conteiner.setDestino("OPERACAO");
        conteiner.setPosicao(posicao);
        conteiner.setAtualizadoEm(LocalDateTime.of(2026, 7, 17, 12, 0));
        return conteiner;
    }
}
