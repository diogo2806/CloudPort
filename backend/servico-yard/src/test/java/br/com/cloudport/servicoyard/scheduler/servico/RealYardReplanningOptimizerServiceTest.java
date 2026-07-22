package br.com.cloudport.servicoyard.scheduler.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.cloudport.servicoyard.scheduler.dto.SchedulerContainerDto;
import br.com.cloudport.servicoyard.scheduler.dto.SchedulerEquipmentDto;
import br.com.cloudport.servicoyard.scheduler.dto.SchedulerPlanoOperacionalRequisicaoDto;
import br.com.cloudport.servicoyard.scheduler.dto.SchedulerPositionCandidateDto;
import br.com.cloudport.servicoyard.scheduler.dto.VesselArrivalDto;
import br.com.cloudport.servicoyard.scheduler.servico.RealYardReplanningOptimizerService.ResultadoOtimizacaoReal;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class RealYardReplanningOptimizerServiceTest {

    private final RealYardReplanningOptimizerService servico = new RealYardReplanningOptimizerService();

    @Test
    void deveGerarPropostaDeterministicaComMemoriaEJustificativas() {
        ResultadoOtimizacaoReal primeiro = servico.otimizar(requisicaoBase());
        ResultadoOtimizacaoReal segundo = servico.otimizar(requisicaoBase());

        assertThat(primeiro.atribuicoes()).hasSize(1);
        assertThat(primeiro.atribuicoes().get(0).getLinhaProposta()).isEqualTo(2);
        assertThat(primeiro.atribuicoes().get(0).getColunaProposta()).isEqualTo(2);
        assertThat(primeiro.atribuicoes().get(0).getCamadaProposta()).isEqualTo("T1");
        assertThat(primeiro.atribuicoes().get(0).getEquipamentoId()).isEqualTo("RTG-01");
        assertThat(primeiro.memoriaCalculo()).containsKeys(
                "distancia",
                "ocupacao",
                "rehandles",
                "equipamento",
                "pontuacaoTotal");
        assertThat(primeiro.justificativas()).anyMatch(texto -> texto.contains("revalidacao transacional"));
        assertThat(primeiro.assinaturaEntrada()).isEqualTo(segundo.assinaturaEntrada());
        assertThat(primeiro.pontuacaoTotal()).isEqualTo(segundo.pontuacaoTotal());
    }

    @Test
    void deveRejeitarPosicoesBloqueadasReservadasOuIncompativeis() {
        SchedulerPlanoOperacionalRequisicaoDto requisicao = requisicaoBase();
        requisicao.setPosicoesCandidatas(List.of(
                posicao(1L, 2, 2, "T1", true, false, false, "BOBINA", "100", "5"),
                posicao(2L, 3, 3, "T1", false, true, false, "BOBINA", "100", "5"),
                posicao(3L, 4, 4, "T2", false, false, false, "BOBINA", "100", "5"),
                posicao(4L, 5, 5, "T1", false, false, false, "CHAPA", "100", "5"),
                posicao(5L, 6, 6, "T1", false, false, false, "BOBINA", "10", "1")
        ));

        assertThatThrownBy(() -> servico.otimizar(requisicao))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Nenhuma carga possui posicao real elegivel");
    }

    @Test
    void deveConsiderarDestinoOcupacaoRehandleECargaDoEquipamento() {
        SchedulerPlanoOperacionalRequisicaoDto requisicao = requisicaoBase();
        SchedulerPositionCandidateDto ocupada = posicao(
                11L, 1, 1, "T1", false, false, false, "BOBINA", "100", "5");
        ocupada.setBloco("LONGE");
        ocupada.setCapacidadePilha(5);
        ocupada.setOcupacaoPilha(4L);
        SchedulerPositionCandidateDto destino = posicao(
                12L, 4, 4, "T1", false, false, false, "BOBINA", "100", "5");
        destino.setBloco("EXPORT");
        destino.setCapacidadePilha(5);
        destino.setOcupacaoPilha(0L);
        requisicao.setPosicoesCandidatas(List.of(ocupada, destino));

        ResultadoOtimizacaoReal resultado = servico.otimizar(requisicao);

        assertThat(resultado.atribuicoes().get(0).getBlocoProposto()).isEqualTo("EXPORT");
        assertThat(resultado.atribuicoes().get(0).getJustificativas())
                .anyMatch(texto -> texto.contains("destino operacional"));
    }

    @Test
    void deveSaturarRehandlesQuandoOcupacaoExcedeInteiro() {
        SchedulerPlanoOperacionalRequisicaoDto requisicao = requisicaoBase();
        SchedulerPositionCandidateDto posicao = requisicao.getPosicoesCandidatas().get(1);
        posicao.setOcupacaoPilha(Long.MAX_VALUE);
        posicao.setCapacidadePilha(Integer.MAX_VALUE);

        ResultadoOtimizacaoReal resultado = servico.otimizar(requisicao);

        assertThat(resultado.atribuicoes()).hasSize(1);
        assertThat(resultado.atribuicoes().get(0).getRehandlesEstimados()).isEqualTo(Integer.MAX_VALUE);
        assertThat(resultado.rehandlesEstimados()).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void devePreservarRehandlesNoMaiorValorInteiroValido() {
        SchedulerPlanoOperacionalRequisicaoDto requisicao = requisicaoBase();
        SchedulerPositionCandidateDto posicao = requisicao.getPosicoesCandidatas().get(1);
        posicao.setOcupacaoPilha((long) Integer.MAX_VALUE + 1L);
        posicao.setCapacidadePilha(Integer.MAX_VALUE);

        ResultadoOtimizacaoReal resultado = servico.otimizar(requisicao);

        assertThat(resultado.atribuicoes()).hasSize(1);
        assertThat(resultado.atribuicoes().get(0).getRehandlesEstimados()).isEqualTo(Integer.MAX_VALUE);
        assertThat(resultado.rehandlesEstimados()).isEqualTo(Integer.MAX_VALUE);
    }

    private SchedulerPlanoOperacionalRequisicaoDto requisicaoBase() {
        SchedulerPlanoOperacionalRequisicaoDto requisicao = new SchedulerPlanoOperacionalRequisicaoDto();
        VesselArrivalDto navio = new VesselArrivalDto(
                "VIS-001",
                "BERCO-01",
                LocalDateTime.of(2026, 7, 18, 8, 0),
                LocalDateTime.of(2026, 7, 19, 8, 0),
                1,
                0);
        requisicao.setNavio(navio);
        requisicao.setCutoffOperacional(LocalDateTime.of(2099, 7, 18, 12, 0));
        requisicao.setEquipamentosDisponiveis(List.of("RTG-01"));
        requisicao.setEquipamentosOperacionais(List.of(equipamento("RTG-01", 2, 2, 25)));
        requisicao.setContainersImportacao(List.of(carga()));
        requisicao.setContainersExportacao(List.of());
        SchedulerPositionCandidateDto bloqueada = posicao(
                1L, 1, 1, "T1", true, false, false, "BOBINA", "100", "5");
        SchedulerPositionCandidateDto valida = posicao(
                2L, 2, 2, "T1", false, false, false, "BOBINA", "100", "5");
        valida.setBloco("EXPORT");
        valida.setCapacidadePilha(5);
        valida.setOcupacaoPilha(1L);
        requisicao.setPosicoesCandidatas(List.of(bloqueada, valida));
        return requisicao;
    }

    private SchedulerContainerDto carga() {
        SchedulerContainerDto carga = new SchedulerContainerDto("CONT-001", 8, 8);
        carga.setCamadaAtual("T1");
        carga.setTipoCarga("BOBINA");
        carga.setMovimento("DESCARGA");
        carga.setDestino("EXPORT");
        carga.setPesoToneladas(BigDecimal.valueOf(40));
        carga.setAlturaMetros(BigDecimal.valueOf(2));
        carga.setSequenciaOperacional(1);
        carga.setDwellTimeHoras(48);
        return carga;
    }

    private SchedulerEquipmentDto equipamento(
            String codigo,
            int linha,
            int coluna,
            int produtividade) {
        SchedulerEquipmentDto equipamento = new SchedulerEquipmentDto();
        equipamento.setEquipamentoId(codigo);
        equipamento.setDisponivel(true);
        equipamento.setConflitoRecurso(false);
        equipamento.setLinhaAtual(linha);
        equipamento.setColunaAtual(coluna);
        equipamento.setProdutividadeMovimentosHora(BigDecimal.valueOf(produtividade));
        equipamento.setTotalOrdens(1);
        equipamento.setPrioridadeWorkQueue(1);
        return equipamento;
    }

    private SchedulerPositionCandidateDto posicao(
            Long id,
            int linha,
            int coluna,
            String camada,
            boolean bloqueada,
            boolean reservada,
            boolean ocupada,
            String tipoCarga,
            String pesoMaximo,
            String alturaMaxima) {
        SchedulerPositionCandidateDto posicao = new SchedulerPositionCandidateDto();
        posicao.setId(id);
        posicao.setLinha(linha);
        posicao.setColuna(coluna);
        posicao.setCamada(camada);
        posicao.setBloqueada(bloqueada);
        posicao.setReservadaPorOutro(reservada);
        posicao.setOcupada(ocupada);
        posicao.setInterditada(false);
        posicao.setAreaPermitida(true);
        posicao.setAllocationCompativel(true);
        posicao.setReeferPermitida(true);
        posicao.setImoPermitida(true);
        posicao.setOogPermitida(true);
        posicao.setTiposCargaPermitidos(List.of(tipoCarga));
        posicao.setPesoMaximoToneladas(new BigDecimal(pesoMaximo));
        posicao.setAlturaMaximaMetros(new BigDecimal(alturaMaxima));
        posicao.setCapacidadePilha(5);
        posicao.setOcupacaoPilha(0L);
        posicao.setDistanciaBerco(linha + coluna);
        return posicao;
    }
}
