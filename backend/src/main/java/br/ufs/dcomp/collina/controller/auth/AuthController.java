package br.ufs.dcomp.collina.controller.auth;

import br.ufs.dcomp.collina.dto.auth.AlterarPerfilRequestDTO;
import br.ufs.dcomp.collina.dto.auth.AlterarStatusUsuarioRequestDTO;
import br.ufs.dcomp.collina.dto.auth.UsuarioCriarRequestDTO;
import br.ufs.dcomp.collina.dto.auth.UsuarioResponseDTO;
import br.ufs.dcomp.collina.model.auth.UsuarioAutenticado;
import br.ufs.dcomp.collina.service.auth.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

// Regras de acesso definidas no SecurityConfig:
//   /api/auth/usuarios/** -> somente ADMIN;  demais rotas de /api/auth -> qualquer usuário autenticado
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioService usuarioService;

    public AuthController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // 1. Dados do usuário do token atual (não consulta o Firebase)
    @GetMapping("/me")
    public ResponseEntity<UsuarioResponseDTO> me(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ResponseEntity.ok(new UsuarioResponseDTO(
                usuario.uid(), usuario.email(), usuario.nome(), usuario.perfil(), true, usuario.emailVerificado()));
    }

    // 2. Garante o perfil do usuário logado (ASSOCIADO por padrão; ADMIN para e-mails configurados). Chamado pelo front no login.
    @PostMapping("/sincronizar")
    public ResponseEntity<UsuarioResponseDTO> sincronizar(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ResponseEntity.ok(usuarioService.sincronizar(usuario.uid()));
    }

    // 3. ADMIN: listar usuários
    @GetMapping("/usuarios")
    public ResponseEntity<List<UsuarioResponseDTO>> listar() {
        return ResponseEntity.ok(usuarioService.listar());
    }

    // 4. ADMIN: criar usuário com perfil (201 Created)
    @PostMapping("/usuarios")
    public ResponseEntity<UsuarioResponseDTO> criar(@Valid @RequestBody UsuarioCriarRequestDTO dto) {
        UsuarioResponseDTO novo = usuarioService.criar(dto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{uid}")
                .buildAndExpand(novo.uid())
                .toUri();
        return ResponseEntity.created(location).body(novo);
    }

    // 5. ADMIN: alterar perfil de um usuário
    @PutMapping("/usuarios/{uid}/perfil")
    public ResponseEntity<UsuarioResponseDTO> alterarPerfil(@PathVariable String uid,
                                                            @Valid @RequestBody AlterarPerfilRequestDTO dto,
                                                            @AuthenticationPrincipal UsuarioAutenticado solicitante) {
        return ResponseEntity.ok(usuarioService.alterarPerfil(uid, dto.perfil(), solicitante.uid()));
    }

    // 6. ADMIN: ativar/desativar usuário
    @PatchMapping("/usuarios/{uid}/status")
    public ResponseEntity<UsuarioResponseDTO> alterarStatus(@PathVariable String uid,
                                                            @Valid @RequestBody AlterarStatusUsuarioRequestDTO dto,
                                                            @AuthenticationPrincipal UsuarioAutenticado solicitante) {
        return ResponseEntity.ok(usuarioService.alterarStatus(uid, dto.ativo(), solicitante.uid()));
    }
}
