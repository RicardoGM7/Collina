package br.ufs.dcomp.collina.dto.financeiro;

import br.ufs.dcomp.collina.model.financeiro.FormaPagamentoEnum;
import jakarta.validation.constraints.NotNull;

/** Pedido para pagar uma mensalidade pelo gateway. Só PIX e CARTAO são aceitos (regra no service). */
public record IniciarCobrancaRequest(
    @NotNull(message = "A forma de pagamento é obrigatória.")
    FormaPagamentoEnum formaPagamento
) {}
