package br.ufs.dcomp.collina.dto.associados;

import br.ufs.dcomp.collina.model.associados.SituacaoCadastralEnum;
import jakarta.validation.constraints.NotNull;

public record AlterarSituacaoRequestDTO(
        @NotNull(message = "A nova situação cadastral é obrigatória.")
        SituacaoCadastralEnum situacao
) {
}
