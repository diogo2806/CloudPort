package br.com.cloudport.servicogate.app.verificacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicogate.app.gestor.GateFlowService;
import br.com.cloudport.servicogate.app.gestor.dto.GateFlowRequest;
import br.com.cloudport.servicogate.app.operacional.GateOperacionalService;
import br.com.cloudport.servicogate.app.operacional.dto.GateOperacionalDtos.AdvanceVisitRequest;
import java.lang.reflect.Method;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.DefaultPointcutAdvisor;

class MotoristaVerificacaoAdvisorConfigurationTest {

    private final MotoristaVerificacaoService service = mock(MotoristaVerificacaoService.class);
    private final MotoristaVerificacaoAdvisorConfiguration configuration =
            new MotoristaVerificacaoAdvisorConfiguration();

    @Test
    void deveBloquearAvancoDaTruckVisitAntesDeExecutarOServico() throws Throwable {
        DefaultPointcutAdvisor advisor = (DefaultPointcutAdvisor)
                configuration.motoristaVerificacaoAvancoAdvisor(service);
        Method method = GateOperacionalService.class.getMethod(
                "avancarVisita",
                Long.class,
                AdvanceVisitRequest.class);

        assertTrue(advisor.getPointcut().getMethodMatcher()
                .matches(method, GateOperacionalService.class));

        MethodInvocation invocation = mock(MethodInvocation.class);
        when(invocation.getArguments()).thenReturn(new Object[]{42L, null});
        when(invocation.proceed()).thenReturn("avancou");

        Object result = ((MethodInterceptor) advisor.getAdvice()).invoke(invocation);

        assertEquals("avancou", result);
        verify(service).exigirVerificacaoVisita(42L);
        verify(invocation).proceed();
    }

    @Test
    void deveBloquearEntradaAntesDeExecutarOFluxoDoGate() throws Throwable {
        DefaultPointcutAdvisor advisor = (DefaultPointcutAdvisor)
                configuration.motoristaVerificacaoEntradaAdvisor(service);
        Method method = GateFlowService.class.getMethod("registrarEntrada", GateFlowRequest.class);
        GateFlowRequest request = mock(GateFlowRequest.class);

        assertTrue(advisor.getPointcut().getMethodMatcher()
                .matches(method, GateFlowService.class));

        MethodInvocation invocation = mock(MethodInvocation.class);
        when(invocation.getArguments()).thenReturn(new Object[]{request});
        when(invocation.proceed()).thenReturn("entrou");

        Object result = ((MethodInterceptor) advisor.getAdvice()).invoke(invocation);

        assertEquals("entrou", result);
        verify(service).exigirVerificacaoEntrada(request);
        verify(invocation).proceed();
    }
}
