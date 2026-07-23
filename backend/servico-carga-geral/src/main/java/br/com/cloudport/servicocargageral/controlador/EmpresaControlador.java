package br.com.cloudport.servicocargageral.controlador;

import br.com.cloudport.servicocargageral.dominio.Empresa;
import br.com.cloudport.servicocargageral.dominio.Empresa.PapelEmpresa;
import br.com.cloudport.servicocargageral.repositorio.EmpresaRepositorio;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/carga-geral/empresas")
public class EmpresaControlador {
    private final EmpresaRepositorio repositorio;
    public EmpresaControlador(EmpresaRepositorio repositorio) { this.repositorio = repositorio; }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR','OPERADOR_GATE')")
    public List<Resposta> listar(@RequestParam(required=false) String busca,
                                @RequestParam(required=false) PapelEmpresa papel,
                                @RequestParam(required=false) Boolean ativo) {
        String termo = busca == null ? "" : busca.trim().toLowerCase();
        return repositorio.findAllByOrderByRazaoSocialAsc().stream()
            .filter(e -> ativo == null || e.isAtivo() == ativo)
            .filter(e -> papel == null || e.getPapeis().contains(papel))
            .filter(e -> termo.isEmpty() || (e.getRazaoSocial()+" "+e.getNomeFantasia()+" "+e.getDocumento()+" "+e.getCodigo()).toLowerCase().contains(termo))
            .map(Resposta::de).collect(Collectors.toList());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN_PORTO')")
    public ResponseEntity<Resposta> criar(@Valid @RequestBody Requisicao r) {
        Empresa e = new Empresa(); aplicar(e, r); validarUnicidade(e, null); e = repositorio.save(e);
        return ResponseEntity.created(URI.create("/api/carga-geral/empresas/"+e.getId())).body(Resposta.de(e));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_PORTO')")
    public Resposta atualizar(@PathVariable UUID id, @Valid @RequestBody Requisicao r) {
        Empresa e = obter(id); aplicar(e, r); validarUnicidade(e, id); return Resposta.de(repositorio.save(e));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN_PORTO')")
    public Resposta status(@PathVariable UUID id, @RequestParam boolean ativo) {
        Empresa e = obter(id); e.setAtivo(ativo); return Resposta.de(repositorio.save(e));
    }

    private Empresa obter(UUID id) { return repositorio.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa não encontrada.")); }
    private void validarUnicidade(Empresa e, UUID id) {
        String doc = e.getDocumento().replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        boolean codigo = id == null ? repositorio.existsByCodigoIgnoreCase(e.getCodigo()) : repositorio.existsByCodigoIgnoreCaseAndIdNot(e.getCodigo(), id);
        boolean documento = id == null ? repositorio.existsByDocumentoNormalizado(doc) : repositorio.existsByDocumentoNormalizadoAndIdNot(doc, id);
        if (codigo) throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe empresa com este código.");
        if (documento) throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe empresa com este CNPJ/documento.");
    }
    private void aplicar(Empresa e, Requisicao r) {
        e.setCodigo(r.codigo()); e.setRazaoSocial(r.razaoSocial()); e.setNomeFantasia(r.nomeFantasia()); e.setDocumento(r.documento());
        e.setInscricaoEstadual(r.inscricaoEstadual()); e.setEndereco(r.endereco()); e.setContato(r.contato()); e.setEmail(r.email());
        e.setTelefone(r.telefone()); e.setPais(r.pais()); e.setObservacoes(r.observacoes()); e.setPapeis(r.papeis());
    }

    public record Requisicao(@NotBlank @Size(max=40) String codigo, @NotBlank @Size(max=180) String razaoSocial,
        @Size(max=180) String nomeFantasia, @NotBlank @Size(max=40) String documento, @Size(max=40) String inscricaoEstadual,
        @Size(max=500) String endereco, @Size(max=180) String contato, @Email @Size(max=180) String email,
        @Size(max=40) String telefone, @NotBlank @Size(max=80) String pais, @Size(max=1000) String observacoes,
        @NotEmpty Set<PapelEmpresa> papeis) {}

    public record Resposta(UUID id, String codigo, String razaoSocial, String nomeFantasia, String documento,
        String inscricaoEstadual, String endereco, String contato, String email, String telefone, String pais,
        boolean ativo, String observacoes, Set<PapelEmpresa> papeis) {
        static Resposta de(Empresa e) { return new Resposta(e.getId(),e.getCodigo(),e.getRazaoSocial(),e.getNomeFantasia(),e.getDocumento(),e.getInscricaoEstadual(),e.getEndereco(),e.getContato(),e.getEmail(),e.getTelefone(),e.getPais(),e.isAtivo(),e.getObservacoes(),e.getPapeis()); }
    }
}