package br.com.cloudport.servicoyard.container.controlador;

import br.com.cloudport.servicoyard.container.dto.AtualizacaoConteinerDTO;
import br.com.cloudport.servicoyard.container.dto.ConteinerDetalheDTO;
import br.com.cloudport.servicoyard.container.dto.ConteinerResumoDTO;
import br.com.cloudport.servicoyard.container.dto.HistoricoOperacaoDTO;
import br.com.cloudport.servicoyard.container.dto.RegistroAlocacaoDTO;
import br.com.cloudport.servicoyard.container.dto.RegistroInspecaoDTO;
import br.com.cloudport.servicoyard.container.dto.RegistroLiberacaoDTO;
import br.com.cloudport.servicoyard.container.dto.RegistroTransferenciaDTO;
import br.com.cloudport.servicoyard.container.enumeracao.TipoMovimentacaoFerroviaEnum;
import br.com.cloudport.servicoyard.container.servico.ConteinerServico;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

@RestController
@RequestMapping("/yard/conteineres")
@Validated
@Tag(name = "Contêineres do Pátio", description = "Operações de gestão de contêineres no pátio")
public class ConteinerControlador {
    private final ConteinerServico conteinerServico;

    public ConteinerControlador(ConteinerServico conteinerServico) {
        this.conteinerServico = conteinerServico;
    }

    @GetMapping
    @Operation(summary = "Listar contêineres",
            description = "Retorna os contêineres do pátio com dados resumidos para exibição em listas")
    public List<ConteinerResumoDTO> listar() {
        return conteinerServico.listarResumo();
    }

    @GetMapping("/{identificador}")
    @Operation(summary = "Detalhar contêiner por identificador interno",
            description = "Busca os dados completos do contêiner informado pelo identificador interno")
    public ConteinerDetalheDTO detalhar(@PathVariable Long identificador) {
        return conteinerServico.buscarDetalhe(identificador);
    }

    @GetMapping("/por-codigo")
    @Operation(summary = "Detalhar contêiner por código",
            description = "Busca os dados completos do contêiner por seu código logístico")
    public ConteinerDetalheDTO detalharPorCodigo(@RequestParam("codigo") String codigo) {
        return conteinerServico.buscarDetalhePorCodigo(codigo);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar alocação de contêiner",
            description = "Cria um novo registro de contêiner no pátio com dados operacionais e cadastrais")
    public ConteinerDetalheDTO registrar(@Valid @RequestBody RegistroAlocacaoDTO dto) {
        return conteinerServico.registrarAlocacao(dto);
    }

    @PutMapping("/{identificador}")
    @Operation(summary = "Atualizar cadastro de contêiner",
            description = "Atualiza os dados cadastrais e operacionais básicos de um contêiner existente")
    public ConteinerDetalheDTO atualizar(@PathVariable Long identificador,
                                         @Valid @RequestBody AtualizacaoConteinerDTO dto) {
        return conteinerServico.atualizarCadastro(identificador, dto);
    }

    @PostMapping("/{identificador}/transferencias")
    @Operation(summary = "Registrar transferência de contêiner",
            description = "Registra uma movimentação de transferência de contêiner dentro do pátio")
    public ConteinerDetalheDTO transferir(@PathVariable Long identificador,
                                          @Valid @RequestBody RegistroTransferenciaDTO dto) {
        return conteinerServico.registrarTransferencia(identificador, dto);
    }

    @PostMapping("/{identificador}/inspecoes")
    @Operation(summary = "Registrar inspeção em contêiner",
            description = "Anexa os resultados de uma inspeção operacional ao histórico do contêiner")
    public ConteinerDetalheDTO inspecionar(@PathVariable Long identificador,
                                           @Valid @RequestBody RegistroInspecaoDTO dto) {
        return conteinerServico.registrarInspecao(identificador, dto);
    }

    @PostMapping("/{identificador}/liberacoes")
    @Operation(summary = "Registrar liberação de contêiner",
            description = "Marca o contêiner como liberado para saída e adiciona o evento ao histórico")
    public ConteinerDetalheDTO liberar(@PathVariable Long identificador,
                                       @Valid @RequestBody RegistroLiberacaoDTO dto) {
        return conteinerServico.registrarLiberacao(identificador, dto);
    }

    @GetMapping("/{identificador}/historico")
    @Operation(summary = "Consultar histórico por identificador",
            description = "Lista as operações registradas para o contêiner utilizando o identificador interno")
    public List<HistoricoOperacaoDTO> historico(@PathVariable Long identificador) {
        return conteinerServico.consultarHistorico(identificador);
    }

    @GetMapping("/por-codigo/historico")
    @Operation(summary = "Consultar histórico por código",
            description = "Lista as operações registradas para o contêiner utilizando o código logístico")
    public List<HistoricoOperacaoDTO> historicoPorCodigo(@RequestParam("codigo") String codigo) {
        return conteinerServico.consultarHistoricoPorCodigo(codigo);
    }

    @GetMapping("/ferrovia/tipos-movimentacao")
    @Operation(summary = "Listar tipos de movimentação ferroviária",
            description = "Retorna os tipos de movimentação ferroviária aceitos para integração com o módulo ferroviário")
    public ResponseEntity<List<Map<String, String>>> listarTiposMovimentacaoFerrovia() {
        List<Map<String, String>> opcoes = new ArrayList<>();
        for (TipoMovimentacaoFerroviaEnum tipo : TipoMovimentacaoFerroviaEnum.values()) {
            Map<String, String> registro = Map.of(
                    "value", tipo.name(),
                    "label", HtmlUtils.htmlEscape(tipo.getDescricao(), StandardCharsets.UTF_8.name())
            );
            opcoes.add(registro);
        }
        return ResponseEntity.ok(opcoes);
    }
}
