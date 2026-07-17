package br.com.cloudport.servicoyard.integracao.navio;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class PlanejamentoCanonicoInterceptor implements HandlerInterceptor {
    private final PlanejamentoCanonicoPersistenciaServico persistenciaServico;

    public PlanejamentoCanonicoInterceptor(PlanejamentoCanonicoPersistenciaServico persistenciaServico) {
        this.persistenciaServico = persistenciaServico;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("GET".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String caminho = request.getRequestURI();
        Long identificador = extrairIdentificador(caminho, "/api/estivagem-bulk/planos/");
        if (identificador != null) {
            persistenciaServico.validarPlanoBulk(identificador);
            return true;
        }
        identificador = extrairIdentificador(caminho, "/api/vessel-planner/planos/");
        if (identificador != null) {
            persistenciaServico.validarPlanoContainer(identificador);
        }
        return true;
    }

    private Long extrairIdentificador(String caminho, String prefixo) {
        int inicio = caminho.indexOf(prefixo);
        if (inicio < 0) return null;
        String restante = caminho.substring(inicio + prefixo.length());
        if (restante.isEmpty()) return null;
        int barra = restante.indexOf('/');
        String valor = barra >= 0 ? restante.substring(0, barra) : restante;
        try {
            return Long.valueOf(valor);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
