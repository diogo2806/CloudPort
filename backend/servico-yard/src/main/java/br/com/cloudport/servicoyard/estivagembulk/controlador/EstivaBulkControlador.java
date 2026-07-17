package br.com.cloudport.servicoyard.estivagembulk.controlador;

import br.com.cloudport.servicoyard.estivagembulk.dto.AnaliseEmpilhamentoDto;
import br.com.cloudport.servicoyard.estivagembulk.dto.EstabilidadeEstrutural;
import br.com.cloudport.servicoyard.estivagembulk.dto.NavioGranelDto;
import br.com.cloudport.servicoyard.estivagembulk.dto.PlanoEstivaBulkDto;
import br.com.cloudport.servicoyard.estivagembulk.dto.PosicaoBobinaDto;
import br.com.cloudport.servicoyard.estivagembulk.dto.PosicionarBobinaRequisicaoDto;
import br.com.cloudport.servicoyard.estivagembulk.dto.PressaoTanktopDto;
import br.com.cloudport.servicoyard.estivagembulk.dto.TacktopDto;
import br.com.cloudport.servicoyard.estivagembulk.modelo.BobinaManifesto;
import br.com.cloudport.servicoyard.estivagembulk.modelo.NavioGranel;
import br.com.cloudport.servicoyard.estivagembulk.modelo.PlanoEstivaBulk;
import br.com.cloudport.servicoyard.estivagembulk.servico.PlanoEstivaBulkServico;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/estivagem-bulk")
public class EstivaBulkControlador {

    private final PlanoEstivaBulkServico servico;

    public EstivaBulkControlador(PlanoEstivaBulkServico servico) {
        this.servico = servico;
    }

    @PostMapping("/navios")
    public ResponseEntity<NavioGranel> registrarNavio(@RequestBody NavioGranelDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servico.registrarNavio(dto));
    }

    @GetMapping("/navios")
    public ResponseEntity<List<NavioGranelDto>> listarNavios() {
        return ResponseEntity.ok(servico.listarNavios(false));
    }

    @GetMapping("/navios/templates")
    public ResponseEntity<List<NavioGranelDto>> listarTemplates() {
        return ResponseEntity.ok(servico.listarNavios(true));
    }

    @PostMapping("/planos")
    public ResponseEntity<?> criarPlano(@RequestBody Map<String, Object> body) {
        try {
            Long navioId = Long.valueOf(body.get("navioId").toString());
            String codigoViagem = (String) body.get("codigoViagem");
            String portoCarga = (String) body.get("portoCarga");
            String portoDescarga = (String) body.get("portoDescarga");
            PlanoEstivaBulk plano = servico.criarPlano(navioId, codigoViagem, portoCarga, portoDescarga);
            return ResponseEntity.status(HttpStatus.CREATED).body(servico.buscarPorId(plano.getId()));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/planos")
    public ResponseEntity<List<PlanoEstivaBulkDto>> listarPlanos(
            @RequestParam Long navioId,
            @RequestParam(required = false) String codigoViagem) {
        return ResponseEntity.ok(servico.listarPlanos(navioId, codigoViagem));
    }

    @GetMapping("/planos/{id}")
    public ResponseEntity<?> buscarPlano(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(servico.buscarPorId(id));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/planos/{id}/bobinas")
    public ResponseEntity<?> adicionarBobina(
            @PathVariable Long id, @RequestBody BobinaManifesto bobina) {
        try {
            servico.adicionarBobina(id, bobina);
            return ResponseEntity.status(HttpStatus.CREATED).body(servico.buscarPorId(id));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/planos/{id}/posicionar")
    public ResponseEntity<?> posicionarBobina(
            @PathVariable Long id, @RequestBody PosicionarBobinaRequisicaoDto requisicao) {
        try {
            PosicaoBobinaDto dto = servico.posicionarBobina(id, requisicao);
            return ResponseEntity.ok(dto);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("erro", e.getMessage(), "tipo", "HARD_CONSTRAINT"));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/planos/{id}/tanktop")
    public ResponseEntity<List<PressaoTanktopDto>> analisarTanktop(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(servico.analisarTanktop(id));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/planos/{id}/empilhamento/{poraoId}")
    public ResponseEntity<AnaliseEmpilhamentoDto> analisarEmpilhamento(
            @PathVariable Long id, @PathVariable Long poraoId) {
        try {
            return ResponseEntity.ok(servico.analisarEmpilhamento(id, poraoId));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/planos/{id}/estabilidade")
    public ResponseEntity<EstabilidadeEstrutural> calcularEstabilidade(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(servico.calcularEstabilidade(id));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/planos/{id}/tacktop")
    public ResponseEntity<TacktopDto> calcularTacktop(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(servico.calcularTacktop(id));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/planos/{id}/validar")
    public ResponseEntity<?> validarEAprovar(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(servico.validarEAprovar(id));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("erro", e.getMessage(), "tipo", "HARD_CONSTRAINT"));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/planos/{id}/relatorio")
    public ResponseEntity<?> relatorio(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(servico.buscarPorId(id));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
