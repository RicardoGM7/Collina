package br.ufs.dcomp.collina.chat.dto;

import java.util.List;

public record ChatResponseDTO(
        String mensagem,
        String conversaId,
        boolean erro,
        List<AcaoPendenteDTO> acoesPendentes
) {
    public ChatResponseDTO(String mensagem, String conversaId) {
        this(mensagem, conversaId, false, List.of());
    }

    public ChatResponseDTO(String mensagem, String conversaId, boolean erro) {
        this(mensagem, conversaId, erro, List.of());
    }
}
