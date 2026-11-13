package br.ufs.dcomp.collina.dto.financeiro;

import br.ufs.dcomp.collina.model.financeiro.TipoMovimentacaoEnum;
import br.ufs.dcomp.collina.model.financeiro.OrigemMovimentacaoEnum;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Digits;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MovimentacaoFinanceiraRequestDTO (

            @NotNull(message = "O tipo de movimentação é obrigatório")
            TipoMovimentacaoEnum tipo,

            @NotNull(message = "A movimentação deve ter uma origem.")
            OrigemMovimentacaoEnum origem,

            @NotNull(message = "A movimentação deve ter um valor.")
            @Positive(message = "A movimentação não pode ter preço nulo ou negativo")
            @Digits(integer = 13, fraction = 2, message = "O valor deve ter no máximo 2 casas decimais.")
            BigDecimal valor,

            String descricao,

            @NotNull(message = "A movimentação deve ter uma data.")
            LocalDateTime dataHora,

            Long pagamentoId,

            Long solicitacaoId
) {}
