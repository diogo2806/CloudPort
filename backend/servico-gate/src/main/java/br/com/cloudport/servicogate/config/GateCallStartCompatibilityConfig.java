package br.com.cloudport.servicogate.config;

import br.com.cloudport.servicogate.app.gestor.GateCallRepository;
import br.com.cloudport.servicogate.model.GateCall;
import br.com.cloudport.servicogate.model.enums.GateCallStatus;
import java.time.LocalDateTime;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class GateCallStartCompatibilityConfig implements WebMvcConfigurer {

    private final ObjectProvider<GateCallRepository> repositoryProvider;

    public GateCallStartCompatibilityConfig(ObjectProvider<GateCallRepository> repositoryProvider) {
        this.repositoryProvider = repositoryProvider;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        GateCallRepository repository = repositoryProvider.getIfAvailable();
        if (repository != null) {
            registry.addInterceptor(new LegacyStartInterceptor(repository))
                    .addPathPatterns("/api/gate/calls/*/start");
        }
    }

    private static class LegacyStartInterceptor implements HandlerInterceptor {
        private final GateCallRepository repository;

        private LegacyStartInterceptor(GateCallRepository repository) {
            this.repository = repository;
        }

        @Override
        @Transactional
        public boolean preHandle(HttpServletRequest request,
                                 HttpServletResponse response,
                                 Object handler) {
            if (!HttpMethod.POST.matches(request.getMethod())) {
                return true;
            }
            Long id = extrairId(request.getRequestURI());
            if (id == null) {
                return true;
            }
            GateCall chamado = repository.findById(id).orElse(null);
            if (chamado != null && chamado.getStatus() == GateCallStatus.CHAMADO) {
                chamado.setStatus(GateCallStatus.ACEITO);
                chamado.setAceitoEm(LocalDateTime.now());
                repository.save(chamado);
            }
            return true;
        }

        private Long extrairId(String uri) {
            String[] partes = uri.split("/");
            for (int i = 0; i < partes.length; i++) {
                if ("calls".equals(partes[i]) && i + 1 < partes.length) {
                    try {
                        return Long.valueOf(partes[i + 1]);
                    } catch (NumberFormatException ex) {
                        return null;
                    }
                }
            }
            return null;
        }
    }
}
