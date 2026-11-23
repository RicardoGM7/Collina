package br.ufs.dcomp.collina.controller.associados;

import br.ufs.dcomp.collina.dto.associados.PlanoRequestDTO;
import br.ufs.dcomp.collina.dto.associados.PlanoResponseDTO;
import br.ufs.dcomp.collina.service.associados.PlanoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/planos")
public class PlanoController {

    @Autowired
    private PlanoService planoService;

    // 1. Listar planos (200 OK)
    @GetMapping
    public ResponseEntity<List<PlanoResponseDTO>> listarTodos() {
        return ResponseEntity.ok(planoService.listarTodos());
    }

    // 2. Buscar plano por ID (200 OK)
    @GetMapping("/{id}")
    public ResponseEntity<PlanoResponseDTO> buscarPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(planoService.buscarPorId(id));
    }

    // 3. Atualizar parâmetros do plano (200 OK)
    @PutMapping("/{id}")
    public ResponseEntity<PlanoResponseDTO> atualizar(@PathVariable Integer id, @Valid @RequestBody PlanoRequestDTO dto) {
        return ResponseEntity.ok(planoService.atualizarParametros(id, dto));
    }
}
