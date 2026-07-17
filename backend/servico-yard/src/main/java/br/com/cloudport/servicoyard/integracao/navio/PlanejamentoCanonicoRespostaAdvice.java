package br.com.cloudport.servicoyard.integracao.navio;

import br.com.cloudport.servicoyard.estivagembulk.dto.PlanoEstivaBulkDto;
import br.com.cloudport.servicoyard.estivagembulk.modelo.NavioGranel;
import br.com.cloudport.servicoyard.estivagembulk.modelo.PlanoEstivaBulk;
import br.com.cloudport.servicoyard.vesselplanner.dto.EstivagemPlanDto;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@ControllerAdvice
public class PlanejamentoCanonicoRespostaAdvice implements ResponseBodyAdvice<Object> {
    private final PlanejamentoCanonicoPersistenciaServico persistenciaServico;

    public PlanejamentoCanonicoRespostaAdvice(PlanejamentoCanonicoPersistenciaServico persistenciaServico) {
        this.persistenciaServico = persistenciaServico;
    }

    @Override
    public boolean supports(MethodParameter returnType,
            Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request, ServerHttpResponse response) {
        if (!(request instanceof ServletServerHttpRequest)) {
            return body;
        }
        HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();
        String caminho = servletRequest.getRequestURI();
        boolean post = "POST".equalsIgnoreCase(servletRequest.getMethod());

        if (post && "/api/estivagem-bulk/navios".equals(caminho) && body instanceof NavioGranel) {
            Long navioCadastroId = parametroLongObrigatorio(servletRequest, "navioCadastroId");
            return persistenciaServico.vincularPerfil(((NavioGranel) body).getId(), navioCadastroId);
        }
        if (post && "/api/estivagem-bulk/planos".equals(caminho) && body instanceof PlanoEstivaBulk) {
            Long visitaNavioId = parametroLongObrigatorio(servletRequest, "visitaNavioId");
            return persistenciaServico.vincularPlanoBulk(((PlanoEstivaBulk) body).getId(), visitaNavioId);
        }
        if (post && "/api/vessel-planner/planos".equals(caminho) && body instanceof EstivagemPlanDto) {
            Long visitaNavioId = parametroLongObrigatorio(servletRequest, "visitaNavioId");
            EstivagemPlanDto dto = (EstivagemPlanDto) body;
            persistenciaServico.vincularPlanoContainer(dto.getId(), visitaNavioId);
            persistenciaServico.enriquecer(dto);
            return dto;
        }
        if (body instanceof PlanoEstivaBulkDto) {
            persistenciaServico.enriquecer((PlanoEstivaBulkDto) body);
        } else if (body instanceof EstivagemPlanDto) {
            persistenciaServico.enriquecer((EstivagemPlanDto) body);
        } else if (body instanceof List<?>) {
            for (Object item : (List<?>) body) {
                if (item instanceof PlanoEstivaBulkDto) {
                    persistenciaServico.enriquecer((PlanoEstivaBulkDto) item);
                } else if (item instanceof EstivagemPlanDto) {
                    persistenciaServico.enriquecer((EstivagemPlanDto) item);
                }
            }
        }
        return body;
    }

    private Long parametroLongObrigatorio(HttpServletRequest request, String nome) {
        String valor = request.getParameter(nome);
        if (valor == null || valor.trim().isEmpty()) {
            throw new IllegalArgumentException("O parâmetro " + nome + " deve ser informado.");
        }
        try {
            return Long.valueOf(valor);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("O parâmetro " + nome + " deve ser numérico.");
        }
    }
}
