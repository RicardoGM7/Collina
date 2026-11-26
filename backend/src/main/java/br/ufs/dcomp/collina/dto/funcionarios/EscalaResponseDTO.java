package br.ufs.dcomp.collina.dto.funcionarios;

import br.ufs.dcomp.collina.model.funcionarios.Escala;
import br.ufs.dcomp.collina.model.funcionarios.StatusEscala;

public record EscalaResponseDTO(
        Integer id,
        String periodo,
        StatusEscala status,
        Integer totalAlocacoes
) {
    public EscalaResponseDTO(Escala escala) {
        this(
                escala.getId(),
                escala.getPeriodo(),
                escala.getStatus(),
                escala.getAlocacoes() != null ? escala.getAlocacoes().size() : 0
        );
    }
}
