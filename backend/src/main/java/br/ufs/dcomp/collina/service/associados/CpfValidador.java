package br.ufs.dcomp.collina.service.associados;

// Validação de formato do CPF compartilhada pelos services do módulo de associados
final class CpfValidador {

    private CpfValidador() {
    }

    static String validar(String cpf) {
        if (cpf == null || !cpf.trim().matches("\\d{11}")) {
            throw new IllegalArgumentException("O CPF deve conter exatamente 11 dígitos numéricos.");
        }
        return cpf.trim();
    }
}
