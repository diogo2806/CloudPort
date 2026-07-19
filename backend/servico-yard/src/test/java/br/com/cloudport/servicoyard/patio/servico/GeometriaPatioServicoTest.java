package br.com.cloudport.servicoyard.patio.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicoyard.patio.dto.GeometriaPatioDto;
import br.com.cloudport.servicoyard.patio.dto.GeometriaPatioRequisicaoDto;
import br.com.cloudport.servicoyard.patio.modelo.GeometriaPatio;
import br.com.cloudport.servicoyard.patio.modelo.TipoGeometriaPatio;
import br.com.cloudport.servicoyard.patio.repositorio.GeometriaPatioRepositorio;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GeometriaPatioServicoTest {

    @Mock
    private GeometriaPatioRepositorio geometriaPatioRepositorio;

    private GeometriaPatioServico servico;
    private ObjectMapper objectMapper;

    @BeforeEach
    void configurar() {
        objectMapper = new ObjectMapper();
        servico = new GeometriaPatioServico(geometriaPatioRepositorio, objectMapper);
    }

    @Test
    void deveCriarPilhaComPoligonoGeoJsonFechado() throws Exception {
        GeometriaPatioRequisicaoDto requisicao = requisicaoValida();
        when(geometriaPatioRepositorio.findByCodigoIgnoreCase("PILHA-A-01-01"))
                .thenReturn(Optional.empty());
        when(geometriaPatioRepositorio.save(any(GeometriaPatio.class)))
                .thenAnswer(invocacao -> {
                    GeometriaPatio geometria = invocacao.getArgument(0);
                    geometria.setId(10L);
                    return geometria;
                });

        GeometriaPatioDto resposta = servico.criar(requisicao);

        assertThat(resposta.getId()).isEqualTo(10L);
        assertThat(resposta.getCodigo()).isEqualTo("PILHA-A-01-01");
        assertThat(resposta.getTipo()).isEqualTo(TipoGeometriaPatio.PILHA);
        assertThat(resposta.getGeoJson().path("geometry").path("type").asText()).isEqualTo("Polygon");
        assertThat(resposta.getAtualizadoPor()).isEqualTo("planejador");
        verify(geometriaPatioRepositorio).save(any(GeometriaPatio.class));
    }

    @Test
    void deveRejeitarPoligonoSemFechamento() throws Exception {
        GeometriaPatioRequisicaoDto requisicao = requisicaoValida();
        requisicao.setGeoJson(objectMapper.readTree("""
                {
                  "type": "Feature",
                  "geometry": {
                    "type": "Polygon",
                    "coordinates": [[
                      [-43.83740, -22.93310],
                      [-43.83735, -22.93310],
                      [-43.83735, -22.93320],
                      [-43.83739, -22.93319]
                    ]]
                  }
                }
                """));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> servico.criar(requisicao));

        assertThat(exception.getMessage()).contains("primeiro e o último ponto");
    }

    @Test
    void deveExigirVinculoOperacionalParaGeometriaDePilha() throws Exception {
        GeometriaPatioRequisicaoDto requisicao = requisicaoValida();
        requisicao.setBloco(null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> servico.criar(requisicao));

        assertThat(exception.getMessage()).contains("bloco, linha e coluna");
    }

    private GeometriaPatioRequisicaoDto requisicaoValida() throws Exception {
        GeometriaPatioRequisicaoDto requisicao = new GeometriaPatioRequisicaoDto();
        requisicao.setCodigo("pilha-a-01-01");
        requisicao.setTipo(TipoGeometriaPatio.PILHA);
        requisicao.setBloco("A");
        requisicao.setLinha(1);
        requisicao.setColuna(1);
        requisicao.setMotivo("Cadastro inicial do desenho do pátio");
        requisicao.setUsuario("planejador");
        requisicao.setGeoJson(objectMapper.readTree("""
                {
                  "type": "Feature",
                  "properties": {
                    "codigo": "PILHA-A-01-01"
                  },
                  "geometry": {
                    "type": "Polygon",
                    "coordinates": [[
                      [-43.83740, -22.93310],
                      [-43.83735, -22.93310],
                      [-43.83735, -22.93320],
                      [-43.83740, -22.93310]
                    ]]
                  }
                }
                """));
        return requisicao;
    }
}
