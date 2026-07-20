package br.com.cloudport.servicoyard.estivagembulk.servico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicoyard.estivagembulk.dto.NavioGranelDto;
import br.com.cloudport.servicoyard.estivagembulk.modelo.ClasseNavio;
import br.com.cloudport.servicoyard.estivagembulk.modelo.NavioGranel;
import br.com.cloudport.servicoyard.estivagembulk.modelo.PoraoNavio;
import br.com.cloudport.servicoyard.estivagembulk.modelo.SetorTanktop;
import br.com.cloudport.servicoyard.estivagembulk.repositorio.NavioGranelRepositorio;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NavioGranelConsultaServicoTest {

    @Mock
    private NavioGranelRepositorio repositorio;

    @InjectMocks
    private NavioGranelConsultaServico servico;

    @Test
    void deveMapearModeloComPoroesESetoresOrdenados() {
        NavioGranel modelo = criarModelo();
        when(repositorio.findByIsTemplateTrue()).thenReturn(List.of(modelo));

        List<NavioGranelDto> resultado = servico.listarModelos();

        assertEquals(1, resultado.size());
        NavioGranelDto dto = resultado.get(0);
        assertEquals("Modelo Panamax", dto.getNome());
        assertEquals("PANAMAX", dto.getClasse());
        assertTrue(dto.isTemplate());
        assertEquals(2, dto.getTotalPoroes());
        assertEquals(1, dto.getPoroes().get(0).getNumero());
        assertEquals("CENTRO", dto.getPoroes().get(0).getSetores().get(0).getNome());
        assertEquals(25.5, dto.getPoroes().get(0).getSetores().get(0).getCapacidadeTM2());
    }

    private NavioGranel criarModelo() {
        NavioGranel modelo = new NavioGranel();
        modelo.setId(10L);
        modelo.setNome("Modelo Panamax");
        modelo.setImo("IMO-MODELO");
        modelo.setClasse(ClasseNavio.PANAMAX);
        modelo.setVersaoPerfil(3L);
        modelo.setTemplate(true);
        modelo.setLpp(225.0);
        modelo.setBoca(32.2);

        PoraoNavio segundoPorao = new PoraoNavio();
        segundoPorao.setId(102L);
        segundoPorao.setNumero(2);
        segundoPorao.setNavio(modelo);

        PoraoNavio primeiroPorao = new PoraoNavio();
        primeiroPorao.setId(101L);
        primeiroPorao.setNumero(1);
        primeiroPorao.setNavio(modelo);

        SetorTanktop setorDireita = criarSetor(202L, "DIREITA", 20.0, primeiroPorao);
        SetorTanktop setorCentro = criarSetor(201L, "CENTRO", 25.5, primeiroPorao);
        primeiroPorao.getSetores().add(setorDireita);
        primeiroPorao.getSetores().add(setorCentro);
        modelo.getPoroes().add(segundoPorao);
        modelo.getPoroes().add(primeiroPorao);
        return modelo;
    }

    private SetorTanktop criarSetor(Long id, String nome, Double capacidade, PoraoNavio porao) {
        SetorTanktop setor = new SetorTanktop();
        setor.setId(id);
        setor.setNome(nome);
        setor.setCapacidadeTM2(capacidade);
        setor.setPorao(porao);
        return setor;
    }
}
