package br.ufs.dcomp.collina.dto.financeiro;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import br.ufs.dcomp.collina.model.financeiro.FormaPagamentoEnum;
import br.ufs.dcomp.collina.model.financeiro.Mensalidade;
import br.ufs.dcomp.collina.model.financeiro.Pagamento;
import br.ufs.dcomp.collina.model.financeiro.StatusTransacaoEnum;

public record PagamentoResponseDTO(
    Long mensalidadeId,
    Long id,
    BigDecimal valor,
    LocalDateTime dataPagamento,
    FormaPagamentoEnum formaPagamento,
    StatusTransacaoEnum status,
    String getwayId
) {
    public PagamentoResponseDTO(Pagamento dados){
        this(
            dados.getMensalidade().getId(),
            dados.getId(),
            dados.getValor(),
            dados.getDataPagamento(),
            dados.getFormaPagamento(),
            dados.getStatus(),
            dados.getIdExternoGateway()
        );
    }
}
