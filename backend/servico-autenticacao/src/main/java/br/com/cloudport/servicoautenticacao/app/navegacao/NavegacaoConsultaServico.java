package br.com.cloudport.servicoautenticacao.app.navegacao;

import br.com.cloudport.servicoautenticacao.app.navegacao.dto.AbaNavegacaoDTO;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NavegacaoConsultaServico {

    private final ConfiguracaoNavegacaoRepositorio configuracaoRepositorio;
    private final SanitizadorConteudoNavegacao sanitizadorConteudoNavegacao;

    public NavegacaoConsultaServico(ConfiguracaoNavegacaoRepositorio configuracaoRepositorio,
                                    SanitizadorConteudoNavegacao sanitizadorConteudoNavegacao) {
        this.configuracaoRepositorio = configuracaoRepositorio;
        this.sanitizadorConteudoNavegacao = sanitizadorConteudoNavegacao;
    }

    @Transactional(readOnly = true)
    public List<AbaNavegacaoDTO> listarAbas() {
        return configuracaoRepositorio.findAllByOrderByOrdemAsc()
                .stream()
                .map(this::converterParaDto)
                .collect(Collectors.toList());
    }

    private AbaNavegacaoDTO converterParaDto(ConfiguracaoNavegacao configuracao) {
        String identificadorSanitizado = sanitizadorConteudoNavegacao.sanitizarIdentificador(configuracao.getIdentificador());
        String rotuloSanitizado = sanitizadorConteudoNavegacao.sanitizarRotulo(configuracao.getRotulo());
        List<String> rotaSanitizada = sanitizadorConteudoNavegacao.sanitizarSegmentosRota(configuracao.getRota());
        String grupoSanitizado = sanitizadorConteudoNavegacao.sanitizarGrupo(configuracao.getGrupo());
        List<String> papeisSanitizados = sanitizadorConteudoNavegacao.sanitizarListaPapeis(configuracao.getRolesPermitidos());
        String mensagemSanitizada = sanitizadorConteudoNavegacao.sanitizarMensagem(configuracao.getMensagemEmBreve());

        return new AbaNavegacaoDTO(
                configuracao.getId(),
                identificadorSanitizado,
                rotuloSanitizado.isEmpty() ? identificadorSanitizado : rotuloSanitizado,
                rotaSanitizada,
                configuracao.isDesabilitado(),
                mensagemSanitizada.isEmpty() ? null : mensagemSanitizada,
                grupoSanitizado,
                papeisSanitizados,
                configuracao.isPadrao()
        );
    }
}
