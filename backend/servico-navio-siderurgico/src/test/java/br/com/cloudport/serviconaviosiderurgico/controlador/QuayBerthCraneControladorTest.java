package br.com.cloudport.serviconaviosiderurgico.controlador;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

class QuayBerthCraneControladorTest {

    @Test
    void deveExporContratosDeQuayBerthECraneNaVisita() throws Exception {
        RequestMapping base = QuayBerthCraneControlador.class.getAnnotation(RequestMapping.class);
        Method quayMonitor = QuayBerthCraneControlador.class.getMethod("obterQuayMonitor", Long.class);
        Method cranePlan = QuayBerthCraneControlador.class.getMethod(
                "salvarPlanoGuindaste",
                Long.class,
                br.com.cloudport.serviconaviosiderurgico.dto.ComandoPlanoGuindasteDTO.class);
        Method produtividade = QuayBerthCraneControlador.class.getMethod("obterProdutividadeCais", Long.class);

        assertArrayEquals(new String[]{"/visitas-navio/{id}"}, base.value());
        assertArrayEquals(new String[]{"/quay-monitor"}, quayMonitor.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[]{"/crane-plan"}, cranePlan.getAnnotation(PostMapping.class).value());
        assertArrayEquals(new String[]{"/produtividade-cais"}, produtividade.getAnnotation(GetMapping.class).value());
    }
}
