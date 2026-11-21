package br.ufs.dcomp.collina.dto.associados;

import br.ufs.dcomp.collina.model.associados.Plano;
import br.ufs.dcomp.collina.model.associados.TipoPlanoEnum;

import java.math.BigDecimal;

public record PlanoResponseDTO(
        Integer id,
        TipoPlanoEnum tipo,
        BigDecimal valorPeriodico,
        Integer limiteMensalReservas,
        Integer maxDependentesAtivos) {

    public PlanoResponseDTO(Plano plano) {
        this(
                plano.getId(),
                plano.getTipo(),
                plano.getMensalidade(),
                plano.getLimiteMensalReservas(),
                plano.getMaxDependentesAtivos()
        );
    }
}
