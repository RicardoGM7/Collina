package br.ufs.dcomp.collina.dto.financeiro;

import java.math.BigDecimal;
import java.time.LocalDate;


import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CancelamentoDeMensalidadeRequestDTO(
    
    @Size(max = 500)
    @NotBlank(message = "Justificativa do cancelamento necessária") 
    String justificativa,

    @NotNull(message = "A nova mensalidade deve ter uma data de vencimento")
    LocalDate novoVencimento,

    @NotNull(message = "A nova mensalidade deve ter um valor.")
    @Positive(message = "A nova mensalidade não pode ter preço nulo ou negativo")
    @Digits(integer = 13, fraction = 2, message = "O valor deve ter no máximo 2 casas decimais.") 
    BigDecimal novoValor,

    // Temporario
    @NotNull(message = "Informe o funcionário responsavel pelo cancelamento")
    Long funcionarioId
) {}
