package br.ufs.dcomp.collina.service.financeiro.integracao;

/**
 * Contrato exigido pelo financeiro ao módulo de funcionários.
 *
 * Garante que o funcionário informado como responsável por uma operação
 * (cancelamento, gasto avulso, confirmação manual de pagamento) existe e
 * está habilitado a realizá-la. Existe enquanto o funcionarioId vem no corpo
 * da requisição; com Firebase Auth, o id virá do usuário autenticado.
 *
 * Exceções esperadas:
 * - {@code IllegalArgumentException} (400): funcionarioId nulo.
 * - {@code ResourceNotFoundException} (404): funcionário inexistente.
 * - {@code IllegalStateException} (409): funcionário inativo ou sem permissão.
 */
public interface VerificadorDeFuncionario {

    void exigirFuncionarioHabilitado(Long funcionarioId);
}
