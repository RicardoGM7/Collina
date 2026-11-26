package br.ufs.dcomp.collina.chat.dto;

/**
 * Resposta da confirmação ou cancelamento de uma ação pendente da IA.
 */
public record ResultadoAcaoDTO(
        String acaoId,
        String status,
        String mensagem,
        boolean erro
) {
}
