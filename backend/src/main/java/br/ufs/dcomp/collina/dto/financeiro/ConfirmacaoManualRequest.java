package br.ufs.dcomp.collina.dto.financeiro;

import java.math.BigDecimal;

import br.ufs.dcomp.collina.model.financeiro.FormaPagamentoEnum;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ConfirmacaoManualRequest(
    
    @NotNull(message= "O valor não pode ser nulo")
    @Digits(integer = 13, fraction = 2)
    @PositiveOrZero(message= "O valor do pagamento deve ser positivo")
    BigDecimal valor,

    @NotNull(message= "A forma de pagamento é obrigatoria")
    FormaPagamentoEnum formaPagamento,

    String idGateway

) {
    public ConfirmacaoManualRequest(BigDecimal valor, FormaPagamentoEnum formaPagamento) {
        this(valor, formaPagamento, null);
    }
}
