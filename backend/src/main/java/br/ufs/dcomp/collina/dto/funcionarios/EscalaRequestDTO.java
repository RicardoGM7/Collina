package br.ufs.dcomp.collina.dto.funcionarios;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record EscalaRequestDTO(
        @NotBlank(message = "O período da escala é obrigatório.")
        @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "O período deve estar no formato AAAA-MM (ex: 2026-11).")
        String periodo
) {}
