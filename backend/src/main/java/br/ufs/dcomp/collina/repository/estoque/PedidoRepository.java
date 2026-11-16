package br.ufs.dcomp.collina.repository.estoque;

import br.ufs.dcomp.collina.model.estoque.Pedido;
import br.ufs.dcomp.collina.model.estoque.StatusTransacaoEnum;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Integer> {

    @Override
    @EntityGraph(attributePaths = {"itens", "itens.produto"})
    List<Pedido> findAll();

    @Override
    @EntityGraph(attributePaths = {"itens", "itens.produto"})
    Optional<Pedido> findById(Integer id);

    // RF-E17: Filtrar pedidos por status e período
    @EntityGraph(attributePaths = {"itens", "itens.produto"})
    @Query("""
            SELECT p FROM Pedido p
            WHERE (:status IS NULL OR p.status = :status)
              AND p.data >= :inicio
              AND p.data < :fim
            ORDER BY p.data DESC
            """)
    List<Pedido> buscarPorFiltro(StatusTransacaoEnum status, Instant inicio, Instant fim);
}
