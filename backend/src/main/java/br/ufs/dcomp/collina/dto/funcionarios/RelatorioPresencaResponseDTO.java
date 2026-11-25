package br.ufs.dcomp.collina.dto.funcionarios;

import java.util.List;
import java.util.Map;

public record RelatorioPresencaResponseDTO(
        long totalAlocacoes,
        long totalPresencas,
        long totalAusencias,
        double porcentagemPresenca,
        List<FuncionarioRankingDTO> maisPresentes,
        List<FuncionarioRankingDTO> maisAusentes,
        Map<String, Long> datasMaiorAusencia
) {}
