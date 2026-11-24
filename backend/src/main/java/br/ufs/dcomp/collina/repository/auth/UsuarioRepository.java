package br.ufs.dcomp.collina.repository.auth;

import br.ufs.dcomp.collina.model.auth.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    Optional<Usuario> findByFirebaseUid(String firebaseUid);
    Optional<Usuario> findByEmailIgnoreCase(String email);
    Optional<Usuario> findByPessoaId(Integer pessoaId);
    boolean existsByFirebaseUid(String firebaseUid);
    boolean existsByEmailIgnoreCase(String email);
}
