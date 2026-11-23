package br.ufs.dcomp.collina.service.financeiro;

import java.time.YearMonth;

import br.ufs.dcomp.collina.model.associados.Associado;

public interface AberturaMensalidadePort {
    // Idempotente: não faz nada se o titular já tem mensalidade EM_ABERTO
    void abrirMensalidadeInicial(Associado titular, YearMonth competencia);

    default void abrirMensalidadeInicial(Associado titular) {
        abrirMensalidadeInicial(titular, YearMonth.now());
    }
}
