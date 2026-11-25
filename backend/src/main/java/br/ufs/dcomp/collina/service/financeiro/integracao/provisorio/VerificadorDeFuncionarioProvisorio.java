package br.ufs.dcomp.collina.service.financeiro.integracao.provisorio;

import org.springframework.stereotype.Component;

import br.ufs.dcomp.collina.service.financeiro.integracao.VerificadorDeFuncionario;

/**
 * PROVISÓRIA. Só exige que o id tenha sido informado; não confere existência
 * nem permissão. REMOVER no mesmo PR que trouxer a implementação real.
 */
@Component
public class VerificadorDeFuncionarioProvisorio implements VerificadorDeFuncionario {

    @Override
    public void exigirFuncionarioHabilitado(Long funcionarioId) {
        if (funcionarioId == null) {
            throw new IllegalArgumentException("O id do funcionário é obrigatório.");
        }
    }
}
