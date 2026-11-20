package br.ufs.dcomp.collina.dto.auth;

import br.ufs.dcomp.collina.model.auth.PerfilUsuarioEnum;
import jakarta.validation.constraints.NotNull;

public record AlterarPerfilRequestDTO(
        @NotNull(message = "O perfil é obrigatório")
        PerfilUsuarioEnum perfil
) {
}
