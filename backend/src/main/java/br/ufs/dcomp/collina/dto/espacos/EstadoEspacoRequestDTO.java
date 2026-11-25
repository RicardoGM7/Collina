package br.ufs.dcomp.collina.dto.espacos;

import br.ufs.dcomp.collina.model.espacos.EstadoEspacoEnum;
import jakarta.validation.constraints.NotNull;

public record EstadoEspacoRequestDTO(
        @NotNull(message = "O estado do espaço é obrigatório.")
        EstadoEspacoEnum estado
) {
}
