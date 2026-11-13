package br.ufs.dcomp.collina.dto.financeiro;

import br.ufs.dcomp.collina.model.financeiro.PrioridadeGastoEnum;
import br.ufs.dcomp.collina.model.financeiro.StatusSolicitacaoGastoEnum;
import br.ufs.dcomp.collina.model.financeiro.SolicitacaoGasto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SolicitacaoGastoResponseDTO (
        Long id,
        String descricao,
        BigDecimal valor,
        PrioridadeGastoEnum prioridade,
        StatusSolicitacaoGastoEnum status,
        LocalDateTime dataSolicitacao,
        Long funcionarioId
) {
        public SolicitacaoGastoResponseDTO (
                        SolicitacaoGasto solicitacao) {
                this (
                        solicitacao.getId(),
                        solicitacao.getDescricao(),
                        solicitacao.getValor(),
                        solicitacao.getPrioridade(),
                        solicitacao.getStatus(),
                        solicitacao.getDataSolicitacao(),
                        solicitacao.getFuncionarioId()
                );
        }
}
