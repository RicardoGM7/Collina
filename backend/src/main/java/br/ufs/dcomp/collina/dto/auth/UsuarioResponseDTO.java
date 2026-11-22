package br.ufs.dcomp.collina.dto.auth;

import br.ufs.dcomp.collina.model.auth.PerfilUsuarioEnum;

public record UsuarioResponseDTO(
        String uid,
        String email,
        String nome,
        PerfilUsuarioEnum perfil,
        boolean ativo,
        boolean emailVerificado
) {
}
