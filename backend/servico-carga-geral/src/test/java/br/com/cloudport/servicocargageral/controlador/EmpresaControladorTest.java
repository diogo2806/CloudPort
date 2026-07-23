package br.com.cloudport.servicocargageral.controlador;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicocargageral.dominio.Empresa;
import br.com.cloudport.servicocargageral.dominio.Empresa.PapelEmpresa;
import br.com.cloudport.servicocargageral.repositorio.EmpresaRepositorio;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class EmpresaControladorTest {
    @Mock
    private EmpresaRepositorio repositorio;

    private EmpresaControlador controlador;

    @BeforeEach
    void configurar() {
        controlador = new EmpresaControlador(repositorio);
    }

    @Test
    void deveCriarEmpresaComDadosValidos() {
        when(repositorio.existsByCodigoIgnoreCase("CLI001")).thenReturn(false);
        when(repositorio.existsByDocumentoNormalizado("12345678000190")).thenReturn(false);
        when(repositorio.save(any(Empresa.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        var resposta = controlador.criar(requisicao("CLI001", "12.345.678/0001-90"));

        assertEquals(HttpStatus.CREATED, resposta.getStatusCode());
        assertEquals("CLI001", resposta.getBody().codigo());
        verify(repositorio).save(any(Empresa.class));
    }

    @Test
    void deveRecusarCodigoDuplicado() {
        when(repositorio.existsByCodigoIgnoreCase("CLI001")).thenReturn(true);

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
            () -> controlador.criar(requisicao("CLI001", "12.345.678/0001-90")));

        assertEquals(HttpStatus.CONFLICT, erro.getStatus());
        assertTrue(erro.getReason().contains("código"));
    }

    @Test
    void deveRecusarDocumentoDuplicadoNormalizado() {
        when(repositorio.existsByCodigoIgnoreCase("CLI001")).thenReturn(false);
        when(repositorio.existsByDocumentoNormalizado("12345678000190")).thenReturn(true);

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
            () -> controlador.criar(requisicao("CLI001", "12.345.678/0001-90")));

        assertEquals(HttpStatus.CONFLICT, erro.getStatus());
        assertTrue(erro.getReason().contains("CNPJ/documento"));
    }

    @Test
    void deveListarFiltrandoPapelEStatus() {
        Empresa ativa = empresa("ATIVA", PapelEmpresa.CLIENTE, true);
        Empresa inativa = empresa("INATIVA", PapelEmpresa.TRANSPORTADORA, false);
        when(repositorio.findAllByOrderByRazaoSocialAsc()).thenReturn(List.of(ativa, inativa));

        var resultado = controlador.listar("ativa", PapelEmpresa.CLIENTE, true);

        assertEquals(1, resultado.size());
        assertEquals("ATIVA", resultado.get(0).codigo());
    }

    @Test
    void deveAtualizarStatus() {
        UUID id = UUID.randomUUID();
        Empresa empresa = empresa("CLI001", PapelEmpresa.CLIENTE, true);
        when(repositorio.findById(id)).thenReturn(Optional.of(empresa));
        when(repositorio.save(empresa)).thenReturn(empresa);

        var resposta = controlador.status(id, false);

        assertEquals(false, resposta.ativo());
    }

    @Test
    void deveRetornarNotFoundParaEmpresaInexistente() {
        UUID id = UUID.randomUUID();
        when(repositorio.findById(id)).thenReturn(Optional.empty());

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
            () -> controlador.status(id, false));

        assertEquals(HttpStatus.NOT_FOUND, erro.getStatus());
    }

    @Test
    void deveManterPermissoesCoerentes() throws Exception {
        Method listar = EmpresaControlador.class.getMethod("listar", String.class, PapelEmpresa.class, Boolean.class);
        Method criar = EmpresaControlador.class.getMethod("criar", EmpresaControlador.Requisicao.class);
        Method atualizar = EmpresaControlador.class.getMethod("atualizar", UUID.class, EmpresaControlador.Requisicao.class);
        Method status = EmpresaControlador.class.getMethod("status", UUID.class, boolean.class);

        assertTrue(listar.getAnnotation(PreAuthorize.class).value().contains("PLANEJADOR"));
        assertTrue(listar.getAnnotation(PreAuthorize.class).value().contains("OPERADOR_GATE"));
        assertEquals("hasRole('ADMIN_PORTO')", criar.getAnnotation(PreAuthorize.class).value());
        assertEquals("hasRole('ADMIN_PORTO')", atualizar.getAnnotation(PreAuthorize.class).value());
        assertEquals("hasRole('ADMIN_PORTO')", status.getAnnotation(PreAuthorize.class).value());
    }

    private EmpresaControlador.Requisicao requisicao(String codigo, String documento) {
        return new EmpresaControlador.Requisicao(codigo, "Cliente Teste", "Cliente", documento, null,
            null, null, "cliente@teste.com", null, "BRASIL", null, Set.of(PapelEmpresa.CLIENTE));
    }

    private Empresa empresa(String codigo, PapelEmpresa papel, boolean ativa) {
        Empresa empresa = new Empresa();
        empresa.setCodigo(codigo);
        empresa.setRazaoSocial(codigo + " Razão Social");
        empresa.setNomeFantasia(codigo);
        empresa.setDocumento(codigo + "-DOC");
        empresa.setPais("BRASIL");
        empresa.setPapeis(Set.of(papel));
        empresa.setAtivo(ativa);
        return empresa;
    }
}
