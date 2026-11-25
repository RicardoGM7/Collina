package br.ufs.dcomp.collina.service.espacos;

/**
 * Porta entre o módulo de reservas e o Financeiro (RF-AS07: associado inadimplente não reserva).
 *
 * O módulo Financeiro ainda não está no repositório. Quando existir, basta criar um bean que
 * implemente esta interface (ex.: MensalidadeService) anotado com @Primary, e apagar a
 * AdimplenciaPadrao. O ReservaService não muda.
 */
public interface ConsultaAdimplencia {

    /** @return true se o associado titular tem mensalidade em atraso. */
    boolean possuiMensalidadeEmAtraso(Integer associadoId);
}
