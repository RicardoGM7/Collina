package br.ufs.dcomp.collina.controller.financeiro;

import java.net.URI;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import br.ufs.dcomp.collina.dto.financeiro.ConfirmacaoManualRequest;
import br.ufs.dcomp.collina.dto.financeiro.PagamentoResponseDTO;
import br.ufs.dcomp.collina.exception.ResourceNotFoundException;
import br.ufs.dcomp.collina.model.auth.Usuario;
import br.ufs.dcomp.collina.model.auth.UsuarioAutenticado;
import br.ufs.dcomp.collina.repository.auth.UsuarioRepository;
import br.ufs.dcomp.collina.service.financeiro.PagamentoService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api")
public class PagamentoController {

    @Autowired
    private PagamentoService pagamentoService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    // RF-F18 / RF-F22: confirma manualmente o pagamento de uma mensalidade PENDENTE ou ATRASADA
    // (201 Created). Cria o Pagamento, marca a mensalidade como PAGA e lança a entrada no caixa.
    // O funcionário autenticado é obtido a partir de tb_usuario através do token Firebase.
    @PostMapping("/mensalidades/{mensalidadeId}/confirmar-pagamento")
    public ResponseEntity<PagamentoResponseDTO> confirmarManualmente(
            @PathVariable Long mensalidadeId,
            @Valid @RequestBody ConfirmacaoManualRequest dados,
            @AuthenticationPrincipal UsuarioAutenticado usuarioLogado) {

        Long funcionarioId = obterIdFuncionario(usuarioLogado);
        PagamentoResponseDTO response = pagamentoService.confirmarManualmente(mensalidadeId, dados, funcionarioId);

        // O request é de /mensalidades/..., então o Location é montado a partir da raiz da aplicação.
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/pagamentos/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    private Long obterIdFuncionario(UsuarioAutenticado usuario) {
        if (usuario == null || usuario.uid() == null || usuario.uid().isBlank()) {
            throw new IllegalArgumentException("Usuário não autenticado.");
        }
        Usuario usuarioBanco = usuarioRepository.findByFirebaseUid(usuario.uid())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado no cadastro do sistema."));
        return usuarioBanco.getId().longValue();
    }

    // Consulta um pagamento (200 OK). 404 se não existir.
    @GetMapping("/pagamentos/{pagamentoId}")
    public ResponseEntity<PagamentoResponseDTO> buscarPorId(@PathVariable Long pagamentoId) {
        return ResponseEntity.ok(pagamentoService.buscarPorId(pagamentoId));
    }

    // Tentativas de pagamento de uma mensalidade (200 OK). 404 se a mensalidade não existir.
    @GetMapping("/mensalidades/{mensalidadeId}/pagamentos")
    public ResponseEntity<List<PagamentoResponseDTO>> listarPorMensalidade(@PathVariable Long mensalidadeId) {
        return ResponseEntity.ok(pagamentoService.listarPorMensalidade(mensalidadeId));
    }

    // Histórico de pagamentos de um titular (200 OK). associadoId é obrigatório (400 se ausente).
    // Restrito a administradores e equipe financeira. Associados consultam seus pagamentos via /api/associados/me/pagamentos.
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'FINANCEIRO')")
    @GetMapping("/pagamentos")
    public ResponseEntity<List<PagamentoResponseDTO>> listarPorTitular(@RequestParam Integer associadoId) {
        return ResponseEntity.ok(pagamentoService.listarPorTitular(associadoId));
    }
}