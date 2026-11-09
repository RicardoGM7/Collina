package br.ufs.dcomp.collina.chat.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatRequestDTO(
        @NotBlank(message = "A mensagem não pode estar vazia.")
        String mensagem,
        String conversaId
) {
}
