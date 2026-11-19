package br.ufs.dcomp.collina.model.estoque;

public enum StatusTransacaoEnum {
    PAGO,
    AGUARDANDO_PAGAMENTO,
    CONFIRMADO,
    RECUSADO,
    EXPIRADO,
    CANCELADO;
}
