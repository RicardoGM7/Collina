package br.ufs.dcomp.collina.chat.dto;

import java.util.List;

/**
 * Representa uma alteração no banco de dados proposta pela IA que aguarda confirmação explícita do usuário.
 */
public record AcaoPendenteDTO(
        String acaoId,
        String titulo,
        String descricao,
        List<String> detalhes,
        boolean critica
) {
}
