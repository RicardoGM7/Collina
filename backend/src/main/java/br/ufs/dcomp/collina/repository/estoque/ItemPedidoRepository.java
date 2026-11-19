package br.ufs.dcomp.collina.repository.estoque;

import br.ufs.dcomp.collina.model.estoque.ItemPedido;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItemPedidoRepository extends JpaRepository<ItemPedido, Integer> {

    @EntityGraph(attributePaths = {"produto"})
    List<ItemPedido> findByPedidoId(Integer pedidoId);

    boolean existsByProdutoId(Integer produtoId);
}
