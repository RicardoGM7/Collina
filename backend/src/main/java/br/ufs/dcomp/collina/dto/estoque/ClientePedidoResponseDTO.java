package br.ufs.dcomp.collina.dto.estoque;

import br.ufs.dcomp.collina.model.auth.PerfilUsuarioEnum;
import br.ufs.dcomp.collina.model.auth.Usuario;

public record ClientePedidoResponseDTO(
        Integer id,
        String nome,
        String email,
        PerfilUsuarioEnum perfil
) {
    public ClientePedidoResponseDTO(Usuario usuario) {
        this(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getPerfil()
        );
    }
}
