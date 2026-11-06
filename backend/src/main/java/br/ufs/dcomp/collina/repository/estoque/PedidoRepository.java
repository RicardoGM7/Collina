package br.ufs.dcomp.collina.repository.estoque;


import br.ufs.dcomp.collina.model.estoque.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PedidoRepository extends JpaRepository<Pedido, Integer> {
}
