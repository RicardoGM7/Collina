package br.ufs.dcomp.collina.dto.funcionarios;

import java.util.List;

public record ValidacaoAlocacaoResponseDTO(
        boolean valido,
        List<String> motivos,
        Double horasDiariasAcumuladas,
        Double horasSemanaisAcumuladas
) {}
