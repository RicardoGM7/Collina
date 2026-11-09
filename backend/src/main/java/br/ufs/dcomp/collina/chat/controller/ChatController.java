package br.ufs.dcomp.collina.chat.controller;


import br.ufs.dcomp.collina.chat.dto.ChatRequestDTO;
import br.ufs.dcomp.collina.chat.dto.ChatResponseDTO;
import br.ufs.dcomp.collina.chat.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
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
     * Endpoint para conversar com o assistente do clube.
     * Exemplo: POST /api/chat
     * Body: { "mensagem": "Adicione 2 cocas no pedido 1" }
     */
    @PostMapping
    public ResponseEntity<ChatResponseDTO> conversar(@RequestBody @Valid ChatRequestDTO request) {
        // 1. Envia o texto do usuário para o ChatService processar com o Gemini
        var resultado = chatService.responder(request.mensagem(), request.conversaId());

        // 2. Encapsula o texto no nosso DTO de resposta e devolve HTTP 200 (OK)
        return ResponseEntity.ok(new ChatResponseDTO(resultado.resposta(), resultado.conversaId()));    }
}