package br.com.cloudport.servicogate.app.gestor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicogate.app.gestor.dto.RetiradaDiretaNavioDTO;
import br.com.cloudport.servicogate.app.gestor.dto.RetiradaDiretaNavioRequest;
import br.com.cloudport.servicogate.exception.BusinessException;
import br.com.cloudport.servicogate.model.RetiradaDiretaNavio;
import br.com.cloudport.servicogate.model.enums.StatusRetiradaDiretaNavio;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class RetiradaDiretaNavioServiceTest {

    @Mock
    private RetiradaDiretaNavioRepository repository;

    private RetiradaDiretaNavioService service;

    @BeforeEach
    void setUp() {
        service = new RetiradaDiretaNavioService(repository);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "operador.gate",
                        "n/a",
                        List.of(new SimpleGrantedAuthority("ROLE_OPERADOR_GATE"))
                )
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deveRegistrarSaidaDiretaDeTrator() {
        RetiradaDiretaNavioRequest request = requestValido();
        when(repository.findByCodigoAutorizacaoIgnoreCase("AUT-2026-001")).thenReturn(Optional.empty());
        when(repository.findByIdentificadorCargaIgnoreCase("TRATOR-CHASSI-001")).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(RetiradaDiretaNavio.class))).thenAnswer(invocation -> {
            RetiradaDiretaNavio entity = invocation.getArgument(0);
            entity.setId(10L);
            entity.setCreatedAt(LocalDateTime.now());
            return entity;
        });

        RetiradaDiretaNavioDTO result = service.processar(request);

        assertEquals(10L, result.getId());
        assertEquals("AUT-2026-001", result.getCodigoAutorizacao());
        assertEquals("TRATOR-CHASSI-001", result.getIdentificadorCarga());
        assertEquals("TRATOR", result.getTipoCarga());
        assertEquals("FINALIZADA", result.getStatus());
        assertEquals("operador.gate", result.getOperador());
        verify(repository).saveAndFlush(any(RetiradaDiretaNavio.class));
    }

    @Test
    void deveSerIdempotenteParaMesmaAutorizacaoECarga() {
        RetiradaDiretaNavio existente = retiradaExistente();
        when(repository.findByCodigoAutorizacaoIgnoreCase("AUT-2026-001"))
                .thenReturn(Optional.of(existente));

        RetiradaDiretaNavioDTO result = service.processar(requestValido());

        assertEquals(20L, result.getId());
        assertEquals("TRATOR-CHASSI-001", result.getIdentificadorCarga());
        verify(repository, never()).saveAndFlush(any(RetiradaDiretaNavio.class));
    }

    @Test
    void deveRejeitarSaidaSemLiberacaoAduaneira() {
        RetiradaDiretaNavioRequest request = requestValido();
        request.setLiberacaoAduaneiraConfirmada(false);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.processar(request));

        assertTrue(exception.getMessage().contains("liberação aduaneira"));
        verify(repository, never()).saveAndFlush(any(RetiradaDiretaNavio.class));
    }

    @Test
    void deveRejeitarCargaQueJaSaiu() {
        when(repository.findByCodigoAutorizacaoIgnoreCase("AUT-2026-001")).thenReturn(Optional.empty());
        when(repository.findByIdentificadorCargaIgnoreCase("TRATOR-CHASSI-001"))
                .thenReturn(Optional.of(retiradaExistente()));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.processar(requestValido()));

        assertTrue(exception.getMessage().contains("já saiu pelo gate"));
        verify(repository, never()).saveAndFlush(any(RetiradaDiretaNavio.class));
    }

    private RetiradaDiretaNavioRequest requestValido() {
        RetiradaDiretaNavioRequest request = new RetiradaDiretaNavioRequest();
        request.setCodigoAutorizacao("aut-2026-001");
        request.setIdentificadorCarga("trator-chassi-001");
        request.setTipoCarga("trator");
        request.setVisitaNavio("vv-2026-009");
        request.setClienteNome("Cliente Teste");
        request.setClienteDocumento("123.456.789-00");
        request.setDocumentosValidados(true);
        request.setLiberacaoAduaneiraConfirmada(true);
        request.setCargaDescarregada(true);
        request.setCondutorHabilitado(true);
        request.setTimestamp(LocalDateTime.now().minusMinutes(1));
        request.setObservacao("Retirada direta após descarga RoRo");
        return request;
    }

    private RetiradaDiretaNavio retiradaExistente() {
        RetiradaDiretaNavio entity = new RetiradaDiretaNavio();
        entity.setId(20L);
        entity.setCodigoAutorizacao("AUT-2026-001");
        entity.setIdentificadorCarga("TRATOR-CHASSI-001");
        entity.setTipoCarga("TRATOR");
        entity.setVisitaNavio("VV-2026-009");
        entity.setClienteNome("Cliente Teste");
        entity.setClienteDocumento("12345678900");
        entity.setStatus(StatusRetiradaDiretaNavio.FINALIZADA);
        entity.setSaidaEm(LocalDateTime.now().minusMinutes(5));
        entity.setOperador("operador.gate");
        entity.setCreatedAt(LocalDateTime.now().minusMinutes(5));
        return entity;
    }
}
