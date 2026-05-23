package br.com.cloudport.serviconavio.linha.servico;

import br.com.cloudport.serviconavio.comum.validacao.SanitizadorEntrada;
import br.com.cloudport.serviconavio.linha.dto.CadastroServicoLinhaDTO;
import br.com.cloudport.serviconavio.linha.dto.PortoRotacaoRequest;
import br.com.cloudport.serviconavio.linha.dto.ServicoLinhaDTO;
import br.com.cloudport.serviconavio.linha.entidade.ServicoLinha;
import br.com.cloudport.serviconavio.linha.repositorio.ServicoLinhaRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ServicoLinhaServicoTest {

    private ServicoLinhaRepositorio repositorio;
    private ServicoLinhaServico servico;

    @BeforeEach
    void preparar() {
        repositorio = mock(ServicoLinhaRepositorio.class);
        SanitizadorEntrada sanitizadorEntrada = mock(SanitizadorEntrada.class);
        when(sanitizadorEntrada.limparTextoObrigatorio(anyString(), anyString()))
                .thenAnswer(inv -> inv.getArgument(0));
        when(sanitizadorEntrada.limparTexto(anyString())).thenAnswer(inv -> inv.getArgument(0));
        when(repositorio.save(any(ServicoLinha.class))).thenAnswer(inv -> inv.getArgument(0));
        servico = new ServicoLinhaServico(repositorio, sanitizadorEntrada);
    }

    private CadastroServicoLinhaDTO cadastro() {
        CadastroServicoLinhaDTO dto = new CadastroServicoLinhaDTO();
        dto.setCodigo("asia1");
        dto.setNome("Ásia Express");
        dto.setArmador("ACME Lines");
        PortoRotacao p1 = porto(1, "brssz", "Santos");
        PortoRotacao p2 = porto(2, "cnsha", "Shanghai");
        dto.setRotacao(Arrays.asList(p1.request, p2.request));
        return dto;
    }

    private static class PortoRotacao {
        final PortoRotacaoRequest request;

        PortoRotacao(PortoRotacaoRequest request) {
            this.request = request;
        }
    }

    private PortoRotacao porto(int seq, String unloc, String nome) {
        PortoRotacaoRequest r = new PortoRotacaoRequest();
        r.setSequencia(seq);
        r.setPortoUnloc(unloc);
        r.setNomePorto(nome);
        return new PortoRotacao(r);
    }

    @Test
    void registrarNormalizaCodigoEMapeiaRotacao() {
        when(repositorio.existsByCodigoIgnoreCase("ASIA1")).thenReturn(false);

        ServicoLinhaDTO resultado = servico.registrar(cadastro());

        assertThat(resultado.getCodigo()).isEqualTo("ASIA1");
        assertThat(resultado.getRotacao()).hasSize(2);
        assertThat(resultado.getRotacao().get(0).getPortoUnloc()).isEqualTo("BRSSZ");
    }

    @Test
    void registrarRejeitaCodigoDuplicado() {
        when(repositorio.existsByCodigoIgnoreCase("ASIA1")).thenReturn(true);

        assertThatThrownBy(() -> servico.registrar(cadastro()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
