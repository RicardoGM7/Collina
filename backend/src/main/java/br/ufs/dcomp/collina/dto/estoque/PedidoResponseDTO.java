package br.ufs.dcomp.collina.dto.estoque;

import br.ufs.dcomp.collina.model.estoque.ItemPedido;
import br.ufs.dcomp.collina.model.estoque.Pedido;
import br.ufs.dcomp.collina.model.estoque.StatusTransacaoEnum;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

//FALTA O PAYMENT E MUDAR TUDO PARA BIGDECIMAL

public record PedidoResponseDTO(
        Integer id,
        Instant data,
        StatusTransacaoEnum statusPedido,
        Double total,                             // <-- O total do pedido calculado!
        Set<ItemPedidoResponseDTO> itens          // Sem vírgula no final aqui!
) {

    // 1. Construtor auxiliar do Pedido completo
    public PedidoResponseDTO(Pedido pedido) {
        this(
                pedido.getId(),
                pedido.getData(),
                pedido.getStatus(),
                // Calcula o total somando (preço * quantidade) de cada item
                pedido.getItens() != null
                        ? pedido.getItens().stream().mapToDouble(i -> i.getPreco() * i.getQuantidade()).sum()
                        : 0.0,
                // Converte a lista de ItemPedido para o Set de DTOs
                pedido.getItens() != null
                        ? pedido.getItens().stream().map(ItemPedidoResponseDTO::new).collect(Collectors.toSet())
                        : Set.of()
        );
    }

    // Record aninhado para as linhas do pedido
    public record ItemPedidoResponseDTO(
            Integer produtoId,
            String nomeDoProduto,
            Integer quantidade,
            Double preco,
            Double subTotal,                      // <-- subTotal (preco * quantidade)
            String descricao
    ) {
        // 2. Construtor auxiliar de cada Item
        public ItemPedidoResponseDTO(ItemPedido item) {
            this(
                    item.getProduto() != null ? item.getProduto().getId() : null,
                    item.getProduto() != null ? item.getProduto().getNome() : null,
                    item.getQuantidade(),
                    item.getPreco(),
                    (item.getPreco() != null && item.getQuantidade() != null)
                            ? item.getPreco() * item.getQuantidade() : 0.0,
                    item.getProduto() != null ? item.getProduto().getDescricao() : null
            );
        }
    }
}