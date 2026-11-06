package br.ufs.dcomp.collina.dto.estoque;

import br.ufs.dcomp.collina.model.estoque.PagamentoPedido;

import java.time.Instant;

public record PagamentoPedidoResponseDTO(
        Integer id,
        Instant data,
        Integer pedidoId
) {
    public PagamentoPedidoResponseDTO(PagamentoPedido p) {
        this(
                p.getId(),
                p.getData(),
                p.getPedido() != null ? p.getPedido().getId() : null
        );
    }
}
