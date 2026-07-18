package br.com.cloudport.servicogate.app.gestor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicogate.exception.ConflictException;
import br.com.cloudport.servicogate.model.Agendamento;
import br.com.cloudport.servicogate.model.GatePass;
import br.com.cloudport.servicogate.model.GateResourceOccupation;
import br.com.cloudport.servicogate.model.Motorista;
import br.com.cloudport.servicogate.model.Veiculo;
import br.com.cloudport.servicogate.model.enums.GateResourceType;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GateResourceOccupationServiceTest {

    @Mock
    private GateResourceOccupationRepository repository;

    @Test
    void deveReaproveitarOcupacaoIdempotenteDaMesmaVisita() {
        GatePass gatePass = criarGatePass(1L, "GP-001");
        Agendamento agendamento = criarAgendamento(gatePass);
        List<GateResourceOccupation> ocupacoes = List.of(
                criarOcupacao(gatePass, GateResourceType.MOTORISTA, "123.456.789-00"),
                criarOcupacao(gatePass, GateResourceType.CAVALO, "ABC1D23"),
                criarOcupacao(gatePass, GateResourceType.CHASSIS, "CHASSIS01"),
                criarOcupacao(gatePass, GateResourceType.UNIDADE, "CONT001"),
                criarOcupacao(gatePass, GateResourceType.UNIDADE, "CONT002"));
        when(repository.findByGatePassIdAndAtivoTrue(1L)).thenReturn(ocupacoes);

        GateResourceOccupationService service = new GateResourceOccupationService(repository);

        List<GateResourceOccupation> resultado = service.ocuparRecursos(
                agendamento,
                gatePass,
                "chassis 01",
                List.of("cont 001", "CONT002"));

        assertThat(resultado).containsExactlyElementsOf(ocupacoes);
        verify(repository, never()).saveAllAndFlush(org.mockito.ArgumentMatchers.anyCollection());
    }

    @Test
    void deveRejeitarAlteracaoDeRecursosEmGatePassJaAtivo() {
        GatePass gatePass = criarGatePass(1L, "GP-001");
        Agendamento agendamento = criarAgendamento(gatePass);
        List<GateResourceOccupation> ocupacoes = List.of(
                criarOcupacao(gatePass, GateResourceType.MOTORISTA, "123.456.789-00"),
                criarOcupacao(gatePass, GateResourceType.CAVALO, "ABC1D23"),
                criarOcupacao(gatePass, GateResourceType.CHASSIS, "CHASSIS01"));
        when(repository.findByGatePassIdAndAtivoTrue(1L)).thenReturn(ocupacoes);

        GateResourceOccupationService service = new GateResourceOccupationService(repository);

        assertThatThrownBy(() -> service.ocuparRecursos(
                agendamento,
                gatePass,
                "CHASSIS-ALTERADO",
                List.of()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("já possui visita ativa com recursos diferentes");
    }

    @Test
    void deveRetornarConflitoQuandoRecursoPertenceAOutraVisita() {
        GatePass gatePassAtual = criarGatePass(1L, "GP-001");
        GatePass gatePassConflitante = criarGatePass(2L, "GP-002");
        Agendamento agendamento = criarAgendamento(gatePassAtual);
        GateResourceOccupation ocupacaoConflitante = criarOcupacao(
                gatePassConflitante,
                GateResourceType.MOTORISTA,
                "123.456.789-00");

        when(repository.findByGatePassIdAndAtivoTrue(1L)).thenReturn(List.of());
        when(repository.findByTipoRecursoAndChaveRecursoInAndAtivoTrue(
                eq(GateResourceType.MOTORISTA),
                anyCollection()))
                .thenReturn(List.of(ocupacaoConflitante));

        GateResourceOccupationService service = new GateResourceOccupationService(repository);

        assertThatThrownBy(() -> service.ocuparRecursos(
                agendamento,
                gatePassAtual,
                null,
                List.of()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("GP-002")
                .hasMessageContaining("MOTORISTA");

        verify(repository, never()).saveAllAndFlush(org.mockito.ArgumentMatchers.anyCollection());
    }

    @Test
    void deveLiberarTodosOsRecursosDaVisitaAtomicamente() {
        GatePass gatePass = criarGatePass(1L, "GP-001");
        GateResourceOccupation motorista = criarOcupacao(
                gatePass,
                GateResourceType.MOTORISTA,
                "123.456.789-00");
        GateResourceOccupation cavalo = criarOcupacao(
                gatePass,
                GateResourceType.CAVALO,
                "ABC1D23");
        List<GateResourceOccupation> ocupacoes = List.of(motorista, cavalo);
        when(repository.findByGatePassIdAndAtivoTrue(1L)).thenReturn(ocupacoes);

        GateResourceOccupationService service = new GateResourceOccupationService(repository);
        service.liberarRecursos(1L);

        assertThat(ocupacoes)
                .allSatisfy(ocupacao -> {
                    assertThat(ocupacao.isAtivo()).isFalse();
                    assertThat(ocupacao.getLiberadoEm()).isNotNull();
                });
        assertThat(motorista.getLiberadoEm()).isEqualTo(cavalo.getLiberadoEm());
        verify(repository).saveAllAndFlush(ocupacoes);
    }

    private Agendamento criarAgendamento(GatePass gatePass) {
        Motorista motorista = new Motorista();
        motorista.setDocumento("123.456.789-00");
        Veiculo veiculo = new Veiculo();
        veiculo.setPlaca("ABC 1D23");

        Agendamento agendamento = new Agendamento();
        agendamento.setMotorista(motorista);
        agendamento.setVeiculo(veiculo);
        agendamento.setGatePass(gatePass);
        gatePass.setAgendamento(agendamento);
        return agendamento;
    }

    private GatePass criarGatePass(Long id, String codigo) {
        GatePass gatePass = new GatePass();
        gatePass.setId(id);
        gatePass.setCodigo(codigo);
        return gatePass;
    }

    private GateResourceOccupation criarOcupacao(GatePass gatePass,
                                                  GateResourceType tipo,
                                                  String chave) {
        GateResourceOccupation ocupacao = new GateResourceOccupation();
        ocupacao.setGatePass(gatePass);
        ocupacao.setTipoRecurso(tipo);
        ocupacao.setChaveRecurso(chave);
        ocupacao.setAtivo(true);
        return ocupacao;
    }
}
