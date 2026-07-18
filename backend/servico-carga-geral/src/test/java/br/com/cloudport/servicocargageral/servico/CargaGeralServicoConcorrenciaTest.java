package br.com.cloudport.servicocargageral.servico;

import static br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.CategoriaReferenciaCarga.COMMODITY;
import static br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.NaturezaCarga.BREAK_BULK;
import static br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.TipoOperacaoConhecimento.IMPORTACAO;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicocargageral.comum.erro.ConflitoCadastroCargaException;
import br.com.cloudport.servicocargageral.dominio.ConhecimentoCarga;
import br.com.cloudport.servicocargageral.dominio.ItemConhecimentoCarga;
import br.com.cloudport.servicocargageral.dto.CargaGeralDTOs.CriarConhecimentoRequest;
import br.com.cloudport.servicocargageral.dto.CargaGeralDTOs.CriarItemRequest;
import br.com.cloudport.servicocargageral.dto.CargaGeralDTOs.CriarLoteRequest;
import br.com.cloudport.servicocargageral.dto.CargaGeralDTOs.CriarReferenciaRequest;
import br.com.cloudport.servicocargageral.repositorio.ConhecimentoCargaRepositorio;
import br.com.cloudport.servicocargageral.repositorio.ItemConhecimentoCargaRepositorio;
import br.com.cloudport.servicocargageral.repositorio.LoteCargaRepositorio;
import br.com.cloudport.servicocargageral.repositorio.MovimentacaoCargaRepositorio;
import br.com.cloudport.servicocargageral.repositorio.ReferenciaCargaRepositorio;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class CargaGeralServicoConcorrenciaTest {

    @Mock
    private ConhecimentoCargaRepositorio conhecimentoRepositorio;
    @Mock
    private ItemConhecimentoCargaRepositorio itemRepositorio;
    @Mock
    private LoteCargaRepositorio loteRepositorio;
    @Mock
    private MovimentacaoCargaRepositorio movimentacaoRepositorio;
    @Mock
    private ReferenciaCargaRepositorio referenciaRepositorio;

    private CargaGeralServico servico;

    @BeforeEach
    void configurar() {
        servico = new CargaGeralServico(
                conhecimentoRepositorio,
                itemRepositorio,
                loteRepositorio,
                movimentacaoRepositorio,
                referenciaRepositorio);
    }

    @Test
    void deveConverterColisaoConcorrenteDeBillOfLading() {
        CriarConhecimentoRequest request = new CriarConhecimentoRequest(
                "BL-2026-001",
                IMPORTACAO,
                "Embarcador",
                "Consignatário",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
        when(conhecimentoRepositorio.existsByNumeroIgnoreCase(request.numero())).thenReturn(false);
        when(conhecimentoRepositorio.saveAndFlush(any(ConhecimentoCarga.class)))
                .thenThrow(violacao("conhecimento_carga_numero_key"));

        assertThatThrownBy(() -> servico.criarConhecimento(request))
                .isInstanceOf(ConflitoCadastroCargaException.class)
                .hasMessage("Já existe um Bill of Lading com esse número.");
    }

    @Test
    void deveConverterColisaoConcorrenteDeSequenciaDoItem() {
        UUID conhecimentoId = UUID.randomUUID();
        ConhecimentoCarga conhecimento = new ConhecimentoCarga();
        CriarItemRequest request = new CriarItemRequest(
                1,
                "Bobina de aço",
                "STEEL_COIL",
                "COIL",
                "BUNDLE",
                BigDecimal.ONE,
                BigDecimal.ZERO,
                BigDecimal.ONE,
                null,
                null,
                false,
                null,
                null,
                null,
                null);
        when(conhecimentoRepositorio.findById(conhecimentoId)).thenReturn(Optional.of(conhecimento));
        when(conhecimentoRepositorio.saveAndFlush(conhecimento))
                .thenThrow(violacao("uk_item_conhecimento_sequencia"));

        assertThatThrownBy(() -> servico.adicionarItem(conhecimentoId, request))
                .isInstanceOf(ConflitoCadastroCargaException.class)
                .hasMessage("A sequência do item já existe no conhecimento.");
    }

    @Test
    void deveConverterColisaoConcorrenteDeCargoLot() {
        UUID itemId = UUID.randomUUID();
        ItemConhecimentoCarga item = new ItemConhecimentoCarga();
        CriarLoteRequest request = new CriarLoteRequest(
                "LOT-2026-001",
                BREAK_BULK,
                BigDecimal.ONE,
                BigDecimal.ZERO,
                BigDecimal.ONE,
                "UN",
                null,
                null,
                null,
                null,
                null,
                null,
                null);
        when(loteRepositorio.existsByCodigoIgnoreCase(request.codigo())).thenReturn(false);
        when(itemRepositorio.findDetalhadoById(itemId)).thenReturn(Optional.of(item));
        when(itemRepositorio.saveAndFlush(item))
                .thenThrow(violacao("lote_carga_codigo_key"));

        assertThatThrownBy(() -> servico.adicionarLote(itemId, request))
                .isInstanceOf(ConflitoCadastroCargaException.class)
                .hasMessage("Já existe um cargo lot com esse código.");
    }

    @Test
    void deveConverterColisaoConcorrenteDeReferencia() {
        CriarReferenciaRequest request = new CriarReferenciaRequest(
                COMMODITY,
                "STEEL_COIL",
                "Bobina de aço",
                null,
                true);
        when(referenciaRepositorio.existsByCategoriaAndCodigoIgnoreCase(request.categoria(), request.codigo()))
                .thenReturn(false);
        when(referenciaRepositorio.saveAndFlush(any()))
                .thenThrow(violacao("uk_referencia_carga_categoria_codigo"));

        assertThatThrownBy(() -> servico.criarReferencia(request))
                .isInstanceOf(ConflitoCadastroCargaException.class)
                .hasMessage("A referência já existe nessa categoria.");
    }

    private DataIntegrityViolationException violacao(String restricao) {
        SQLException sqlException = new SQLException("violação de unicidade", "23505");
        ConstraintViolationException hibernateException =
                new ConstraintViolationException("restrição violada", sqlException, restricao);
        return new DataIntegrityViolationException("falha ao persistir", hibernateException);
    }
}
