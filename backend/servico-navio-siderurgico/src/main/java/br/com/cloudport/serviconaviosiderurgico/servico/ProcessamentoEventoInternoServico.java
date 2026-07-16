package br.com.cloudport.serviconaviosiderurgico.servico;

import br.com.cloudport.serviconaviosiderurgico.dominio.EventoInternoProcessado;
import br.com.cloudport.serviconaviosiderurgico.repositorio.EventoInternoProcessadoRepositorio;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ProcessamentoEventoInternoServico {

    private final EventoInternoProcessadoRepositorio repositorio;

    public ProcessamentoEventoInternoServico(EventoInternoProcessadoRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean processarUmaVez(UUID eventoId, String tipoEvento, Runnable efeito) {
        if (eventoId == null) {
            throw new IllegalArgumentException("O identificador do evento interno e obrigatorio.");
        }
        if (!StringUtils.hasText(tipoEvento)) {
            throw new IllegalArgumentException("O tipo do evento interno e obrigatorio.");
        }
        if (efeito == null) {
            throw new IllegalArgumentException("O efeito do evento interno e obrigatorio.");
        }

        String chave = eventoId.toString();
        if (repositorio.existsById(chave)) {
            return false;
        }

        EventoInternoProcessado processado = new EventoInternoProcessado();
        processado.setEventoId(chave);
        processado.setTipoEvento(tipoEvento.trim());
        repositorio.saveAndFlush(processado);
        efeito.run();
        return true;
    }
}
