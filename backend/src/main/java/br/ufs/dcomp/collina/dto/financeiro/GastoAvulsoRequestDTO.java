package br.ufs.dcomp.collina.dto.financeiro;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record GastoAvulsoRequestDTO(

    @Size(max = 500)
    @NotBlank(message= "Informe a descrição do gasto")
    String descricao,

    @NotNull 
    @PositiveOrZero(message= "O valor do gasto não pode ser negativo")
    @Digits(integer = 13, fraction = 2, message = "O valor deve ter no máximo 2 casas decimais.") 
    BigDecimal valor,

    @NotBlank(message = "O gasto não está vinculado a um associado")
    Long autorDoGastoId

) {}
