package br.ufs.dcomp.collina.dto.financeiro;

import br.ufs.dcomp.collina.model.financeiro.TipoMovimentacaoEnum;
import br.ufs.dcomp.collina.model.financeiro.MovimentacaoFinanceira;
import br.ufs.dcomp.collina.model.financeiro.OrigemMovimentacaoEnum;
import br.ufs.dcomp.collina.model.financeiro.SituacaoMovimentacaoEnum;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MovimentacaoFinanceiraResponseDTO (
            Long id,
            TipoMovimentacaoEnum tipo,
            OrigemMovimentacaoEnum origem,
            BigDecimal valor,
            String descricao,
            LocalDateTime dataHora,
            SituacaoMovimentacaoEnum situacao,
            Long pagamentoId
) {
    public MovimentacaoFinanceiraResponseDTO(
            MovimentacaoFinanceira movimentacao) {
        this(
            movimentacao.getId(),
            movimentacao.getTipo(),
            movimentacao.getOrigem(),
            movimentacao.getValor(),
            movimentacao.getDescricao() != null ? movimentacao.getDescricao() : null,
            movimentacao.getDataHora(),
            movimentacao.getSituacao(),
            movimentacao.getPagamentoId()
        );
    }
}
