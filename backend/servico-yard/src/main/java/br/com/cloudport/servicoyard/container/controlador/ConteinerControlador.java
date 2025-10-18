package br.com.cloudport.servicoyard.container.controlador;

import br.com.cloudport.servicoyard.container.dto.AtualizacaoConteinerDTO;
import br.com.cloudport.servicoyard.container.dto.ConteinerDetalheDTO;
import br.com.cloudport.servicoyard.container.dto.ConteinerResumoDTO;
import br.com.cloudport.servicoyard.container.dto.HistoricoOperacaoDTO;
import br.com.cloudport.servicoyard.container.dto.RegistroAlocacaoDTO;
import br.com.cloudport.servicoyard.container.dto.RegistroInspecaoDTO;
import br.com.cloudport.servicoyard.container.dto.RegistroLiberacaoDTO;
import br.com.cloudport.servicoyard.container.dto.RegistroTransferenciaDTO;
import br.com.cloudport.servicoyard.container.servico.ConteinerServico;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/yard/conteineres")
@Validated
public class ConteinerControlador {
    private final ConteinerServico conteinerServico;

    public ConteinerControlador(ConteinerServico conteinerServico) {
        this.conteinerServico = conteinerServico;
    }

    @GetMapping
    public List<ConteinerResumoDTO> listar() {
        return conteinerServico.listarResumo();
    }

    @GetMapping("/{identificador}")
    public ConteinerDetalheDTO detalhar(@PathVariable Long identificador) {
        return conteinerServico.buscarDetalhe(identificador);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConteinerDetalheDTO registrar(@Valid @RequestBody RegistroAlocacaoDTO dto) {
        return conteinerServico.registrarAlocacao(dto);
    }

    @PutMapping("/{identificador}")
    public ConteinerDetalheDTO atualizar(@PathVariable Long identificador,
                                         @Valid @RequestBody AtualizacaoConteinerDTO dto) {
        return conteinerServico.atualizarCadastro(identificador, dto);
    }

    @PostMapping("/{identificador}/transferencias")
    public ConteinerDetalheDTO transferir(@PathVariable Long identificador,
                                          @Valid @RequestBody RegistroTransferenciaDTO dto) {
        return conteinerServico.registrarTransferencia(identificador, dto);
    }

    @PostMapping("/{identificador}/inspecoes")
    public ConteinerDetalheDTO inspecionar(@PathVariable Long identificador,
                                           @Valid @RequestBody RegistroInspecaoDTO dto) {
        return conteinerServico.registrarInspecao(identificador, dto);
    }

    @PostMapping("/{identificador}/liberacoes")
    public ConteinerDetalheDTO liberar(@PathVariable Long identificador,
                                       @Valid @RequestBody RegistroLiberacaoDTO dto) {
        return conteinerServico.registrarLiberacao(identificador, dto);
    }

    @GetMapping("/{identificador}/historico")
    public List<HistoricoOperacaoDTO> historico(@PathVariable Long identificador) {
        return conteinerServico.consultarHistorico(identificador);
    }
}
