package br.ufs.dcomp.collina.dto.estoque;

import br.ufs.dcomp.collina.model.estoque.PagamentoPedido;

import java.math.BigDecimal;
import java.time.Instant;

public record PagamentoPedidoResponseDTO(
        Integer id,
        Instant data,
        BigDecimal valor,
        Integer pedidoId
) {
    public PagamentoPedidoResponseDTO(PagamentoPedido p) {
        this(
                p.getId(),
                p.getData(),
                p.getValor(),
                p.getPedido() != null ? p.getPedido().getId() : null
        );
    }
}
