package br.ufs.dcomp.collina.dto.financeiro;

import br.ufs.dcomp.collina.model.financeiro.SaldoClube;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SaldoClubeResponseDTO (
            BigDecimal valor,
            LocalDateTime dataAtualizacao
)  {
    public SaldoClubeResponseDTO(SaldoClube saldo) {
        this(
            saldo.getValor(),
            saldo.getDataAtualizacao()
        );
    }
}
