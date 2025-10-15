package br.com.cloudport.servicoautenticacao.app.role;

import br.com.cloudport.servicoautenticacao.app.role.dto.PapelDTO;
import br.com.cloudport.servicoautenticacao.model.Papel;
import br.com.cloudport.servicoautenticacao.repositories.PapelRepositorio;
import br.com.cloudport.servicoautenticacao.repositories.UsuarioPapelRepositorio;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PapelServicoTest {

    @Mock
    private PapelRepositorio papelRepositorio;

    @Mock
    private UsuarioPapelRepositorio usuarioPapelRepositorio;

    @InjectMocks
    private PapelServico papelServico;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void salvarPapel_sucesso() {
        PapelDTO dto = new PapelDTO();
        dto.setNome("ADMIN");

        when(papelRepositorio.findByNome("ADMIN")).thenReturn(Optional.empty());
        when(papelRepositorio.save(any(Papel.class))).thenAnswer(invocation -> {
            Papel papel = invocation.getArgument(0);
            papel.setId(1L);
            return papel;
        });

        PapelDTO resultado = papelServico.salvarPapel(dto);

        assertNotNull(resultado.getId());
        assertEquals("ADMIN", resultado.getNome());
        verify(papelRepositorio).save(any(Papel.class));
    }

    @Test
    void salvarPapel_nomeDuplicado_lancaExcecao() {
        PapelDTO dto = new PapelDTO();
        dto.setNome("ADMIN");

        when(papelRepositorio.findByNome("ADMIN")).thenReturn(Optional.of(new Papel("ADMIN")));

        assertThrows(IllegalArgumentException.class, () -> papelServico.salvarPapel(dto));
        verify(papelRepositorio, never()).save(any());
    }
}
