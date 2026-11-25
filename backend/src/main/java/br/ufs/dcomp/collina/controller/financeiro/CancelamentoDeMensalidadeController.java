package br.ufs.dcomp.collina.controller.financeiro;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.ufs.dcomp.collina.dto.financeiro.CancelamentoDeMensalidadeRequestDTO;
import br.ufs.dcomp.collina.dto.financeiro.MensalidadeResponseDTO;
import br.ufs.dcomp.collina.exception.ResourceNotFoundException;
import br.ufs.dcomp.collina.model.auth.Usuario;
import br.ufs.dcomp.collina.model.auth.UsuarioAutenticado;
import br.ufs.dcomp.collina.repository.auth.UsuarioRepository;
import br.ufs.dcomp.collina.service.financeiro.CancelamentoMensalidadeService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/mensalidades")
public class CancelamentoDeMensalidadeController {

    @Autowired
    private CancelamentoMensalidadeService cancelamentoService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    // UC-FIN05 / RF-F11: cancelar uma mensalidade PENDENTE ou ATRASADA e reemiti-la (200 OK).
    // O funcionário responsável é extraído do token autenticado, preservando a auditoria.
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'RECEPCAO', 'FINANCEIRO')")
    @PostMapping("/{id}/cancelar-e-reemitir")
    public ResponseEntity<MensalidadeResponseDTO> cancelarEReemitir(
            @PathVariable Long id,
            @Valid @RequestBody CancelamentoDeMensalidadeRequestDTO dto,
            @AuthenticationPrincipal UsuarioAutenticado usuarioLogado) {
        Long funcionarioId = obterIdFuncionario(usuarioLogado);
        return ResponseEntity.ok(cancelamentoService.cancelarEReemitir(dto, id, funcionarioId));
    }

    private Long obterIdFuncionario(UsuarioAutenticado usuario) {
        if (usuario == null || usuario.uid() == null || usuario.uid().isBlank()) {
            throw new IllegalArgumentException("Usuário não autenticado.");
        }
        Usuario usuarioBanco = usuarioRepository.findByFirebaseUid(usuario.uid())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado no cadastro do sistema."));
        return usuarioBanco.getId().longValue();
    }
}
