package br.ufs.dcomp.collina.dto.estoque;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PagamentoPedidoRequestDTO(
        @Positive(message = "O valor pago deve ser maior que zero.")
        @Digits(integer = 8, fraction = 2, message = "O valor pago deve ter no máximo 8 dígitos inteiros e 2 casas decimais.")
        BigDecimal valorPago
) {}
