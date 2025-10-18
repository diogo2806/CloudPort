package br.com.cloudport.servicoautenticacao.app.privacidade;

import br.com.cloudport.servicoautenticacao.app.privacidade.dto.OpcaoPrivacidadeRespostaDTO;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class PrivacidadeConsultaServico {

    private final ConfiguracaoPrivacidadeRepositorio configuracaoPrivacidadeRepositorio;
    private final SanitizadorConteudoPrivacidade sanitizadorConteudoPrivacidade;

    public PrivacidadeConsultaServico(
            ConfiguracaoPrivacidadeRepositorio configuracaoPrivacidadeRepositorio,
            SanitizadorConteudoPrivacidade sanitizadorConteudoPrivacidade
    ) {
        this.configuracaoPrivacidadeRepositorio = configuracaoPrivacidadeRepositorio;
        this.sanitizadorConteudoPrivacidade = sanitizadorConteudoPrivacidade;
    }

    public List<OpcaoPrivacidadeRespostaDTO> listarOpcoes() {
        return configuracaoPrivacidadeRepositorio.findAll()
                .stream()
                .map(opcao -> OpcaoPrivacidadeRespostaDTO.fromModelo(
                        opcao,
                        sanitizadorConteudoPrivacidade.sanitizarDescricao(opcao.getDescricao())
                ))
                .collect(Collectors.toList());
    }
}
