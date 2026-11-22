package br.ufs.dcomp.collina.dto.auth;

import jakarta.validation.constraints.NotNull;

public record AlterarStatusUsuarioRequestDTO(
        @NotNull(message = "O status é obrigatório")
        Boolean ativo
) {
}
