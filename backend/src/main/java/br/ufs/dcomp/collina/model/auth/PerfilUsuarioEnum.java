package br.ufs.dcomp.collina.model.auth;

import java.util.Optional;

// Perfis de acesso do sistema (Requisitos.docx, RF-AUTO01). O perfil do usuário fica no Firebase, como custom claim "perfil".
public enum PerfilUsuarioEnum {
    ADMIN,          // Administrador
    RECEPCAO,       // Funcionário de Recepção
    FINANCEIRO,     // Funcionário Financeiro
    ESTOQUE,        // Funcionário de Estoque
    ESCALA_RH,      // Funcionário de Escala/RH
    ASSOCIADO;      // Associado titular (Proprietário ou Contribuinte)

    // Nome da claim gravada no token do Firebase (o front lê a mesma chave)
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
