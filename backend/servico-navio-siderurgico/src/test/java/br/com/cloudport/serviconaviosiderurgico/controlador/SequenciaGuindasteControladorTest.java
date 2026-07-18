package br.com.cloudport.serviconaviosiderurgico.controlador;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import br.com.cloudport.serviconaviosiderurgico.dto.ComandosSequenciaGuindasteDTO;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

class SequenciaGuindasteControladorTest {

    @Test
    void deveExporContratosBus1150() throws Exception {
        RequestMapping base = SequenciaGuindasteControlador.class.getAnnotation(RequestMapping.class);
        Method criar = SequenciaGuindasteControlador.class.getMethod(
                "criar", ComandosSequenciaGuindasteDTO.Criar.class);
        Method iniciar = SequenciaGuindasteControlador.class.getMethod(
                "iniciar", String.class, ComandosSequenciaGuindasteDTO.Transicao.class);
        Method pausar = SequenciaGuindasteControlador.class.getMethod(
                "pausar", String.class, ComandosSequenciaGuindasteDTO.Transicao.class);
        Method finalizar = SequenciaGuindasteControlador.class.getMethod(
                "finalizar", String.class, ComandosSequenciaGuindasteDTO.Transicao.class);
        Method cancelar = SequenciaGuindasteControlador.class.getMethod(
                "cancelar", String.class, ComandosSequenciaGuindasteDTO.Transicao.class);
        Method buscar = SequenciaGuindasteControlador.class.getMethod("buscar", String.class);
        Method historico = SequenciaGuindasteControlador.class.getMethod("listarAuditoria", String.class);

        assertArrayEquals(new String[]{"/api/crane-sequences"}, base.value());
        assertArrayEquals(new String[]{}, criar.getAnnotation(PostMapping.class).value());
        assertArrayEquals(new String[]{"/{movementId}/start"}, iniciar.getAnnotation(PostMapping.class).value());
        assertArrayEquals(new String[]{"/{movementId}/pause"}, pausar.getAnnotation(PostMapping.class).value());
        assertArrayEquals(new String[]{"/{movementId}/finish"}, finalizar.getAnnotation(PostMapping.class).value());
        assertArrayEquals(new String[]{"/{movementId}/cancel"}, cancelar.getAnnotation(PostMapping.class).value());
        assertArrayEquals(new String[]{"/{movementId}"}, buscar.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[]{"/{movementId}/history"}, historico.getAnnotation(GetMapping.class).value());
    }
}
