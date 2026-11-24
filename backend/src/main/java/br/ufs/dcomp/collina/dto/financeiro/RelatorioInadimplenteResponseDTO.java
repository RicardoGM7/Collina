package br.ufs.dcomp.collina.dto.financeiro;

import java.math.BigDecimal;
import java.time.LocalDate;

import br.ufs.dcomp.collina.model.financeiro.Mensalidade;

/**
 * Item do relatório de inadimplentes (RF-F15).
 * Apresenta as mensalidades com status ATRASADA e os dados do titular responsável.
 */
public record RelatorioInadimplenteResponseDTO(
    Integer associadoId,
    String nomeAssociado,
    BigDecimal valor,
    String competencia,
    LocalDate dataVencimento
) {
    public RelatorioInadimplenteResponseDTO(Mensalidade mensalidade) {
        this(
            mensalidade.getAssociadoTitular() != null ? mensalidade.getAssociadoTitular().getId() : null,
            mensalidade.getAssociadoTitular() != null && mensalidade.getAssociadoTitular().getPessoa() != null
                ? mensalidade.getAssociadoTitular().getPessoa().getNome()
                : null,
            mensalidade.getValorFinal(),
            mensalidade.getCompetencia(),
            mensalidade.getDataVencimento()
        );
    }
}
