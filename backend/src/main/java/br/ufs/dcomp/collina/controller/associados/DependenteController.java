package br.ufs.dcomp.collina.controller.associados;

import br.ufs.dcomp.collina.dto.associados.DependenteRequestDTO;
import br.ufs.dcomp.collina.dto.associados.DependenteUpdateDTO;
import br.ufs.dcomp.collina.dto.associados.VinculoDependenciaResponseDTO;
import br.ufs.dcomp.collina.model.associados.StatusVinculoEnum;
import br.ufs.dcomp.collina.service.associados.VinculoDependenciaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class DependenteController {

    @Autowired
    private VinculoDependenciaService vinculoService;

    // 1. RF-AS03: cadastrar dependente de um titular (201 Created)
    @PostMapping("/associados/{titularId}/dependentes")
    public ResponseEntity<VinculoDependenciaResponseDTO> cadastrar(@PathVariable Integer titularId,
                                                                   @Valid @RequestBody DependenteRequestDTO dto) {
        VinculoDependenciaResponseDTO novo = vinculoService.cadastrarDependente(titularId, dto);

        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/dependentes/{id}")
                .buildAndExpand(novo.id())
                .toUri();

        return ResponseEntity.created(location).body(novo);
    }

    // 2. Listar dependentes de um titular, com filtro opcional ?status=ATIVO|INATIVO (200 OK)
    @GetMapping("/associados/{titularId}/dependentes")
    public ResponseEntity<List<VinculoDependenciaResponseDTO>> listarPorTitular(
            @PathVariable Integer titularId,
            @RequestParam(required = false) StatusVinculoEnum status) {
        return ResponseEntity.ok(vinculoService.listarPorTitular(titularId, status));
    }

    // 3. Buscar vínculo por ID (200 OK)
    @GetMapping("/dependentes/{vinculoId}")
    public ResponseEntity<VinculoDependenciaResponseDTO> buscarPorId(@PathVariable Integer vinculoId) {
        return ResponseEntity.ok(vinculoService.buscarPorId(vinculoId));
    }

    // 4. Atualizar dados do dependente (200 OK)
    @PutMapping("/dependentes/{vinculoId}")
    public ResponseEntity<VinculoDependenciaResponseDTO> atualizar(@PathVariable Integer vinculoId,
                                                                   @Valid @RequestBody DependenteUpdateDTO dto) {
        return ResponseEntity.ok(vinculoService.atualizarDependente(vinculoId, dto));
    }

    // 5. Inativar vínculo por solicitação (200 OK)
    @PatchMapping("/dependentes/{vinculoId}/inativar")
    public ResponseEntity<VinculoDependenciaResponseDTO> inativar(@PathVariable Integer vinculoId) {
        return ResponseEntity.ok(vinculoService.inativarVinculo(vinculoId));
    }

    // 6. RF-AS04: dispara a inativação dos vínculos de quem já completou 21 anos (200 OK)
    @PostMapping("/dependentes/inativar-por-idade")
    public ResponseEntity<Map<String, Integer>> inativarPorIdade() {
        return ResponseEntity.ok(Map.of("vinculosInativados", vinculoService.inativarVinculosVencidosPorIdade()));
    }
}
