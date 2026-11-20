package br.ufs.dcomp.collina.dto.estoque;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CategoriaRequestDTO(
        @NotBlank(message = "O nome da categoria é obrigatório.")
        String nome,

        @Pattern(
                regexp = "^#[0-9A-Fa-f]{6}$",
                message = "A cor deve estar no formato hexadecimal (ex: #2BB673)."
        )
        String cor
) {}
