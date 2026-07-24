package br.com.cloudport.servicogate.app.frota;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicogate.app.cidadao.VeiculoRepository;
import br.com.cloudport.servicogate.app.configuracoes.TransportadoraRepository;
import br.com.cloudport.servicogate.app.frota.VeiculoFrotaDtos.Resposta;
import br.com.cloudport.servicogate.app.frota.VeiculoFrotaDtos.Salvar;
import br.com.cloudport.servicogate.model.Transportadora;
import br.com.cloudport.servicogate.model.Veiculo;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class VeiculoFrotaServicoTest {

    @Mock
    private VeiculoRepository veiculoRepository;
    @Mock
    private TransportadoraRepository transportadoraRepository;

    private VeiculoFrotaServico servico;

    @BeforeEach
    void setUp() {
        servico = new VeiculoFrotaServico(veiculoRepository, transportadoraRepository);
    }

    @Test
    void deveUsarVinculoJwtAoCriarVeiculo() {
        Transportadora transportadora = transportadora(7L, "12345678000199");
        Authentication authentication = transportadoraAuthentication(transportadora.getDocumento());
        when(transportadoraRepository.findByDocumento(transportadora.getDocumento())).thenReturn(Optional.of(transportadora));
        when(veiculoRepository.findByPlaca("ABC1D23")).thenReturn(Optional.empty());
        when(veiculoRepository.findByPlacaCarreta("DEF4G56")).thenReturn(Optional.empty());
        when(veiculoRepository.save(any())).thenAnswer(invocation -> {
            Veiculo veiculo = invocation.getArgument(0);
            veiculo.setId(10L);
            return veiculo;
        });

        Resposta resposta = servico.criar(
                new Salvar("ABC-1D23", "DEF-4G56", "R 450", "CAMINHAO", 7L, true),
                authentication);

        assertThat(resposta.transportadoraId()).isEqualTo(7L);
        assertThat(resposta.placa()).isEqualTo("ABC1D23");
        assertThat(resposta.placaCarreta()).isEqualTo("DEF4G56");
    }

    @Test
    void deveBloquearTransportadoraDiferenteDoVinculoJwt() {
        Transportadora transportadora = transportadora(7L, "12345678000199");
        Authentication authentication = transportadoraAuthentication(transportadora.getDocumento());
        when(transportadoraRepository.findByDocumento(transportadora.getDocumento())).thenReturn(Optional.of(transportadora));

        assertThatThrownBy(() -> servico.criar(
                new Salvar("ABC1D23", null, null, "CAMINHAO", 8L, true),
                authentication))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("não pode acessar veículos de outra transportadora");

        verify(veiculoRepository, never()).save(any());
        verify(transportadoraRepository, never()).findById(8L);
    }

    @Test
    void deveListarSomenteFrotaDaTransportadoraAutenticada() {
        Transportadora transportadora = transportadora(7L, "12345678000199");
        Veiculo veiculo = new Veiculo();
        veiculo.setId(10L);
        veiculo.setPlaca("ABC1D23");
        veiculo.setTipo("CAMINHAO");
        veiculo.setAtivo(true);
        veiculo.setTransportadora(transportadora);
        Authentication authentication = transportadoraAuthentication(transportadora.getDocumento());
        when(transportadoraRepository.findByDocumento(transportadora.getDocumento())).thenReturn(Optional.of(transportadora));
        when(veiculoRepository.findByTransportadoraIdOrderByPlacaAsc(7L)).thenReturn(List.of(veiculo));

        List<Resposta> resposta = servico.listar(null, 999L, null, authentication);

        assertThat(resposta).extracting(Resposta::transportadoraId).containsExactly(7L);
        verify(veiculoRepository).findByTransportadoraIdOrderByPlacaAsc(7L);
        verify(veiculoRepository, never()).findByTransportadoraIdOrderByPlacaAsc(999L);
    }

    private Authentication transportadoraAuthentication(String documento) {
        Jwt jwt = new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(600),
                Map.of("alg", "none"),
                Map.of("sub", "transportadora", "transportadoraDocumento", documento));
        return new JwtAuthenticationToken(
                jwt,
                List.of(new SimpleGrantedAuthority("ROLE_TRANSPORTADORA")),
                "transportadora");
    }

    private Transportadora transportadora(Long id, String documento) {
        Transportadora transportadora = new Transportadora();
        transportadora.setId(id);
        transportadora.setNome("Transportadora Teste");
        transportadora.setDocumento(documento);
        return transportadora;
    }
}
