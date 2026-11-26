package br.ufs.dcomp.collina.controller.espacos;

import br.ufs.dcomp.collina.dto.espacos.BlocoRequestDTO;
import br.ufs.dcomp.collina.dto.espacos.BlocoResponseDTO;
import br.ufs.dcomp.collina.dto.espacos.DisponibilidadeResponseDTO;
import br.ufs.dcomp.collina.dto.espacos.EspacoRequestDTO;
import br.ufs.dcomp.collina.dto.espacos.EspacoResponseDTO;
import br.ufs.dcomp.collina.dto.espacos.EstadoEspacoRequestDTO;
import br.ufs.dcomp.collina.model.auth.UsuarioAutenticado;
import br.ufs.dcomp.collina.service.espacos.EspacoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

// Permissões por perfil: ver SecurityConfig (/api/espacos/**).
@RestController
@RequestMapping("/api/espacos")
public class EspacoController {

    @Autowired
    private EspacoService espacoService;

    // 1. Listar espaços (associado vê só os reserváveis)
    @GetMapping
    public ResponseEntity<List<EspacoResponseDTO>> listar(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ResponseEntity.ok(espacoService.listar(usuario.perfil()));
    }

    // 2. Buscar espaço por ID
    @GetMapping("/{id}")
    public ResponseEntity<EspacoResponseDTO> buscarPorId(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                                         @PathVariable Integer id) {
        return ResponseEntity.ok(espacoService.buscarPorId(usuario.perfil(), id));
    }

    // 3. Cadastrar espaço (201 Created) - RECEPCAO / ADMIN
    @PostMapping
    public ResponseEntity<EspacoResponseDTO> cadastrar(@Valid @RequestBody EspacoRequestDTO dto) {
        EspacoResponseDTO novo = espacoService.cadastrar(dto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(novo.id())
                .toUri();
        return ResponseEntity.created(location).body(novo);
    }

    // 4. Atualizar espaço - RECEPCAO / ADMIN
    @PutMapping("/{id}")
    public ResponseEntity<EspacoResponseDTO> atualizar(@PathVariable Integer id,
                                                       @Valid @RequestBody EspacoRequestDTO dto) {
        return ResponseEntity.ok(espacoService.atualizar(id, dto));
    }

    // 5. Alterar estado (ATIVO / EM_MANUTENCAO / INATIVO) - RECEPCAO / ADMIN
    @PatchMapping("/{id}/estado")
    public ResponseEntity<EspacoResponseDTO> alterarEstado(@PathVariable Integer id,
                                                           @Valid @RequestBody EstadoEspacoRequestDTO dto) {
        return ResponseEntity.ok(espacoService.alterarEstado(id, dto.estado()));
    }

    // 5b. Remover espaço (204 No Content) - RECEPCAO / ADMIN
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable Integer id) {
        espacoService.remover(id);
        return ResponseEntity.noContent().build();
    }

    // 6. Listar blocos de horário do espaço
    @GetMapping("/{id}/blocos")
    public ResponseEntity<List<BlocoResponseDTO>> listarBlocos(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                                               @PathVariable Integer id) {
        return ResponseEntity.ok(espacoService.listarBlocos(usuario.perfil(), id));
    }

    // 7. Configurar bloco de 2h (201 Created) - somente ADMIN
    @PostMapping("/{id}/blocos")
    public ResponseEntity<BlocoResponseDTO> adicionarBloco(@PathVariable Integer id,
                                                           @Valid @RequestBody BlocoRequestDTO dto) {
        BlocoResponseDTO novo = espacoService.adicionarBloco(id, dto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{blocoId}")
                .buildAndExpand(novo.id())
                .toUri();
        return ResponseEntity.created(location).body(novo);
    }

    // 7b. Remover bloco de 2h (204 No Content) - somente ADMIN
    @DeleteMapping("/{id}/blocos/{blocoId}")
    public ResponseEntity<Void> removerBloco(@PathVariable Integer id,
                                             @PathVariable Integer blocoId) {
        espacoService.removerBloco(id, blocoId);
        return ResponseEntity.noContent().build();
    }

    // 8. Disponibilidade do dia: ?data=AAAA-MM-DD
    @GetMapping("/{id}/disponibilidade")
    public ResponseEntity<DisponibilidadeResponseDTO> disponibilidade(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Integer id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return ResponseEntity.ok(espacoService.consultarDisponibilidade(usuario.perfil(), id, data));
    }
}
