package br.ufs.dcomp.collina.dto.financeiro;

import br.ufs.dcomp.collina.model.financeiro.TipoMovimentacaoEnum;
import br.ufs.dcomp.collina.model.financeiro.OrigemMovimentacaoEnum;

import java.math.BigDecimal;
import java.util.List;

public record RelatorioFechamentoResponseDTO (
        String competencia,
        BigDecimal totalEntradas,
        BigDecimal totalSaidas,
        BigDecimal saldoPeriodo,
        List<ItemCategoriaDTO> porCategoria
) {
    public record ItemCategoriaDTO (
        TipoMovimentacaoEnum tipo,
        OrigemMovimentacaoEnum origem,
        BigDecimal total
    ) {}
}
