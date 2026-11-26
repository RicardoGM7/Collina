package br.ufs.dcomp.collina.controller.funcionarios;

import br.ufs.dcomp.collina.dto.funcionarios.TurnoRequestDTO;
import br.ufs.dcomp.collina.dto.funcionarios.TurnoResponseDTO;
import br.ufs.dcomp.collina.service.funcionarios.EscalaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/turnos")
public class TurnoController {

    @Autowired
    private EscalaService escalaService;

    @PostMapping
    public ResponseEntity<TurnoResponseDTO> criarTurno(@Valid @RequestBody TurnoRequestDTO dto) {
        TurnoResponseDTO criado = escalaService.criarTurno(dto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criado.id())
                .toUri();
        return ResponseEntity.created(location).body(criado);
    }

    @GetMapping
    public ResponseEntity<List<TurnoResponseDTO>> listarTurnos() {
        return ResponseEntity.ok(escalaService.listarTurnos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TurnoResponseDTO> buscarPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(escalaService.buscarTurnoPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TurnoResponseDTO> editarTurno(@PathVariable Integer id,
                                                        @Valid @RequestBody TurnoRequestDTO dto) {
        return ResponseEntity.ok(escalaService.editarTurno(id, dto));
    }
}
