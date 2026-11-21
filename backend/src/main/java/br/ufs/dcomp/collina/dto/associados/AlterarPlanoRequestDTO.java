package br.ufs.dcomp.collina.dto.associados;

import br.ufs.dcomp.collina.model.associados.TipoPlanoEnum;
import jakarta.validation.constraints.NotNull;

public record AlterarPlanoRequestDTO(
        @NotNull(message = "O novo plano é obrigatório.")
        TipoPlanoEnum tipoPlano
) {
}
