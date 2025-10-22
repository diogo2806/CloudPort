package br.com.cloudport.servicoautenticacao.app.notificacoes;

import br.com.cloudport.servicoautenticacao.app.notificacoes.dto.CanalNotificacaoDTO;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NotificacoesPreferenciasServico {

    private final CanalNotificacaoRepositorio canalNotificacaoRepositorio;
    private final SanitizadorConteudoNotificacao sanitizadorConteudoNotificacao;

    public NotificacoesPreferenciasServico(
            CanalNotificacaoRepositorio canalNotificacaoRepositorio,
            SanitizadorConteudoNotificacao sanitizadorConteudoNotificacao
    ) {
        this.canalNotificacaoRepositorio = canalNotificacaoRepositorio;
        this.sanitizadorConteudoNotificacao = sanitizadorConteudoNotificacao;
    }

    @Transactional(readOnly = true)
    public List<CanalNotificacaoDTO> listarCanais() {
        return canalNotificacaoRepositorio.findAll(Sort.by(Sort.Order.asc("nomeCanal")))
                .stream()
                .map(this::mapearParaDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public CanalNotificacaoDTO atualizarStatus(Long identificador, boolean habilitado) {
        CanalNotificacao canal = canalNotificacaoRepositorio.findById(identificador)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Canal de notificação não foi localizado."
                ));
        canal.setHabilitado(habilitado);
        CanalNotificacao salvo = canalNotificacaoRepositorio.save(canal);
        return mapearParaDto(salvo);
    }

    private CanalNotificacaoDTO mapearParaDto(CanalNotificacao canal) {
        String nomeSanitizado = sanitizadorConteudoNotificacao.sanitizarNomeCanal(canal.getNomeCanal());
        return new CanalNotificacaoDTO(canal.getIdentificador(), nomeSanitizado, canal.isHabilitado());
    }
}
