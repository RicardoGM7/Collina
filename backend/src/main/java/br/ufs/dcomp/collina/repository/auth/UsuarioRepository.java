package br.ufs.dcomp.collina.repository.auth;

import br.ufs.dcomp.collina.model.auth.StatusUsuarioEnum;
import br.ufs.dcomp.collina.model.auth.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    Optional<Usuario> findByFirebaseUid(String firebaseUid);
    Optional<Usuario> findByEmailIgnoreCase(String email);
    Optional<Usuario> findByPessoaId(Integer pessoaId);
    List<Usuario> findByStatusOrderByNomeAsc(StatusUsuarioEnum status);
    boolean existsByFirebaseUid(String firebaseUid);
    boolean existsByEmailIgnoreCase(String email);
}
