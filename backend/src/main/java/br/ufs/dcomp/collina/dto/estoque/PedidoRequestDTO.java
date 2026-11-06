package br.ufs.dcomp.collina.dto.estoque;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.Set;

public record PedidoRequestDTO(
        @NotEmpty(message = "O pedido deve conter pelo menos um item.")
        @Valid
        Set<ItemPedidoRequestDTO> itens
) {
    public record ItemPedidoRequestDTO(
            @NotNull(message = "O ID do produto é obrigatório.")
            Integer produtoId,

            @NotNull(message = "A quantidade é obrigatória.")
            @Positive(message = "A quantidade do item deve ser maior que zero.")
            Integer quantidade
    ) {}
}
