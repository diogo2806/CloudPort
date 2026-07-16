package br.com.cloudport.servicoyard.patio.listatrabalho.servico;

import br.com.cloudport.servicoyard.patio.listatrabalho.dto.ComandoMotivadoDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.HistoricoOperacaoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.repositorio.HistoricoOperacaoPatioRepositorio;
import java.time.LocalDateTime;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AuditoriaComandoPatioServico {

    private final HistoricoOperacaoPatioRepositorio historicoRepositorio;

    public AuditoriaComandoPatioServico(HistoricoOperacaoPatioRepositorio historicoRepositorio) {
        this.historicoRepositorio = historicoRepositorio;
    }

    @Transactional
    public void registrar(Long workQueueId,
                          Long ordemId,
                          String acao,
                          ComandoMotivadoDto comando,
                          String detalhes) {
        HistoricoOperacaoPatio historico = new HistoricoOperacaoPatio();
        historico.setWorkQueueId(workQueueId);
        historico.setOrdemTrabalhoPatioId(ordemId);
        historico.setAcao(acao);
        historico.setUsuario(usuarioEfetivo(comando));
        historico.setMotivo(limitar(comando.getMotivo().trim(), 500));
        historico.setDetalhes(limitar(detalhes, 2000));
        historico.setCriadoEm(LocalDateTime.now());
        historicoRepositorio.save(historico);
    }

    private String usuarioEfetivo(ComandoMotivadoDto comando) {
        if (StringUtils.hasText(comando.getUsuario())) {
            return comando.getUsuario().trim();
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && StringUtils.hasText(authentication.getName())
                ? authentication.getName()
                : "sistema";
    }

    private String limitar(String valor, int limite) {
        if (valor == null || valor.length() <= limite) {
            return valor;
        }
        return valor.substring(0, limite);
    }
}
