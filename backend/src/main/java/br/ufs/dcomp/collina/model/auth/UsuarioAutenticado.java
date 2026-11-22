package br.ufs.dcomp.collina.model.auth;

// Usuário da requisição atual, montado a partir do ID token do Firebase.
// Nos controllers: public ResponseEntity<?> metodo(@AuthenticationPrincipal UsuarioAutenticado usuario)
// O uid é o valor a gravar em Associado.usuarioId (vínculo AUTH01).
// perfil pode ser null se o usuário ainda não foi sincronizado (POST /api/auth/sincronizar).
public record UsuarioAutenticado(
        String uid,
        String email,
        String nome,
        PerfilUsuarioEnum perfil,
        boolean emailVerificado
) {
}
