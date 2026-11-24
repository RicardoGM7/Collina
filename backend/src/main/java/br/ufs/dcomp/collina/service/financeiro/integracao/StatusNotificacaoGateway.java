package br.ufs.dcomp.collina.service.financeiro.integracao;

/** Situações que o financeiro entende de um gateway, independentes do provedor. */
public enum StatusNotificacaoGateway {
    /** Ainda em processamento: nada a fazer. */
    PENDENTE,
    /** Pagamento aprovado: Pagamento CONFIRMADO. */
    APROVADO,
    /** Pagamento recusado: Pagamento RECUSADO. */
    RECUSADO,
    /** Cobrança venceu sem pagamento: Pagamento EXPIRADO. */
    EXPIRADO;
}
