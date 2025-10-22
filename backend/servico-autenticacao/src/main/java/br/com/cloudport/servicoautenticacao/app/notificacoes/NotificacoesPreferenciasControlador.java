package br.com.cloudport.servicoautenticacao.app.notificacoes;

import br.com.cloudport.servicoautenticacao.app.notificacoes.dto.AtualizacaoStatusCanalDTO;
import br.com.cloudport.servicoautenticacao.app.notificacoes.dto.CanalNotificacaoDTO;
import java.util.List;
import javax.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/configuracoes/notificacoes")
public class NotificacoesPreferenciasControlador {

    private final NotificacoesPreferenciasServico notificacoesPreferenciasServico;

    public NotificacoesPreferenciasControlador(NotificacoesPreferenciasServico notificacoesPreferenciasServico) {
        this.notificacoesPreferenciasServico = notificacoesPreferenciasServico;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR','OPERADOR_GATE')")
    public ResponseEntity<List<CanalNotificacaoDTO>> listarCanais() {
        return ResponseEntity.ok(notificacoesPreferenciasServico.listarCanais());
    }

    @PatchMapping("/{identificador}")
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR','OPERADOR_GATE')")
    public ResponseEntity<CanalNotificacaoDTO> atualizarStatus(
            @PathVariable("identificador") Long identificador,
            @Valid @RequestBody AtualizacaoStatusCanalDTO atualizacaoStatusCanalDTO
    ) {
        CanalNotificacaoDTO atualizado = notificacoesPreferenciasServico
                .atualizarStatus(identificador, Boolean.TRUE.equals(atualizacaoStatusCanalDTO.getHabilitado()));
        return ResponseEntity.ok(atualizado);
    }
}
