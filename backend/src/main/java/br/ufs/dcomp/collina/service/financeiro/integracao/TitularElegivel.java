package br.ufs.dcomp.collina.service.financeiro.integracao;

import java.math.BigDecimal;

/**
 * Titular que deve receber mensalidade na competência, com o valor base do
 * plano vigente. É um snapshot: o financeiro copia o valor para a Mensalidade.
 */
public record TitularElegivel(Long associadoId, BigDecimal valorBase) {

    public TitularElegivel {
        if (associadoId == null) {
            throw new IllegalArgumentException("O id do associado é obrigatório.");
        }
        if (valorBase == null || valorBase.signum() < 0) {
            throw new IllegalArgumentException("O valor base não pode ser nulo nem negativo.");
        }
    }
}
