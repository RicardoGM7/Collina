package br.ufs.dcomp.collina.service.financeiro.integracao;

import java.time.LocalDateTime;

/**
 * Resultado da criação de uma cobrança no gateway.
 *
 * @param idExterno     identificador da cobrança no gateway (vai para Pagamento.idExternoGateway)
 * @param urlPagamento  link do checkout (cartão); nulo no Pix
 * @param codigoPix     código "copia e cola" do Pix; nulo no cartão
 * @param expiraEm      até quando a cobrança pode ser paga
 */
public record CobrancaGateway(String idExterno, String urlPagamento, String codigoPix, LocalDateTime expiraEm) {

    public CobrancaGateway {
        if (idExterno == null || idExterno.isBlank()) {
            throw new IllegalArgumentException("O id externo da cobrança é obrigatório.");
        }
        if (expiraEm == null) {
            throw new IllegalArgumentException("A validade da cobrança é obrigatória.");
        }
    }
}
