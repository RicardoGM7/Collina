package br.ufs.dcomp.collina.dto.financeiro;

import br.ufs.dcomp.collina.model.financeiro.PrioridadeGastoEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Digits;
import java.time.LocalDateTime;

import java.math.BigDecimal;

public record SolicitacaoGastoRequestDTO (
        @NotBlank(message = "A descrição da soliticação não pode ser nula.")
        String descricao,

        @NotNull(message = "O valor da solicitação não pode ser nulo.")
        @Positive(message = "O valor da solicitação deve ser maior que zero.")
        @Digits(integer = 13, fraction = 2, message = "O valor deve ter no máximo 2 casas decimais.")
        BigDecimal valor,

        @NotNull(message = "A prioridade da solicitação não pode ser nula.")
        PrioridadeGastoEnum prioridade,

        @NotNull(message = "A data da solicitacao não pode ser nula.")
        LocalDateTime dataSolicitacao
) {}
