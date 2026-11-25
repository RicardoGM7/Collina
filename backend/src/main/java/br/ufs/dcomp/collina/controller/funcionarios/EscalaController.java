package br.ufs.dcomp.collina.controller.funcionarios;

import br.ufs.dcomp.collina.dto.funcionarios.*;
import br.ufs.dcomp.collina.service.funcionarios.EscalaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/escalas")
public class EscalaController {

    @Autowired
    private EscalaService escalaService;

    // ==========================================
    // 1. Endpoints de Escala
    // ==========================================

    @PostMapping
    public ResponseEntity<EscalaResponseDTO> criarEscala(@Valid @RequestBody EscalaRequestDTO dto) {
        EscalaResponseDTO criada = escalaService.criarEscala(dto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criada.id())
                .toUri();
        return ResponseEntity.created(location).body(criada);
    }

    @GetMapping
    public ResponseEntity<List<EscalaResponseDTO>> listarEscalas() {
        return ResponseEntity.ok(escalaService.listarEscalas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EscalaResponseDTO> buscarPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(escalaService.buscarEscalaPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EscalaResponseDTO> editarEscala(@PathVariable Integer id,
                                                          @Valid @RequestBody EscalaRequestDTO dto) {
        return ResponseEntity.ok(escalaService.editarEscala(id, dto));
    }

    @PatchMapping("/{id}/publicar")
    public ResponseEntity<EscalaResponseDTO> publicarEscala(@PathVariable Integer id) {
        return ResponseEntity.ok(escalaService.publicarEscala(id));
    }

    // ==========================================
    // 2. Endpoints de Alocação de Funcionários
    // ==========================================

    @PostMapping("/alocacoes")
    public ResponseEntity<AlocacaoResponseDTO> alocarFuncionario(@Valid @RequestBody AlocacaoRequestDTO dto) {
        AlocacaoResponseDTO alocada = escalaService.alocarFuncionario(dto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(alocada.id())
                .toUri();
        return ResponseEntity.created(location).body(alocada);
    }

    @PostMapping("/alocacoes/validar")
    public ResponseEntity<ValidacaoAlocacaoResponseDTO> validarAlocacao(@Valid @RequestBody AlocacaoRequestDTO dto) {
        return ResponseEntity.ok(escalaService.validarAlocacao(dto));
    }

    @GetMapping("/{id}/alocacoes")
    public ResponseEntity<List<AlocacaoResponseDTO>> listarAlocacoesPorEscala(@PathVariable Integer id) {
        return ResponseEntity.ok(escalaService.listarAlocacoesPorEscala(id));
    }

    @GetMapping("/alocacoes/funcionario/{funcionarioId}")
    public ResponseEntity<List<AlocacaoResponseDTO>> listarAlocacoesPorFuncionario(@PathVariable Integer funcionarioId) {
        return ResponseEntity.ok(escalaService.listarAlocacoesPorFuncionario(funcionarioId));
    }

    @DeleteMapping("/alocacoes/{id}")
    public ResponseEntity<Void> cancelarAlocacao(@PathVariable Integer id) {
        escalaService.cancelarAlocacao(id);
        return ResponseEntity.noContent().build();
    }
}
