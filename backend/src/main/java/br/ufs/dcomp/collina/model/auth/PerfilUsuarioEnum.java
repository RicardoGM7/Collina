package br.ufs.dcomp.collina.model.auth;

import java.util.Optional;

// Perfis de acesso do sistema (armazenados em tb_usuario).
public enum PerfilUsuarioEnum {
    ADMIN,          // Administrador
    RECEPCAO,       // Funcionário de Recepção
    FINANCEIRO,     // Funcionário Financeiro
    ESTOQUE,        // Funcionário de Estoque
    ESCALA_RH,      // Funcionário de Escala/RH
    ASSOCIADO;      // Associado titular (Proprietário ou Contribuinte)

    // Claim legada usada apenas na migração automática de contas antigas do Firebase para tb_usuario
    public static final String CLAIM = "perfil";

    // Authority usada no Spring Security: hasRole("ADMIN") <-> "ROLE_ADMIN"
    public String authority() {
        return "ROLE_" + name();
    }

    // Converte o valor vindo de uma claim (pode ser null ou texto inválido) para o enum
    public static Optional<PerfilUsuarioEnum> deClaim(Object valor) {
        if (valor == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(PerfilUsuarioEnum.valueOf(valor.toString()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
