package br.ufs.dcomp.collina.dto.estoque;

import br.ufs.dcomp.collina.model.estoque.ItemPedido;
import br.ufs.dcomp.collina.model.estoque.Pedido;
import br.ufs.dcomp.collina.model.estoque.StatusTransacaoEnum;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

public record PedidoResponseDTO(
        Integer id,
        Instant data,
        StatusTransacaoEnum statusPedido,
        BigDecimal total,
        Set<ItemPedidoResponseDTO> itens
) {

    // 1. Construtor auxiliar do Pedido completo
    public PedidoResponseDTO(Pedido pedido) {
        this(
                pedido.getId(),
                pedido.getData(),
                pedido.getStatus(),
                // Calcula o total somando (preço * quantidade) de cada item com precisão BigDecimal
                pedido.getItens() != null
                        ? pedido.getItens().stream()
                                .map(i -> (i.getPreco() != null && i.getQuantidade() != null)
                                        ? i.getPreco().multiply(BigDecimal.valueOf(i.getQuantidade()))
                                        : BigDecimal.ZERO)
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                        : BigDecimal.ZERO,
                // Converte a lista de ItemPedido para o Set de DTOs
                pedido.getItens() != null
                        ? pedido.getItens().stream().map(ItemPedidoResponseDTO::new).collect(Collectors.toSet())
                        : Set.of()
        );
    }

    // Record aninhado para as linhas do pedido
    public record ItemPedidoResponseDTO(
            Integer id,
            Integer produtoId,
            String nomeDoProduto,
            Integer quantidade,
            BigDecimal preco,
            BigDecimal subTotal,
            String descricao
    ) {
        // 2. Construtor auxiliar de cada Item
        public ItemPedidoResponseDTO(ItemPedido item) {
            this(
                    item.getId(),
                    item.getProduto() != null ? item.getProduto().getId() : null,
                    item.getProduto() != null ? item.getProduto().getNome() : null,
                    item.getQuantidade(),
                    item.getPreco(),
                    (item.getPreco() != null && item.getQuantidade() != null)
                            ? item.getPreco().multiply(BigDecimal.valueOf(item.getQuantidade()))
                            : BigDecimal.ZERO,
                    item.getProduto() != null ? item.getProduto().getDescricao() : null
            );
        }
    }
}