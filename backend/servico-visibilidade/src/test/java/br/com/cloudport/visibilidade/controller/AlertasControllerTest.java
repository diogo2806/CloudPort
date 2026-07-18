package br.com.cloudport.visibilidade.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cloudport.visibilidade.dto.AcaoAlertaRequest;
import br.com.cloudport.visibilidade.entity.Alerta;
import br.com.cloudport.visibilidade.service.AlertasService;
import java.security.Principal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class AlertasControllerTest {

    @Mock
    private AlertasService alertasService;

    @Mock
    private Principal principal;

    private AlertasController controller;

    @BeforeEach
    void configurar() {
        controller = new AlertasController(alertasService);
    }

    @Test
    void devePriorizarUsuarioAutenticadoAoReconhecer() {
        Alerta alerta = new Alerta();
        alerta.setId(7L);
        when(principal.getName()).thenReturn("operador-autenticado");
        when(alertasService.reconhecerAlerta(7L, "operador-autenticado")).thenReturn(alerta);

        Alerta resultado = controller.reconhecer(7L, new AcaoAlertaRequest(), principal);

        assertEquals(7L, resultado.getId());
        verify(alertasService).reconhecerAlerta(7L, "operador-autenticado");
    }
}
