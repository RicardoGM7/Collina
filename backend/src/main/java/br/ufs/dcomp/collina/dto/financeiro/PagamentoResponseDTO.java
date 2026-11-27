package br.ufs.dcomp.collina.dto.financeiro;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import br.ufs.dcomp.collina.model.financeiro.FormaPagamentoEnum;
import br.ufs.dcomp.collina.model.financeiro.Pagamento;
import br.ufs.dcomp.collina.model.financeiro.StatusTransacaoEnum;

public record PagamentoResponseDTO(
    Long mensalidadeId,
    Long id,
    String tipoGasto,
    BigDecimal valor,
    LocalDateTime dataPagamento,
    FormaPagamentoEnum formaPagamento,
    StatusTransacaoEnum status,
    String comprovante,
    String gatewayId
) {
    public PagamentoResponseDTO(Pagamento dados){
        this(
            dados.getMensalidade() != null ? dados.getMensalidade().getId() : null,
            dados.getId(),
            "MENSALIDADE",
            dados.getValor(),
            dados.getDataPagamento(),
            dados.getFormaPagamento(),
            dados.getStatus(),
            dados.getComprovante(),
            dados.getIdExternoGateway()
        );
    }

    public PagamentoResponseDTO(
            Long mensalidadeId,
            Long id,
            BigDecimal valor,
            LocalDateTime dataPagamento,
            FormaPagamentoEnum formaPagamento,
            StatusTransacaoEnum status,
            String getwayId) {
        this(
            mensalidadeId,
            id,
            "MENSALIDADE",
            valor,
            dataPagamento,
            formaPagamento,
            status,
            null,
            getwayId
        );
    }
}
