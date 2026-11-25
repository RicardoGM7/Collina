package br.ufs.dcomp.collina.dto.financeiro;

import br.ufs.dcomp.collina.model.financeiro.TipoMovimentacaoEnum;
import br.ufs.dcomp.collina.model.financeiro.OrigemMovimentacaoEnum;
import br.ufs.dcomp.collina.model.financeiro.SituacaoMovimentacaoEnum;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record FiltroMovimentacaoRequestDTO (
        TipoMovimentacaoEnum tipo,
        OrigemMovimentacaoEnum origem,
        SituacaoMovimentacaoEnum situacao,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dataInicio,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dataFim
) {}