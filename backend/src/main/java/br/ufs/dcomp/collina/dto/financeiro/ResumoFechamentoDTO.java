package br.ufs.dcomp.collina.dto.financeiro;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Resultado de uma execução do fechamento mensal.
 *
 * @param competencia        competência aberta nesta execução, no formato AAAA-MM
 * @param processados        titulares cuja mensalidade foi fechada e cuja nova mensalidade foi aberta
 * @param ignorados          titulares que já tinham a mensalidade da nova competência (repetição segura)
 * @param titularesComFalha  ids dos titulares cuja transação falhou e foi desfeita; podem ser reprocessados
 * @param dataExecucao       data e hora em que o fechamento foi executado
 */
public record ResumoFechamentoDTO(
        String competencia,
        int processados,
        int ignorados,
        List<Integer> titularesComFalha,
        LocalDateTime dataExecucao
) {
    public ResumoFechamentoDTO(
            String competencia,
            int processados,
            int ignorados,
            List<Integer> titularesComFalha) {
        this(competencia, processados, ignorados, titularesComFalha, null);
    }
}
