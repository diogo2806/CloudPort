package br.com.cloudport.servicoyard.patio.servico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.cloudport.servicoyard.container.entidade.TipoCargaConteiner;
import br.com.cloudport.servicoyard.patio.modelo.ConteinerPatio;
import br.com.cloudport.servicoyard.patio.modelo.EquipamentoPatio;
import br.com.cloudport.servicoyard.patio.modelo.PosicaoPatio;
import br.com.cloudport.servicoyard.patio.modelo.StatusConteiner;
import br.com.cloudport.servicoyard.patio.modelo.StatusEquipamento;
import br.com.cloudport.servicoyard.patio.modelo.TipoEquipamento;
import br.com.cloudport.servicoyard.patio.servico.OtimizadorPesquisaOperacionalPatioServico.ResultadoOtimizacao;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Otimizador de pátio por pesquisa operacional")
class OtimizadorPesquisaOperacionalPatioServicoTest {

    private OtimizadorPesquisaOperacionalPatioServico otimizador;

    @BeforeEach
    void configurar() {
        otimizador = new OtimizadorPesquisaOperacionalPatioServico();
    }

    @Test
    @DisplayName("Deve maximizar a quantidade alocada antes de minimizar o custo")
    void deveFazerAtribuicaoGlobalEmVezDeEscolhaGulosa() {
        ConteinerPatio seco = conteiner(1L, "SECO001", TipoCargaConteiner.SECO, "BLOCO-A", "10");
        ConteinerPatio reefer = conteiner(2L, "REEFER001", TipoCargaConteiner.REFRIGERADO, "BLOCO-A", "10");

        PosicaoPatio flexivel = posicao(1L, 1, 1, "1", "BLOCO-A", "SECO,REFRIGERADO");
        PosicaoPatio somenteSeco = posicao(2L, 10, 10, "1", "BLOCO-B", "SECO");
        EquipamentoPatio equipamentoReefer = equipamento(1L, 1, 1);

        ResultadoOtimizacao resultado = otimizador.otimizar(
                List.of(seco, reefer),
                List.of(flexivel, somenteSeco),
                List.of(equipamentoReefer),
                List.of(seco, reefer));

        assertEquals(2, resultado.getAlocacoes().size());
        assertEquals(flexivel, resultado.getAlocacoes().get(reefer));
        assertEquals(somenteSeco, resultado.getAlocacoes().get(seco));
        assertTrue(resultado.getMotivosNaoAlocacao().isEmpty());
    }

    @Test
    @DisplayName("Deve respeitar bloqueio, reserva, peso e altura da pilha")
    void deveAplicarRestricoesComoCondicoesObrigatorias() {
        ConteinerPatio pesado = conteiner(1L, "PESADO001", TipoCargaConteiner.SECO, "BLOCO-A", "26");

        PosicaoPatio bloqueada = posicao(1L, 1, 1, "1", "BLOCO-A", "SECO");
        bloqueada.setBloqueada(true);
        PosicaoPatio reservada = posicao(2L, 2, 2, "1", "BLOCO-A", "SECO");
        reservada.reservar("outra-operacao", "OUTRO001", LocalDateTime.now().plusHours(1));
        PosicaoPatio camadaDois = posicao(3L, 3, 3, "2", "BLOCO-A", "SECO");
        PosicaoPatio apoioCamadaDois = posicao(4L, 3, 3, "1", "BLOCO-A", "SECO");
        PosicaoPatio valida = posicao(5L, 4, 4, "1", "BLOCO-A", "SECO");
        valida.setPesoMaximoToneladas(new BigDecimal("30"));

        ConteinerPatio apoio = conteiner(10L, "APOIO001", TipoCargaConteiner.SECO, "BLOCO-A", "15");
        apoio.setPosicao(apoioCamadaDois);

        ResultadoOtimizacao resultado = otimizador.otimizar(
                List.of(pesado),
                List.of(bloqueada, reservada, apoioCamadaDois, camadaDois, valida),
                List.of(equipamento(1L, 4, 4)),
                List.of(pesado, apoio));

        assertEquals(1, resultado.getAlocacoes().size());
        assertEquals(valida, resultado.getAlocacoes().get(pesado));
    }

    @Test
    @DisplayName("Não deve criar buraco na pilha e deve liberar a camada seguinte após o apoio")
    void deveRespeitarApoioFisicoDaPilha() {
        ConteinerPatio conteiner = conteiner(1L, "SECO001", TipoCargaConteiner.SECO, "BLOCO-A", "10");
        PosicaoPatio camadaDois = posicao(2L, 1, 1, "2", "BLOCO-A", "SECO");

        ResultadoOtimizacao semApoio = otimizador.otimizar(
                List.of(conteiner),
                List.of(camadaDois),
                List.of(equipamento(1L, 1, 1)),
                List.of(conteiner));

        assertTrue(semApoio.getAlocacoes().isEmpty());
        assertFalse(semApoio.getMotivosNaoAlocacao().isEmpty());

        PosicaoPatio camadaUm = posicao(1L, 1, 1, "1", "BLOCO-A", "SECO");
        ConteinerPatio apoio = conteiner(2L, "APOIO001", TipoCargaConteiner.SECO, "BLOCO-A", "10");
        apoio.setPosicao(camadaUm);
        conteiner.setPosicao(null);

        ResultadoOtimizacao comApoio = otimizador.otimizar(
                List.of(conteiner),
                List.of(camadaUm, camadaDois),
                List.of(equipamento(1L, 1, 1)),
                List.of(conteiner, apoio));

        assertEquals(camadaDois, comApoio.getAlocacoes().get(conteiner));
    }

    @Test
    @DisplayName("Deve isolar cargas perigosas também dentro do mesmo lote")
    void deveIsolarCargasPerigosas() {
        ConteinerPatio perigosoUm = conteiner(1L, "IMO001", TipoCargaConteiner.PERIGOSO, "BLOCO-A", "10");
        ConteinerPatio perigosoDois = conteiner(2L, "IMO002", TipoCargaConteiner.PERIGOSO, "BLOCO-A", "10");
        PosicaoPatio posicaoUm = posicao(1L, 1, 1, "1", "BLOCO-A", "PERIGOSO");
        PosicaoPatio posicaoVizinha = posicao(2L, 2, 2, "1", "BLOCO-A", "PERIGOSO");
        PosicaoPatio posicaoIsolada = posicao(3L, 5, 5, "1", "BLOCO-A", "PERIGOSO");

        ResultadoOtimizacao resultado = otimizador.otimizar(
                List.of(perigosoUm, perigosoDois),
                List.of(posicaoUm, posicaoVizinha, posicaoIsolada),
                List.of(equipamento(1L, 1, 1)),
                List.of(perigosoUm, perigosoDois));

        assertEquals(2, resultado.getAlocacoes().size());
        PosicaoPatio primeira = resultado.getAlocacoes().get(perigosoUm);
        PosicaoPatio segunda = resultado.getAlocacoes().get(perigosoDois);
        assertTrue(Math.abs(primeira.getLinha() - segunda.getLinha()) > 1
                || Math.abs(primeira.getColuna() - segunda.getColuna()) > 1);
    }

    private ConteinerPatio conteiner(
            Long id,
            String codigo,
            TipoCargaConteiner tipoCarga,
            String destino,
            String peso
    ) {
        ConteinerPatio conteiner = new ConteinerPatio();
        conteiner.setId(id);
        conteiner.setCodigo(codigo);
        conteiner.setTipoCarga(tipoCarga);
        conteiner.setDestino(destino);
        conteiner.setPesoToneladas(new BigDecimal(peso));
        conteiner.setStatus(StatusConteiner.ALOCADO);
        return conteiner;
    }

    private PosicaoPatio posicao(
            Long id,
            Integer linha,
            Integer coluna,
            String camada,
            String bloco,
            String tiposPermitidos
    ) {
        PosicaoPatio posicao = new PosicaoPatio(id, linha, coluna, camada);
        posicao.setBloco(bloco);
        posicao.setAreaPermitida(true);
        posicao.setTiposCargaPermitidos(tiposPermitidos);
        posicao.setCapacidadePilha(4);
        posicao.setCamadaMaxima(4);
        return posicao;
    }

    private EquipamentoPatio equipamento(Long id, Integer linha, Integer coluna) {
        return new EquipamentoPatio(
                id,
                "RTG-" + id,
                TipoEquipamento.RTG,
                linha,
                coluna,
                StatusEquipamento.OPERACIONAL);
    }
}
