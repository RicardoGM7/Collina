package br.ufs.dcomp.collina.dto.estoque;

import br.ufs.dcomp.collina.model.estoque.MovimentacaoEstoque;
import br.ufs.dcomp.collina.model.estoque.MovimentacaoEstoqueEnum;

import java.time.Instant;

public record MovimentacaoEstoqueResponseDTO(
        Integer id,
        Integer produtoID,
        String nomeProduto,
        Instant data,
        MovimentacaoEstoqueEnum tipo,
        Integer quantidade,
        String observacao
        // Integer funcionarioID
) {

    public MovimentacaoEstoqueResponseDTO(MovimentacaoEstoque mov) {
        this(
                mov.getId(),
                (mov.getEstoque() != null && mov.getEstoque().getProduto() != null) ? mov.getEstoque().getProduto().getId() : null,
                (mov.getEstoque() != null && mov.getEstoque().getProduto() != null) ? mov.getEstoque().getProduto().getNome() : null,
                mov.getData(),
                mov.getTipo(),
                mov.getQuantidade(),
                mov.getObservacao()
                // mov.getResponsavel().getId()
        );
    }


}
