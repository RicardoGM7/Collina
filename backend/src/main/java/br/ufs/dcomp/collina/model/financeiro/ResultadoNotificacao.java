package br.ufs.dcomp.collina.model.financeiro;

/** O que aconteceu com uma notificação do gateway. Só ASSINATURA_INVALIDA vira erro (401). */
public enum ResultadoNotificacao {
    /** Mudou o estado do pagamento (e, se aprovado, da mensalidade e do caixa). */
    PROCESSADA,
    /** Nada a fazer: duplicada, fora de ordem, ainda pendente, cobrança desconhecida ou que exige conferência manual. */
    IGNORADA,
    /** Não veio do gateway: não foi processada. */
    ASSINATURA_INVALIDA;
}
