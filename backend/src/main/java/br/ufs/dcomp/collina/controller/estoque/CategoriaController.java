package br.ufs.dcomp.collina.controller.estoque;

import br.ufs.dcomp.collina.dto.estoque.CategoriaRequestDTO;
import br.ufs.dcomp.collina.dto.estoque.CategoriaResponseDTO;
import br.ufs.dcomp.collina.service.estoque.CategoriaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

    @Autowired
    private CategoriaService categoriaService;

    // 1. Criar nova categoria (201 Created)
    @PostMapping
    public ResponseEntity<CategoriaResponseDTO> criar(@Valid @RequestBody CategoriaRequestDTO dto) {
        CategoriaResponseDTO novaCategoria = categoriaService.adicionarCategoria(dto);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(novaCategoria.id())
                .toUri();

        return ResponseEntity.created(location).body(novaCategoria);
    }

    // 2. Listar todas as categorias (200 OK)
    @GetMapping
    public ResponseEntity<List<CategoriaResponseDTO>> listarTodas() {
        List<CategoriaResponseDTO> categorias = categoriaService.listarTodas();
        return ResponseEntity.ok(categorias);
    }

    // 3. Buscar categoria por ID (200 OK)
    @GetMapping("/{id}")
    public ResponseEntity<CategoriaResponseDTO> buscarPorId(@PathVariable Integer id) {
        CategoriaResponseDTO categoria = categoriaService.buscarPorId(id);
        return ResponseEntity.ok(categoria);
    }

    // 4. Editar / Atualizar categoria (200 OK)
    @PutMapping("/{id}")
    public ResponseEntity<CategoriaResponseDTO> atualizar(@PathVariable Integer id, @Valid @RequestBody CategoriaRequestDTO dto) {
        CategoriaResponseDTO categoriaAtualizada = categoriaService.editarCategoria(id, dto);
        return ResponseEntity.ok(categoriaAtualizada);
    }

    // 5. Remover categoria (204 No Content)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable Integer id) {
        categoriaService.removerCategoria(id);
        return ResponseEntity.noContent().build();
    }
}
