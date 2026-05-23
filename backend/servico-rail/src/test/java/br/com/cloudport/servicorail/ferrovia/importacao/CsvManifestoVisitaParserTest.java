package br.com.cloudport.servicorail.ferrovia.importacao;

import br.com.cloudport.servicorail.comum.sanitizacao.SanitizadorEntrada;
import br.com.cloudport.servicorail.ferrovia.modelo.StatusVisitaTrem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CsvManifestoVisitaParserTest {

    private CsvManifestoVisitaParser parser;

    @BeforeEach
    void preparar() {
        SanitizadorEntrada sanitizadorEntrada = mock(SanitizadorEntrada.class);
        when(sanitizadorEntrada.limparTexto(anyString()))
                .thenAnswer(invocacao -> invocacao.getArgument(0));
        parser = new CsvManifestoVisitaParser(sanitizadorEntrada);
    }

    private byte[] csv(String conteudo) {
        return conteudo.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void parseInterpretaVisitaConteineresEVagoes() {
        String conteudo = String.join("\n",
                "TIPO;COL1;COL2;COL3;COL4;COL5",
                "VISITA;TREM-001;RUMO;2026-05-23 08:00;2026-05-23 12:00;PLANEJADO",
                "CONTEINER_DESCARGA;MSCU1234567;MSCU7654321",
                "CONTEINER_CARGA;TCLU1111111",
                "VAGAO;1;VAG-01;PLATAFORMA",
                "VAGAO;2;VAG-02;PLATAFORMA");

        ResultadoManifestoVisita resultado = parser.parse("manifesto.csv", csv(conteudo));

        assertThat(resultado.getIdentificadorTrem()).isEqualTo("TREM-001");
        assertThat(resultado.getOperadoraFerroviaria()).isEqualTo("RUMO");
        assertThat(resultado.getHoraChegadaPrevista()).isEqualTo(LocalDateTime.of(2026, 5, 23, 8, 0));
        assertThat(resultado.getHoraPartidaPrevista()).isEqualTo(LocalDateTime.of(2026, 5, 23, 12, 0));
        assertThat(resultado.getStatusVisita()).isEqualTo(StatusVisitaTrem.PLANEJADO);
        assertThat(resultado.getIdentificacoesDescarga()).containsExactly("MSCU1234567", "MSCU7654321");
        assertThat(resultado.getIdentificacoesCarga()).containsExactly("TCLU1111111");
        assertThat(resultado.getVagoes()).hasSize(2);
        assertThat(resultado.getVagoes().get(0).getIdentificadorVagao()).isEqualTo("VAG-01");
    }

    @Test
    void parseRejeitaArquivoVazio() {
        assertThatThrownBy(() -> parser.parse("vazio.csv", csv("")))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void parseRejeitaManifestoSemIdentificadorDoTrem() {
        String conteudo = String.join("\n",
                "VISITA;;RUMO;2026-05-23 08:00",
                "CONTEINER_DESCARGA;MSCU1234567");

        assertThatThrownBy(() -> parser.parse("manifesto.csv", csv(conteudo)))
                .isInstanceOf(ResponseStatusException.class);
    }
}
