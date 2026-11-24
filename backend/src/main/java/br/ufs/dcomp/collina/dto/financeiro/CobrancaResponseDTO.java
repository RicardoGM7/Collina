package br.ufs.dcomp.collina.dto.financeiro;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import br.ufs.dcomp.collina.model.financeiro.FormaPagamentoEnum;
import br.ufs.dcomp.collina.model.financeiro.Pagamento;
import br.ufs.dcomp.collina.model.financeiro.StatusTransacaoEnum;

/** O que o pagador precisa para pagar: código Pix (copia e cola) ou link do checkout, e até quando. */
public record CobrancaResponseDTO(
    Long pagamentoId,
    Long mensalidadeId,
    FormaPagamentoEnum formaPagamento,
    BigDecimal valor,
    StatusTransacaoEnum status,
    String idExterno,
    String urlPagamento,
    String codigoPix,
    LocalDateTime expiraEm
) {
    public CobrancaResponseDTO(Pagamento pagamento) {
        this(
            pagamento.getId(),
            pagamento.getMensalidade() != null ? pagamento.getMensalidade().getId() : null,
            pagamento.getFormaPagamento(),
            pagamento.getValor(),
            pagamento.getStatus(),
            pagamento.getIdExternoGateway(),
            pagamento.getUrlPagamento(),
            pagamento.getCodigoPix(),
            pagamento.getExpiraEm()
        );
    }
}
