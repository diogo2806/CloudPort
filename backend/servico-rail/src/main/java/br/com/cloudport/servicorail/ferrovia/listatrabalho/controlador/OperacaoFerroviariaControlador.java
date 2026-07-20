package br.com.cloudport.servicorail.ferrovia.listatrabalho.controlador;

import br.com.cloudport.servicorail.ferrovia.inspecao.dto.InspecaoVagaoDto.LiberacaoOverride;
import br.com.cloudport.servicorail.ferrovia.inspecao.dto.InspecaoVagaoDto.Registro;
import br.com.cloudport.servicorail.ferrovia.inspecao.dto.InspecaoVagaoDto.Resposta;
import br.com.cloudport.servicorail.ferrovia.inspecao.servico.InspecaoVagaoServico;
import br.com.cloudport.servicorail.ferrovia.manobra.dto.PlanoManobraFerroviariaDto.AlteracaoStatus;
import br.com.cloudport.servicorail.ferrovia.manobra.dto.PlanoManobraFerroviariaDto.Criacao;
import br.com.cloudport.servicorail.ferrovia.manobra.servico.PlanoManobraFerroviariaServico;
import java.security.Principal;
import java.util.List;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rail/ferrovia/lista-trabalho/visitas/{idVisita}")
public class OperacaoFerroviariaControlador {

    private final PlanoManobraFerroviariaServico planoManobraServico;
    private final InspecaoVagaoServico inspecaoVagaoServico;

    public OperacaoFerroviariaControlador(PlanoManobraFerroviariaServico planoManobraServico,
                                          InspecaoVagaoServico inspecaoVagaoServico) {
        this.planoManobraServico = planoManobraServico;
        this.inspecaoVagaoServico = inspecaoVagaoServico;
    }

    @GetMapping("/manobras")
    public List<br.com.cloudport.servicorail.ferrovia.manobra.dto.PlanoManobraFerroviariaDto.Resposta>
    listarManobras(@PathVariable("idVisita") Long idVisita) {
        return planoManobraServico.listar(idVisita);
    }

    @PostMapping("/manobras")
    @ResponseStatus(HttpStatus.CREATED)
    public br.com.cloudport.servicorail.ferrovia.manobra.dto.PlanoManobraFerroviariaDto.Resposta
    criarManobra(@PathVariable("idVisita") Long idVisita,
                 @Valid @RequestBody Criacao dto) {
        return planoManobraServico.criar(idVisita, dto);
    }

    @PatchMapping("/manobras/{idManobra}/status")
    public br.com.cloudport.servicorail.ferrovia.manobra.dto.PlanoManobraFerroviariaDto.Resposta
    alterarStatusManobra(@PathVariable("idVisita") Long idVisita,
                         @PathVariable("idManobra") Long idManobra,
                         @Valid @RequestBody AlteracaoStatus dto,
                         Principal principal) {
        return planoManobraServico.alterarStatus(
                idVisita,
                idManobra,
                dto,
                principal != null ? principal.getName() : null);
    }

    @GetMapping("/inspecoes-vagoes")
    public List<Resposta> listarInspecoes(@PathVariable("idVisita") Long idVisita) {
        return inspecaoVagaoServico.listar(idVisita);
    }

    @PostMapping("/inspecoes-vagoes")
    @ResponseStatus(HttpStatus.CREATED)
    public Resposta registrarInspecao(@PathVariable("idVisita") Long idVisita,
                                      @Valid @RequestBody Registro dto) {
        return inspecaoVagaoServico.registrar(idVisita, dto);
    }

    @PatchMapping("/inspecoes-vagoes/{idInspecao}/override")
    public Resposta liberarOverride(@PathVariable("idVisita") Long idVisita,
                                    @PathVariable("idInspecao") Long idInspecao,
                                    @Valid @RequestBody LiberacaoOverride dto) {
        return inspecaoVagaoServico.liberarOverride(idVisita, idInspecao, dto);
    }
}
