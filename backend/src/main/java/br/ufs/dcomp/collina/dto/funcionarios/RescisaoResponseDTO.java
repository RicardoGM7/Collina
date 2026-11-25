package br.ufs.dcomp.collina.dto.funcionarios;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RescisaoResponseDTO(
        Integer contratoId,
        Integer funcionarioId,
        String nomeFuncionario,
        BigDecimal salarioBase,
        LocalDate dataFim,
        Integer diasTrabalhados,
        BigDecimal valorRescisaoProporcional
) {}
