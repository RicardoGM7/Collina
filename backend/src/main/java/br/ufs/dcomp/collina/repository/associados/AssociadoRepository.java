package br.ufs.dcomp.collina.repository.associados;

import br.ufs.dcomp.collina.model.associados.Associado;
import br.ufs.dcomp.collina.model.associados.SituacaoCadastralEnum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AssociadoRepository extends JpaRepository<Associado, Integer> {
    Optional<Associado> findByPessoaCpf(String cpf);
    Optional<Associado> findByUsuarioId(String usuarioId);
    boolean existsByPessoaCpf(String cpf);
    boolean existsByUsuarioId(String usuarioId);
    // Usado pelo Financeiro na emissão de mensalidades (RF-F05)
    List<Associado> findBySituacaoCadastralIn(Collection<SituacaoCadastralEnum> situacoes);
}
