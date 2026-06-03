package br.com.cloudport.servicoyard.estivagembulk.servico;

import static org.junit.jupiter.api.Assertions.*;

import br.com.cloudport.servicoyard.estivagembulk.dto.BallastOtimizacaoDto;
import br.com.cloudport.servicoyard.estivagembulk.modelo.NavioGranel;
import br.com.cloudport.servicoyard.estivagembulk.modelo.PlanoEstivaBulk;
import br.com.cloudport.servicoyard.estivagembulk.modelo.TanqueBallast;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BallastOtimizadorServico - Otimização de tanques de ballast")
class BallastOtimizadorServicoTest {

    private BallastOtimizadorServico servico;

    @BeforeEach
    void setup() {
        servico = new BallastOtimizadorServico();
    }

    @Test
    @DisplayName("Navio sem tanques retorna otimização inválida com aviso")
    void navioSemTanquesRetornaInvalido() {
        PlanoEstivaBulk plano = new PlanoEstivaBulk();
        NavioGranel navio = new NavioGranel();
        navio.setNome("MV SEM TANQUES");
        plano.setNavio(navio);

        BallastOtimizacaoDto dto = servico.otimizar(plano, 0.5);

        assertFalse(dto.isOtimizacaoValida());
        assertFalse(dto.getAlertas().isEmpty());
    }

    @Test
    @DisplayName("Otimização com tanques retorna resultado válido")
    void otimizacaoComTanquesRetornaValido() {
        PlanoEstivaBulk plano = criarPlanoComTanques();

        BallastOtimizacaoDto dto = servico.otimizar(plano, 0.5);

        assertTrue(dto.isOtimizacaoValida());
        assertFalse(dto.getTanques().isEmpty());
    }

    @Test
    @DisplayName("Trim alvo zero resulta em trim resultante próximo de zero")
    void trimAlvoZeroGeraEquilibrio() {
        PlanoEstivaBulk plano = criarPlanoComTanques();

        BallastOtimizacaoDto dto = servico.otimizar(plano, 0.0);

        assertTrue(dto.isOtimizacaoValida());
        assertTrue(Math.abs(dto.getTrimResultanteM()) <= Math.abs(dto.getTrimAlvoM()) + 0.5,
                "Trim resultante deve ser razoável para o alvo");
    }

    @Test
    @DisplayName("Resultado contém trim alvo definido corretamente")
    void trimAlvoRegistradoCorretamente() {
        PlanoEstivaBulk plano = criarPlanoComTanques();
        double trimAlvo = 1.2;

        BallastOtimizacaoDto dto = servico.otimizar(plano, trimAlvo);

        assertEquals(trimAlvo, dto.getTrimAlvoM(), 0.001);
    }

    private PlanoEstivaBulk criarPlanoComTanques() {
        NavioGranel navio = new NavioGranel();
        navio.setNome("MV TEST");
        navio.setLpp(200.0);
        navio.setBoca(32.0);
        navio.setDeslocamento(45000.0);
        navio.setGm(1.5);

        TanqueBallast tp = new TanqueBallast();
        tp.setNome("PEAK TANK PROA");
        tp.setCapacidadeM3(500.0);
        tp.setVolumeAtualM3(0.0);
        tp.setPosLongCentroM(5.0);
        tp.setPosTransCentroM(0.0);
        tp.setNavio(navio);
        navio.getTanquesBallast().add(tp);

        TanqueBallast ta = new TanqueBallast();
        ta.setNome("AFT PEAK TANK");
        ta.setCapacidadeM3(600.0);
        ta.setVolumeAtualM3(0.0);
        ta.setPosLongCentroM(195.0);
        ta.setPosTransCentroM(0.0);
        ta.setNavio(navio);
        navio.getTanquesBallast().add(ta);

        PlanoEstivaBulk plano = new PlanoEstivaBulk();
        plano.setNavio(navio);
        return plano;
    }
}
