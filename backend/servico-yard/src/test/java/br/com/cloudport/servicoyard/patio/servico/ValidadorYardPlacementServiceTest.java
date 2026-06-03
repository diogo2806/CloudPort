package br.com.cloudport.servicoyard.patio.servico;

import br.com.cloudport.servicoyard.container.entidade.TipoCargaConteiner;
import br.com.cloudport.servicoyard.patio.dto.ConteinerPatioRequisicaoDto;
import br.com.cloudport.servicoyard.patio.modelo.ConteinerPatio;
import br.com.cloudport.servicoyard.patio.modelo.PosicaoPatio;
import br.com.cloudport.servicoyard.patio.modelo.StatusConteiner;
import br.com.cloudport.servicoyard.patio.repositorio.ConteinerPatioRepositorio;
import br.com.cloudport.servicoyard.recursos.entidade.BercoPortuario;
import br.com.cloudport.servicoyard.recursos.entidade.StatusBerco;
import br.com.cloudport.servicoyard.recursos.repositorio.BercoPortuarioRepositorio;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@DisplayName("ValidadorYardPlacementService - Testes de Validação de Yard Planning")
class ValidadorYardPlacementServiceTest {

    @Mock
    private ConteinerPatioRepositorio conteinerPatioRepositorio;

    @Mock
    private BercoPortuarioRepositorio bercoRepositorio;

    private ValidadorYardPlacementService validador;

    private ConteinerPatioRequisicaoDto criarRequisicaoPadrao() {
        ConteinerPatioRequisicaoDto dto = new ConteinerPatioRequisicaoDto();
        dto.setCodigo("CNTR001");
        dto.setLinha(1);
        dto.setColuna(1);
        dto.setStatus(StatusConteiner.ALOCADO);
        dto.setDestino("BERCO_01");
        dto.setCamadaOperacional("1");
        return dto;
    }

    private BercoPortuario criarBercoPadrao() {
        BercoPortuario berco = new BercoPortuario();
        berco.setId(1L);
        berco.setCodigo("BERCO_01");
        berco.setNome("Berço 01");
        berco.setComprimentoMetros(300);
        berco.setCaladoMetros(new BigDecimal("12.50"));
        berco.setGuinchesPermanentes(4);
        berco.setCapacidadeToneladasDia(5000);
        berco.setVoltagem("400V/60Hz");
        berco.setAguaPotavel(true);
        berco.setEnergiaGenerica(true);
        berco.setIluminacaoNoturna(true);
        berco.setSistemaSeguranca(true);
        berco.setCobertura(false);
        berco.setCompatContainer(true);
        berco.setCompatBreakbulk(true);
        berco.setCompatRoro(true);
        berco.setCompatCargaGeral(true);
        berco.setCompatReefer(true);
        berco.setCompatPerigosa(true);
        berco.setCompatGranel(true);
        berco.setZonaPrimaria("ZONA_A");
        berco.setDistanciaZonaMetros(50);
        berco.setTempoTransporteMinutos(10);
        berco.setDiasOperacao("SEG-DOM");
        berco.setStatus(StatusBerco.OPERACIONAL);
        return berco;
    }

    private ConteinerPatio criarConteinerPadrao() {
        PosicaoPatio posicao = new PosicaoPatio();
        posicao.setLinha(1);
        posicao.setColuna(1);
        posicao.setCamadaOperacional("1");

        ConteinerPatio conteiner = new ConteinerPatio();
        conteiner.setId(1L);
        conteiner.setCodigo("CNTR001");
        conteiner.setTipoCarga(TipoCargaConteiner.SECO);
        conteiner.setPesoToneladas(new BigDecimal("15"));
        conteiner.setStatus(StatusConteiner.ALOCADO);
        conteiner.setPosicao(posicao);
        conteiner.setDestino("BERCO_01");
        return conteiner;
    }

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        validador = new ValidadorYardPlacementService(conteinerPatioRepositorio, bercoRepositorio);
    }

    @Test
    @DisplayName("Deve validar contêiner SECO em berço compatível")
    void validarSecoEmBercoCompativel() {
        ConteinerPatioRequisicaoDto dto = criarRequisicaoPadrao();
        dto.setTipoCarga("SECO");

        BercoPortuario berco = criarBercoPadrao();
        when(bercoRepositorio.findByCodigoIgnoreCase(anyString())).thenReturn(Optional.of(berco));

        assertDoesNotThrow(() -> validador.validarAlocacao(dto));
    }

    @Test
    @DisplayName("Deve rejeitar REEFER em berço sem compatibilidade")
    void rejeitarReeferEmBercoIncompativel() {
        ConteinerPatioRequisicaoDto dto = criarRequisicaoPadrao();
        dto.setTipoCarga("REFRIGERADO");

        BercoPortuario berco = criarBercoPadrao();
        berco.setCompatReefer(false);
        when(bercoRepositorio.findByCodigoIgnoreCase(anyString())).thenReturn(Optional.of(berco));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> validador.validarAlocacao(dto));
        assert(exception.getMessage().contains("não suporta contêineres refrigerados"));
    }

    @Test
    @DisplayName("Deve rejeitar REEFER em berço sem energia")
    void rejeitarReeferEmBercoSemEnergia() {
        ConteinerPatioRequisicaoDto dto = criarRequisicaoPadrao();
        dto.setTipoCarga("REFRIGERADO");

        BercoPortuario berco = criarBercoPadrao();
        berco.setCompatReefer(true);
        berco.setEnergiaGenerica(false);
        when(bercoRepositorio.findByCodigoIgnoreCase(anyString())).thenReturn(Optional.of(berco));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> validador.validarAlocacao(dto));
        assert(exception.getMessage().contains("infraestrutura de energia"));
    }

    @Test
    @DisplayName("Deve rejeitar PERIGOSO em berço sem compatibilidade")
    void rejeitarPerigososoEmBercoIncompativel() {
        ConteinerPatioRequisicaoDto dto = criarRequisicaoPadrao();
        dto.setTipoCarga("PERIGOSO");

        BercoPortuario berco = criarBercoPadrao();
        berco.setCompatPerigosa(false);
        when(bercoRepositorio.findByCodigoIgnoreCase(anyString())).thenReturn(Optional.of(berco));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> validador.validarAlocacao(dto));
        assert(exception.getMessage().contains("não autoriza cargas perigosas"));
    }

    @Test
    @DisplayName("Deve rejeitar empilhamento acima de 4 níveis")
    void rejeitarAlturaMaiorQue4Niveis() {
        ConteinerPatioRequisicaoDto dto = criarRequisicaoPadrao();
        dto.setCamadaOperacional("5");
        dto.setTipoCarga("SECO");

        ConteinerPatio conteiner = criarConteinerPadrao();
        when(conteinerPatioRepositorio.findByCodigoIgnoreCase("CNTR001"))
                .thenReturn(Optional.of(conteiner));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> validador.validarAlocacao(dto));
        assert(exception.getMessage().contains("Altura de empilhamento máxima"));
    }

    @Test
    @DisplayName("Deve reduzir altura máxima para 2 níveis com peso > 20t")
    void reduzirAlturaParaPesoMaior20Toneladas() {
        ConteinerPatioRequisicaoDto dto = criarRequisicaoPadrao();
        dto.setCamadaOperacional("3");

        ConteinerPatio conteiner = criarConteinerPadrao();
        conteiner.setPesoToneladas(new BigDecimal("22"));
        when(conteinerPatioRepositorio.findByCodigoIgnoreCase("CNTR001"))
                .thenReturn(Optional.of(conteiner));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> validador.validarAlocacao(dto));
        assert(exception.getMessage().contains("pode ser empilhado apenas até nível 2"));
    }

    @Test
    @DisplayName("Deve reduzir altura máxima para 1 nível com peso > 25t")
    void reduzirAlturaParaPesoMaior25Toneladas() {
        ConteinerPatioRequisicaoDto dto = criarRequisicaoPadrao();
        dto.setCamadaOperacional("2");

        ConteinerPatio conteiner = criarConteinerPadrao();
        conteiner.setPesoToneladas(new BigDecimal("26"));
        when(conteinerPatioRepositorio.findByCodigoIgnoreCase("CNTR001"))
                .thenReturn(Optional.of(conteiner));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> validador.validarAlocacao(dto));
        assert(exception.getMessage().contains("pode ser empilhado apenas até nível 1"));
    }

    @Test
    @DisplayName("Deve permitir nível 1 para contêiner pesado (> 25t)")
    void permitirNivel1ParaContainerPesado() {
        ConteinerPatioRequisicaoDto dto = criarRequisicaoPadrao();
        dto.setCamadaOperacional("1");

        ConteinerPatio conteiner = criarConteinerPadrao();
        conteiner.setPesoToneladas(new BigDecimal("28"));
        when(conteinerPatioRepositorio.findByCodigoIgnoreCase("CNTR001"))
                .thenReturn(Optional.of(conteiner));

        BercoPortuario berco = criarBercoPadrao();
        when(bercoRepositorio.findByCodigoIgnoreCase(anyString())).thenReturn(Optional.of(berco));

        assertDoesNotThrow(() -> validador.validarAlocacao(dto));
    }
}
