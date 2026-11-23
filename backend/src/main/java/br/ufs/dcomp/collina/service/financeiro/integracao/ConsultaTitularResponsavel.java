package br.ufs.dcomp.collina.service.financeiro.integracao;

/**
 * Contrato exigido pelo financeiro ao módulo de associados.
 *
 * Pergunta única: dado o id de um associado que gerou um gasto, quem é o
 * titular responsável por pagá-lo?
 * - Se o associado é titular, a resposta é o próprio id.
 * - Se é dependente, a resposta é o titular do vínculo de dependência.
 *
 * Exceções esperadas (todas já mapeadas pelo GlobalExceptionHandler):
 * - {@code IllegalArgumentException} (400): associadoId nulo.
 * - {@code ResourceNotFoundException} (404): associado inexistente.
 * - {@code IllegalStateException} (409): dependente sem vínculo ativo ou
 *   associado inativo.
 */
public interface ConsultaTitularResponsavel {

    Integer buscarIdTitularResponsavel(Integer associadoId);
}
