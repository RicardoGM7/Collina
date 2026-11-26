package br.ufs.dcomp.collina.controller.financeiro;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.ufs.dcomp.collina.dto.financeiro.GastoAvulsoRequestDTO;
import br.ufs.dcomp.collina.dto.financeiro.MensalidadeResponseDTO;
import br.ufs.dcomp.collina.exception.ResourceNotFoundException;
import br.ufs.dcomp.collina.model.auth.Usuario;
import br.ufs.dcomp.collina.model.auth.UsuarioAutenticado;
import br.ufs.dcomp.collina.repository.auth.UsuarioRepository;
import br.ufs.dcomp.collina.service.financeiro.MensalidadeService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/mensalidades")
public class MensalidadeController {

    @Autowired
    private MensalidadeService mensalidadeService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    // RF-F06: listar as mensalidades de um titular (todas ou de uma competência) (200 OK).
    @GetMapping
    public ResponseEntity<List<MensalidadeResponseDTO>> listar(
            @RequestParam("associadoId") Integer associadoId,
            @RequestParam(name = "competencia", required = false) YearMonth competencia) {
        return ResponseEntity.ok(mensalidadeService.listarMensalidadesDoAssociado(associadoId, competencia));
    }

    // RF-F08: lançar gasto avulso (200 OK). O funcionário responsável é resolvido a partir
    // do usuário autenticado no token Firebase.
    @PostMapping("/gastos-avulsos")
    public ResponseEntity<MensalidadeResponseDTO> incluirGastoAvulso(
            @Valid @RequestBody GastoAvulsoRequestDTO dto,
            @AuthenticationPrincipal UsuarioAutenticado usuarioLogado) {
        Long funcionarioId = obterIdFuncionario(usuarioLogado);
        MensalidadeResponseDTO mensalidade = mensalidadeService.incluirGastoAvulso(dto, funcionarioId, LocalDateTime.now());
        return ResponseEntity.ok(mensalidade);
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
