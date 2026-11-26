package br.ufs.dcomp.collina.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequestDTO(
        @NotBlank(message = "A mensagem não pode estar vazia.")
        @Size(max = 2000, message = "A mensagem deve ter no máximo 2000 caracteres.")
        String mensagem,

        @Size(max = 100, message = "O identificador da conversa deve ter no máximo 100 caracteres.")
        String conversaId
) {
}
