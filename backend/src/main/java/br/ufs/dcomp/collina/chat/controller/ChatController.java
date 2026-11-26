package br.ufs.dcomp.collina.chat.controller;

import br.ufs.dcomp.collina.chat.dto.ChatRequestDTO;
import br.ufs.dcomp.collina.chat.dto.ChatResponseDTO;
import br.ufs.dcomp.collina.chat.dto.ResultadoAcaoDTO;
import br.ufs.dcomp.collina.chat.service.ChatService;
import br.ufs.dcomp.collina.model.auth.UsuarioAutenticado;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * Endpoint para conversar com a assistente IA (Carollina AI).
     * Requer autenticação Firebase (perfil controlado no SecurityConfig e no ChatService).
     */
    @PostMapping
    public ResponseEntity<ChatResponseDTO> conversar(
            @RequestBody @Valid ChatRequestDTO request,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {

        var resultado = chatService.responder(request.mensagem(), request.conversaId(), usuario);
        return ResponseEntity.ok(new ChatResponseDTO(
                resultado.resposta(),
                resultado.conversaId(),
                resultado.erro(),
                resultado.acoesPendentes()
        ));
    }

    /**
     * Confirma e executa uma alteração no banco proposta anteriormente pela IA.
     */
    @PostMapping("/acoes/{acaoId}/confirmar")
    public ResponseEntity<ResultadoAcaoDTO> confirmarAcao(
            @PathVariable String acaoId,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {

        return ResponseEntity.ok(chatService.confirmarAcao(acaoId, usuario));
    }

    /**
     * Cancela uma alteração proposta pela IA sem modificar o banco de dados.
     */
    @PostMapping("/acoes/{acaoId}/cancelar")
    public ResponseEntity<ResultadoAcaoDTO> cancelarAcao(
            @PathVariable String acaoId,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {

        return ResponseEntity.ok(chatService.cancelarAcao(acaoId, usuario));
    }

    /**
     * Limpa a memória e ações pendentes de uma conversa encerrada pelo usuário logado.
     */
    @DeleteMapping("/{conversaId}")
    public ResponseEntity<Void> limparConversa(
            @PathVariable String conversaId,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {

        chatService.limparConversa(conversaId, usuario);
        return ResponseEntity.noContent().build();
    }
}
