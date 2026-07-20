package br.com.cloudport.servicogate.app.verificacao;

import br.com.cloudport.servicogate.app.gestor.GateFlowService;
import br.com.cloudport.servicogate.app.gestor.dto.GateFlowRequest;
import br.com.cloudport.servicogate.app.operacional.GateOperacionalService;
import java.lang.reflect.Method;
import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.Advisor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class MotoristaVerificacaoAdvisorConfiguration {

    @Bean
    public Advisor motoristaVerificacaoAvancoAdvisor(MotoristaVerificacaoService verificacaoService) {
        StaticMethodMatcherPointcut pointcut = new StaticMethodMatcherPointcut() {
            @Override
            public boolean matches(Method method, Class<?> targetClass) {
                return GateOperacionalService.class.isAssignableFrom(targetClass)
                        && "avancarVisita".equals(method.getName())
                        && method.getParameterCount() == 2;
            }
        };
        Advice advice = (MethodInterceptor) invocation -> {
            Long visitaId = (Long) invocation.getArguments()[0];
            verificacaoService.exigirVerificacaoVisita(visitaId);
            return invocation.proceed();
        };
        DefaultPointcutAdvisor advisor = new DefaultPointcutAdvisor(pointcut, advice);
        advisor.setOrder(Ordered.HIGHEST_PRECEDENCE + 50);
        return advisor;
    }

    @Bean
    public Advisor motoristaVerificacaoEntradaAdvisor(MotoristaVerificacaoService verificacaoService) {
        StaticMethodMatcherPointcut pointcut = new StaticMethodMatcherPointcut() {
            @Override
            public boolean matches(Method method, Class<?> targetClass) {
                return GateFlowService.class.isAssignableFrom(targetClass)
                        && "registrarEntrada".equals(method.getName())
                        && method.getParameterCount() == 1;
            }
        };
        Advice advice = (MethodInterceptor) invocation -> {
            GateFlowRequest request = (GateFlowRequest) invocation.getArguments()[0];
            verificacaoService.exigirVerificacaoEntrada(request);
            return invocation.proceed();
        };
        DefaultPointcutAdvisor advisor = new DefaultPointcutAdvisor(pointcut, advice);
        advisor.setOrder(Ordered.HIGHEST_PRECEDENCE + 50);
        return advisor;
    }
}
