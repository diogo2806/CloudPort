package br.com.cloudport.visibilidade.controller;

import br.com.cloudport.visibilidade.dto.AcaoAlertaRequest;
import br.com.cloudport.visibilidade.dto.AlertaResumoDTO;
import br.com.cloudport.visibilidade.entity.Alerta;
import br.com.cloudport.visibilidade.service.AlertasService;
import java.security.Principal;
import java.util.List;
import javax.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/visibilidade/alertas")
public class AlertasController {

    private final AlertasService alertasService;

    public AlertasController(AlertasService alertasService) {
        this.alertasService = alertasService;
    }

    @GetMapping("/filtrados")
    public Page<Alerta> buscarFiltrados(
            @RequestParam(required = false) List<String> severidade,
            @RequestParam(required = false) List<String> tipo,
            @RequestParam(defaultValue = "ativo") String status,
            @PageableDefault(size = 50, sort = "dataGerada", direction = Sort.Direction.DESC) Pageable pageable) {
        return alertasService.buscarAlertasFiltrados(severidade, tipo, status, pageable);
    }

    @GetMapping("/resumo")
    public AlertaResumoDTO obterResumo() {
        return alertasService.obterResumoAtivos();
    }

    @PatchMapping("/{id}/reconhecer")
    public Alerta reconhecer(@PathVariable Long id,
                             @Valid @RequestBody(required = false) AcaoAlertaRequest request,
                             Principal principal) {
        return alertasService.reconhecerAlerta(id, resolverUsuario(request, principal));
    }

    @PatchMapping("/{id}/resolver")
    public Alerta resolver(@PathVariable Long id,
                           @Valid @RequestBody(required = false) AcaoAlertaRequest request,
                           Principal principal) {
        return alertasService.resolverAlerta(id, resolverUsuario(request, principal));
    }

    private String resolverUsuario(AcaoAlertaRequest request, Principal principal) {
        if (principal != null && principal.getName() != null && !principal.getName().isBlank()) {
            return principal.getName();
        }
        return request != null ? request.getUsuario() : null;
    }
}
