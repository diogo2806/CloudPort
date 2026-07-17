package br.com.cloudport.serviconaviosiderurgico.controlador;

import br.com.cloudport.serviconaviosiderurgico.dominio.VisitaNavio;
import br.com.cloudport.serviconaviosiderurgico.dto.VisitaPlanejamentoDTO;
import br.com.cloudport.serviconaviosiderurgico.servico.VisitaNavioServico;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/visitas-navio")
public class PlanejamentoNavioConsultaControlador {

    private final VisitaNavioServico visitaNavioServico;

    public PlanejamentoNavioConsultaControlador(VisitaNavioServico visitaNavioServico) {
        this.visitaNavioServico = visitaNavioServico;
    }

    @GetMapping("/{identificador}/planejamento")
    public VisitaPlanejamentoDTO detalharParaPlanejamento(@PathVariable Long identificador) {
        VisitaNavio visita = visitaNavioServico.buscarEntidade(identificador);
        if (visita.getNavio().getNavioCadastroId() == null) {
            throw new IllegalStateException(
                    "A visita operacional não está vinculada ao cadastro canônico de navios.");
        }
        return VisitaPlanejamentoDTO.de(visita);
    }
}
