package br.ufs.dcomp.collina.dto.espacos;

import br.ufs.dcomp.collina.model.espacos.SituacaoBlocoEnum;
import br.ufs.dcomp.collina.model.espacos.TipoBlocoEnum;

import java.time.LocalDate;
import java.util.List;

// RF-ESP03 / UC-GE01: agenda de um espaço em um dia.
// Associado recebe só os blocos LIVRES; funcionários recebem todos com a situação de cada um.
public record DisponibilidadeResponseDTO(
        Integer espacoId,
        LocalDate data,
        List<BlocoDisponibilidadeDTO> blocos,
        String mensagem
) {
    public record BlocoDisponibilidadeDTO(
            Integer blocoId,
            String horaInicio,
            String horaFim,
            TipoBlocoEnum tipo,
            SituacaoBlocoEnum situacao
    ) {
    }
}
