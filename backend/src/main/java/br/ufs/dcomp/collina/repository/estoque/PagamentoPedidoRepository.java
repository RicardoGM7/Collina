package br.ufs.dcomp.collina.repository.estoque;

import br.ufs.dcomp.collina.model.estoque.PagamentoPedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PagamentoPedidoRepository extends JpaRepository<PagamentoPedido, Integer> {
    Optional<PagamentoPedido> findByPedidoId(Integer pedidoId);
}
