package br.com.cloudport.servicocargageral.servico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicocargageral.dominio.AmarradoCarga;
import br.com.cloudport.servicocargageral.dominio.ConhecimentoCarga;
import br.com.cloudport.servicocargageral.dominio.ItemConhecimentoCarga;
import br.com.cloudport.servicocargageral.dominio.LoteCarga;
import br.com.cloudport.servicocargageral.dto.AmarradoCargaDTOs.AmarradoResposta;
import br.com.cloudport.servicocargageral.dto.AmarradoCargaDTOs.CriarAmarradoRequest;
import br.com.cloudport.servicocargageral.repositorio.AmarradoCargaRepositorio;
import br.com.cloudport.servicocargageral.repositorio.LoteCargaRepositorio;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AmarradoCargaServicoTest {

    @Mock
    private AmarradoCargaRepositorio amarradoRepositorio;

    @Mock
    private LoteCargaRepositorio loteRepositorio;

    private AmarradoCargaServico servico;

    @BeforeEach
    void configurar() {
        servico = new AmarradoCargaServico(amarradoRepositorio, loteRepositorio, "PULMAO_OPERACIONAL");
    }

    @Test
    void deveIdentificarAmarradoMistoEDirecionarParaAreaParametrizada() {
        LoteCarga loteCoberto = lote("LOT-001", "DRY_COVERED", "VISITA-001", 1);
        LoteCarga lotePatio = lote("LOT-002", "OPEN_YARD", "VISITA-001", 2);
        CriarAmarradoRequest request = new CriarAmarradoRequest(
                "am-001",
                "VISITA-001",
                List.of(lotePatio.getId(), loteCoberto.getId()));

        when(amarradoRepositorio.existsByCodigoIgnoreCase("AM-001")).thenReturn(false);
        when(loteRepositorio.findAllById(request.loteIds())).thenReturn(List.of(lotePatio, loteCoberto));
        when(amarradoRepositorio.existsByLotes_Id(any(UUID.class))).thenReturn(false);
        when(amarradoRepositorio.save(any(AmarradoCarga.class))).thenAnswer(invocacao -> persistir(invocacao.getArgument(0)));

        AmarradoResposta resposta = servico.criar(request);

        assertEquals("AM-001", resposta.codigo());
        assertTrue(resposta.misto());
        assertTrue(resposta.integro());
        assertEquals(2, resposta.quantidadeReferencias());
        assertEquals(List.of("DRY_COVERED", "OPEN_YARD"), resposta.gruposArmazenagem());
        assertEquals("PULMAO_OPERACIONAL", resposta.destinoDirecionamento());
        assertTrue(resposta.motivoDirecionamento().contains("grupos de armazenagem distintos"));
        assertNotNull(resposta.direcionadoEm());
        assertEquals(List.of("LOT-001", "LOT-002"), resposta.referencias().stream()
                .map(referencia -> referencia.loteCodigo())
                .toList());
        verify(amarradoRepositorio).save(any(AmarradoCarga.class));
    }

    @Test
    void deveDirecionarAmarradoNaoMistoParaPilhaDoGrupo() {
        LoteCarga loteUm = lote("LOT-001", "DRY_COVERED", "VISITA-001", 1);
        LoteCarga loteDois = lote("LOT-002", "DRY_COVERED", "VISITA-001", 2);
        CriarAmarradoRequest request = new CriarAmarradoRequest(
                "AM-002",
                "VISITA-001",
                List.of(loteUm.getId(), loteDois.getId()));

        when(loteRepositorio.findAllById(request.loteIds())).thenReturn(List.of(loteUm, loteDois));
        when(amarradoRepositorio.existsByLotes_Id(any(UUID.class))).thenReturn(false);
        when(amarradoRepositorio.save(any(AmarradoCarga.class))).thenAnswer(invocacao -> persistir(invocacao.getArgument(0)));

        AmarradoResposta resposta = servico.criar(request);

        assertFalse(resposta.misto());
        assertEquals(List.of("DRY_COVERED"), resposta.gruposArmazenagem());
        assertEquals("DRY_COVERED", resposta.destinoDirecionamento());
        assertTrue(resposta.motivoDirecionamento().contains("mesmo grupo"));
    }

    @Test
    void deveRejeitarCargoLotJaVinculadoAOutroAmarrado() {
        LoteCarga lote = lote("LOT-001", "DRY_COVERED", "VISITA-001", 1);
        CriarAmarradoRequest request = new CriarAmarradoRequest(
                "AM-003",
                "VISITA-001",
                List.of(lote.getId()));

        when(loteRepositorio.findAllById(request.loteIds())).thenReturn(List.of(lote));
        when(amarradoRepositorio.existsByLotes_Id(lote.getId())).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> servico.criar(request));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verify(amarradoRepositorio, never()).save(any(AmarradoCarga.class));
    }

    @Test
    void deveRejeitarReferenciasDeVisitasDiferentes() {
        LoteCarga lote = lote("LOT-001", "DRY_COVERED", "VISITA-OUTRA", 1);
        CriarAmarradoRequest request = new CriarAmarradoRequest(
                "AM-004",
                "VISITA-001",
                List.of(lote.getId()));

        when(loteRepositorio.findAllById(request.loteIds())).thenReturn(List.of(lote));
        when(amarradoRepositorio.existsByLotes_Id(lote.getId())).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> servico.criar(request));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verify(amarradoRepositorio, never()).save(any(AmarradoCarga.class));
    }

    private LoteCarga lote(String codigo, String grupoArmazenagem, String visitaNavioId, int sequencia) {
        ConhecimentoCarga conhecimento = new ConhecimentoCarga();
        conhecimento.setNumero("BL-" + sequencia);

        ItemConhecimentoCarga item = new ItemConhecimentoCarga();
        ReflectionTestUtils.setField(item, "id", UUID.randomUUID());
        item.setConhecimento(conhecimento);
        item.setSequencia(sequencia);
        item.setDescricao("Referência " + sequencia);
        item.setCodigoArmazenagem(grupoArmazenagem);

        LoteCarga lote = new LoteCarga();
        ReflectionTestUtils.setField(lote, "id", UUID.randomUUID());
        lote.setCodigo(codigo);
        lote.setItem(item);
        lote.setVisitaNavioId(visitaNavioId);
        return lote;
    }

    private AmarradoCarga persistir(AmarradoCarga amarrado) {
        OffsetDateTime agora = OffsetDateTime.now();
        ReflectionTestUtils.setField(amarrado, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(amarrado, "criadoEm", agora);
        ReflectionTestUtils.setField(amarrado, "atualizadoEm", agora);
        return amarrado;
    }
}
