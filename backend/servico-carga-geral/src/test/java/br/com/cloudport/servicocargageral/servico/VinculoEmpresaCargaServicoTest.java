package br.com.cloudport.servicocargageral.servico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicocargageral.dominio.AuditoriaVinculoEmpresaCarga;
import br.com.cloudport.servicocargageral.dominio.Empresa;
import br.com.cloudport.servicocargageral.dominio.Empresa.PapelEmpresa;
import br.com.cloudport.servicocargageral.dominio.VinculoEmpresaCarga;
import br.com.cloudport.servicocargageral.dominio.VinculoEmpresaCarga.TipoRecursoCarga;
import br.com.cloudport.servicocargageral.dto.VinculoEmpresaCargaDTOs.AtualizarVinculosRequest;
import br.com.cloudport.servicocargageral.dto.VinculoEmpresaCargaDTOs.VinculoEmpresaRequest;
import br.com.cloudport.servicocargageral.dto.VinculoEmpresaCargaDTOs.VinculoEmpresaResposta;
import br.com.cloudport.servicocargageral.repositorio.AuditoriaVinculoEmpresaCargaRepositorio;
import br.com.cloudport.servicocargageral.repositorio.ConhecimentoCargaRepositorio;
import br.com.cloudport.servicocargageral.repositorio.EmpresaRepositorio;
import br.com.cloudport.servicocargageral.repositorio.LoteCargaRepositorio;
import br.com.cloudport.servicocargageral.repositorio.VinculoEmpresaCargaRepositorio;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class VinculoEmpresaCargaServicoTest {

    @Mock private VinculoEmpresaCargaRepositorio vinculoRepositorio;
    @Mock private AuditoriaVinculoEmpresaCargaRepositorio auditoriaRepositorio;
    @Mock private EmpresaRepositorio empresaRepositorio;
    @Mock private ConhecimentoCargaRepositorio conhecimentoRepositorio;
    @Mock private LoteCargaRepositorio loteRepositorio;

    private VinculoEmpresaCargaServico servico;

    @BeforeEach
    void configurar() {
        servico = new VinculoEmpresaCargaServico(
                vinculoRepositorio,
                auditoriaRepositorio,
                empresaRepositorio,
                conhecimentoRepositorio,
                loteRepositorio);
    }

    @Test
    void deveVincularEmpresaAtivaComPapelCompativel() {
        UUID conhecimentoId = UUID.randomUUID();
        Empresa empresa = empresa(PapelEmpresa.CLIENTE, true);
        AtomicReference<VinculoEmpresaCarga> salvo = new AtomicReference<>();
        AtomicInteger consultas = new AtomicInteger();

        when(conhecimentoRepositorio.existsById(conhecimentoId)).thenReturn(true);
        when(empresaRepositorio.findById(empresa.getId())).thenReturn(Optional.of(empresa));
        when(vinculoRepositorio.findByTipoRecursoAndRecursoIdOrderByPapelAsc(
                TipoRecursoCarga.CONHECIMENTO, conhecimentoId))
                .thenAnswer(invocacao -> consultas.getAndIncrement() == 0
                        ? List.of()
                        : List.of(salvo.get()));
        when(vinculoRepositorio.save(any(VinculoEmpresaCarga.class))).thenAnswer(invocacao -> {
            VinculoEmpresaCarga vinculo = invocacao.getArgument(0);
            ReflectionTestUtils.setField(vinculo, "id", UUID.randomUUID());
            salvo.set(vinculo);
            return vinculo;
        });

        List<VinculoEmpresaResposta> resposta = servico.atualizarConhecimento(
                conhecimentoId,
                new AtualizarVinculosRequest(List.of(
                        new VinculoEmpresaRequest(PapelEmpresa.CLIENTE, empresa.getId()))));

        assertEquals(1, resposta.size());
        assertEquals(PapelEmpresa.CLIENTE, resposta.get(0).papel());
        assertEquals(empresa.getId(), resposta.get(0).empresaId());
        verify(vinculoRepositorio).save(any(VinculoEmpresaCarga.class));
        verify(auditoriaRepositorio).save(any(AuditoriaVinculoEmpresaCarga.class));
    }

    @Test
    void deveRejeitarEmpresaInexistente() {
        UUID loteId = UUID.randomUUID();
        UUID empresaId = UUID.randomUUID();
        when(loteRepositorio.existsById(loteId)).thenReturn(true);
        when(vinculoRepositorio.findByTipoRecursoAndRecursoIdOrderByPapelAsc(
                TipoRecursoCarga.LOTE, loteId)).thenReturn(List.of());
        when(empresaRepositorio.findById(empresaId)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> servico.atualizarLote(
                        loteId,
                        new AtualizarVinculosRequest(List.of(
                                new VinculoEmpresaRequest(PapelEmpresa.CLIENTE, empresaId)))));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verify(vinculoRepositorio, never()).save(any(VinculoEmpresaCarga.class));
    }

    @Test
    void deveRejeitarEmpresaInativa() {
        UUID loteId = UUID.randomUUID();
        Empresa empresa = empresa(PapelEmpresa.CLIENTE, false);
        prepararLoteSemVinculos(loteId, empresa);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> servico.atualizarLote(
                        loteId,
                        new AtualizarVinculosRequest(List.of(
                                new VinculoEmpresaRequest(PapelEmpresa.CLIENTE, empresa.getId())))));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verify(vinculoRepositorio, never()).save(any(VinculoEmpresaCarga.class));
    }

    @Test
    void deveRejeitarEmpresaSemPapelSolicitado() {
        UUID loteId = UUID.randomUUID();
        Empresa empresa = empresa(PapelEmpresa.OPERADOR, true);
        prepararLoteSemVinculos(loteId, empresa);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> servico.atualizarLote(
                        loteId,
                        new AtualizarVinculosRequest(List.of(
                                new VinculoEmpresaRequest(PapelEmpresa.CLIENTE, empresa.getId())))));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verify(vinculoRepositorio, never()).save(any(VinculoEmpresaCarga.class));
    }

    private void prepararLoteSemVinculos(UUID loteId, Empresa empresa) {
        when(loteRepositorio.existsById(loteId)).thenReturn(true);
        when(vinculoRepositorio.findByTipoRecursoAndRecursoIdOrderByPapelAsc(
                TipoRecursoCarga.LOTE, loteId)).thenReturn(List.of());
        when(empresaRepositorio.findById(empresa.getId())).thenReturn(Optional.of(empresa));
    }

    private Empresa empresa(PapelEmpresa papel, boolean ativa) {
        Empresa empresa = new Empresa();
        ReflectionTestUtils.setField(empresa, "id", UUID.randomUUID());
        empresa.setCodigo("EMP-001");
        empresa.setRazaoSocial("Empresa Operacional S.A.");
        empresa.setNomeFantasia("Empresa Operacional");
        empresa.setDocumento("00.000.000/0001-00");
        empresa.setPais("BRASIL");
        empresa.setAtivo(ativa);
        empresa.setPapeis(new LinkedHashSet<>(List.of(papel)));
        return empresa;
    }
}
