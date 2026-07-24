package br.com.cloudport.servicoyard.patio.servico;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.cloudport.servicoyard.container.entidade.TipoCargaConteiner;
import br.com.cloudport.servicoyard.patio.dto.ViolacaoEstivagemPatioDto;
import br.com.cloudport.servicoyard.patio.modelo.ConteinerPatio;
import br.com.cloudport.servicoyard.patio.modelo.PosicaoPatio;
import br.com.cloudport.servicoyard.patio.modelo.TipoRegraEstivagemPatio;
import br.com.cloudport.servicoyard.patio.repositorio.ConteinerPatioRepositorio;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DetectorAvisoEstivagemPatioServicoTest {

    private final DetectorAvisoEstivagemPatioServico servico =
            new DetectorAvisoEstivagemPatioServico(Mockito.mock(ConteinerPatioRepositorio.class));

    @Test
    void deveDetectarPesoAlturaReeferReservaEApoio() {
        PosicaoPatio posicao = posicao(10L, 2, 4, "3");
        posicao.setPesoMaximoToneladas(new BigDecimal("15.000"));
        posicao.setCamadaMaxima(2);
        posicao.setTiposCargaPermitidos("SECO;PERIGOSO");
        posicao.reservar("RES-1", "OUTRO123", LocalDateTime.now().plusHours(1));

        ConteinerPatio unidade = unidade(20L, "REEF123", TipoCargaConteiner.REFRIGERADO,
                new BigDecimal("22.000"), posicao);

        Set<TipoRegraEstivagemPatio> regras = regras(servico.detectar(unidade, List.of(unidade)));

        assertThat(regras).contains(
                TipoRegraEstivagemPatio.PESO,
                TipoRegraEstivagemPatio.ALTURA,
                TipoRegraEstivagemPatio.REEFER,
                TipoRegraEstivagemPatio.RESERVA,
                TipoRegraEstivagemPatio.APOIO);
    }

    @Test
    void deveDetectarPerigosoAdjacenteSemConsiderarAPropriaUnidade() {
        ConteinerPatio unidade = unidade(1L, "IMO001", TipoCargaConteiner.PERIGOSO,
                new BigDecimal("10.000"), posicao(1L, 5, 5, "1"));
        ConteinerPatio vizinha = unidade(2L, "IMO002", TipoCargaConteiner.PERIGOSO,
                new BigDecimal("10.000"), posicao(2L, 6, 5, "1"));

        assertThat(regras(servico.detectar(unidade, List.of(unidade))))
                .doesNotContain(TipoRegraEstivagemPatio.PERIGOSO);
        assertThat(regras(servico.detectar(unidade, List.of(unidade, vizinha))))
                .contains(TipoRegraEstivagemPatio.PERIGOSO);
    }

    @Test
    void deveDetectarCapacidadeEPesoInvertidoNaPilha() {
        PosicaoPatio base = posicao(1L, 7, 8, "1");
        base.setCapacidadePilha(1);
        ConteinerPatio leve = unidade(1L, "LEVE001", TipoCargaConteiner.SECO,
                new BigDecimal("8.000"), base);

        PosicaoPatio topo = posicao(2L, 7, 8, "2");
        topo.setCapacidadePilha(1);
        ConteinerPatio pesado = unidade(2L, "PESO001", TipoCargaConteiner.SECO,
                new BigDecimal("20.000"), topo);

        Set<TipoRegraEstivagemPatio> regras = regras(
                servico.detectar(pesado, List.of(leve, pesado)));

        assertThat(regras).contains(
                TipoRegraEstivagemPatio.CAPACIDADE,
                TipoRegraEstivagemPatio.REGRA_PILHA);
    }

    private Set<TipoRegraEstivagemPatio> regras(List<ViolacaoEstivagemPatioDto> violacoes) {
        return violacoes.stream().map(ViolacaoEstivagemPatioDto::regra).collect(Collectors.toSet());
    }

    private ConteinerPatio unidade(Long id,
                                    String codigo,
                                    TipoCargaConteiner tipo,
                                    BigDecimal peso,
                                    PosicaoPatio posicao) {
        ConteinerPatio unidade = new ConteinerPatio();
        unidade.setId(id);
        unidade.setCodigo(codigo);
        unidade.setTipoCarga(tipo);
        unidade.setPesoToneladas(peso);
        unidade.setPosicao(posicao);
        return unidade;
    }

    private PosicaoPatio posicao(Long id, int linha, int coluna, String camada) {
        PosicaoPatio posicao = new PosicaoPatio(id, linha, coluna, camada);
        posicao.setAreaPermitida(true);
        return posicao;
    }
}
