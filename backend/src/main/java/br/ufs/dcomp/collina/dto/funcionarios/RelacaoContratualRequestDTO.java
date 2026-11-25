package br.ufs.dcomp.collina.dto.funcionarios;

import br.ufs.dcomp.collina.model.funcionarios.TipoContrato;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RelacaoContratualRequestDTO(
        @NotNull(message = "O ID do funcionário é obrigatório.")
        Integer funcionarioId,

        @NotNull(message = "O tipo de contrato é obrigatório.")
        TipoContrato tipo,

        @NotNull(message = "O salário é obrigatório.")
        @DecimalMin(value = "0.0", inclusive = false, message = "O salário deve ser maior que zero.")
        BigDecimal salario,

        @NotNull(message = "A data de início é obrigatória.")
        LocalDate dataInicio,

        LocalDate dataFim
) {}
