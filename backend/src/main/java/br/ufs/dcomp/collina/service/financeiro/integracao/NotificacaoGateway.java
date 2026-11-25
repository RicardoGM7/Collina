package br.ufs.dcomp.collina.service.financeiro.integracao;

import java.math.BigDecimal;

/**
 * Notificação do gateway já traduzida para o nosso formato.
 *
 * @param idExterno  cobrança a que a notificação se refere
 * @param status     situação informada pelo gateway
 * @param valor      valor que o gateway diz ter recebido (conferido contra a mensalidade, RF-F22)
 */
public record NotificacaoGateway(String idExterno, StatusNotificacaoGateway status, BigDecimal valor) {

    public NotificacaoGateway {
        if (idExterno == null || idExterno.isBlank()) {
            throw new IllegalArgumentException("O id externo da notificação é obrigatório.");
        }
        if (status == null) {
            throw new IllegalArgumentException("O status da notificação é obrigatório.");
        }
        if (valor == null || valor.signum() < 0) {
            throw new IllegalArgumentException("O valor da notificação não pode ser nulo nem negativo.");
        }
    }
}
