package br.ufs.dcomp.collina.service.financeiro.integracao;

import java.time.YearMonth;
import java.util.List;

/**
 * Contrato exigido pelo financeiro para o fechamento mensal: quais titulares
 * devem ser cobrados e por quanto.
 *
 * Regras do contrato:
 * - Nunca devolve {@code null}; sem titulares, devolve lista vazia.
 * - Devolve só titulares (dependentes não recebem mensalidade própria).
 * - O valorBase é o do plano vigente no momento da chamada.
 * - {@code IllegalArgumentException} (400) se a competência for nula.
 */
public interface ConsultaTitularesElegiveis {

    List<TitularElegivel> listarElegiveis(YearMonth competencia);
}
