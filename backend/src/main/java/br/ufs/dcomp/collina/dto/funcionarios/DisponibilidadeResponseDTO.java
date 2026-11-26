package br.ufs.dcomp.collina.dto.funcionarios;

import java.time.LocalDate;

public record DisponibilidadeResponseDTO(
        Integer funcionarioId,
        String nomeFuncionario,
        LocalDate data,
        boolean disponivel,
        String motivoIndisponibilidade
) {}
