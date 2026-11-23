package br.ufs.dcomp.collina.dto.auth;

import br.ufs.dcomp.collina.model.auth.PerfilUsuarioEnum;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UsuarioCriarRequestDTO(
        @NotBlank(message = "O nome é obrigatório")
        String nome,

        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "E-mail inválido")
        String email,

        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 6, max = 128, message = "A senha deve ter entre 6 e 128 caracteres")
        String senha,

        @NotNull(message = "O perfil é obrigatório")
        PerfilUsuarioEnum perfil
) {
}
