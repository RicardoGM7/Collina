package br.ufs.dcomp.collina.service.espacos;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/**
 * Regras de negócio do módulo de espaços e reservas, sem banco nem Spring (fáceis de testar).
 * Convenção do projeto: dado inválido -> IllegalArgumentException (400);
 * regra de negócio violada -> IllegalStateException (409), com a mensagem que explica o motivo.
 */
public final class ReservaRegras {

    // "Hoje" do clube é sempre o de Brasília, independentemente do fuso do servidor
    public static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    // RF-ESP02: o bloco é a unidade indivisível de 2 horas
    public static final Duration DURACAO_BLOCO = Duration.ofHours(2);

    // UC-GE02 / INC09: prazo padrão (configurável) de antecedência da reserva
    public static final int MAX_DIAS_ANTECEDENCIA_PADRAO = 7;

    private ReservaRegras() {
    }

    // ----------------------------------------------------------------- espaço

    // RF-ESP01 / RD-04
    public static void validarEspaco(boolean publico, Integer limiteConvidados, Integer capacidadeMaxima) {
        if (limiteConvidados == null || limiteConvidados < 0) {
            throw new IllegalArgumentException("O limite de convidados deve ser um número maior ou igual a zero.");
        }
        if (publico) {
            return; // livre acesso: sem controle de ocupação
        }
        if (capacidadeMaxima == null || capacidadeMaxima <= 0) {
            throw new IllegalArgumentException(
                    "A capacidade máxima (AVCB) é obrigatória para espaços que não são públicos.");
        }
        if (limiteConvidados > capacidadeMaxima) {
            throw new IllegalArgumentException(
                    "O limite de convidados não pode exceder a capacidade máxima do espaço.");
        }
    }

    // ------------------------------------------------------------------ bloco

    // UC-GE05: duração exata de 2 horas
    public static void validarDuracaoBloco(LocalTime inicio, LocalTime fim) {
        if (!fim.isAfter(inicio) || !Duration.between(inicio, fim).equals(DURACAO_BLOCO)) {
            throw new IllegalStateException(
                    "O bloco deve ter exatamente 2 horas de duração (unidade indivisível).");
        }
    }

    public static boolean blocosSeSobrepoem(LocalTime inicioA, LocalTime fimA, LocalTime inicioB, LocalTime fimB) {
        return inicioA.isBefore(fimB) && inicioB.isBefore(fimA);
    }

    // ---------------------------------------------------------------- reserva

    // UC-GE02: de hoje até N dias à frente; nunca no passado
    public static void validarAntecedencia(LocalDate dataUso, LocalDate hoje, int maxDias) {
        long dias = ChronoUnit.DAYS.between(hoje, dataUso);
        if (dias < 0) {
            throw new IllegalStateException("Não é possível reservar para uma data passada.");
        }
        if (dias > maxDias) {
            throw new IllegalStateException(
                    "A reserva só pode ser feita com até " + maxDias + " dias de antecedência.");
        }
    }

    // RF-AS09 / UC-GE03: cancelamento até o dia anterior à data agendada
    public static void validarPrazoCancelamento(LocalDate dataUso, LocalDate hoje) {
        if (ChronoUnit.DAYS.between(hoje, dataUso) < 1) {
            throw new IllegalStateException(
                    "Prazo de cancelamento expirado: só é possível cancelar até o dia anterior à data agendada.");
        }
    }

    // RF-AS10
    public static void validarConvidados(int limiteConvidados, int qtdConvidados) {
        if (qtdConvidados > limiteConvidados) {
            throw new IllegalStateException(
                    "A quantidade de convidados excede o limite do espaço (máximo " + limiteConvidados + ").");
        }
    }

    // RF-AS08 / RD-04: titular + dependente + convidados <= capacidade (apenas espaços controlados)
    public static void validarOcupacao(boolean publico, Integer capacidadeMaxima,
                                       boolean comDependente, int qtdConvidados) {
        if (publico || capacidadeMaxima == null) {
            return;
        }
        int total = 1 + (comDependente ? 1 : 0) + qtdConvidados;
        if (total > capacidadeMaxima) {
            throw new IllegalStateException("A ocupação prevista (" + total
                    + ") excede a capacidade máxima do espaço (" + capacidadeMaxima + ").");
        }
    }

    // RF-AS02: cada bloco reservado conta 1 na cota mensal do plano
    public static void validarCota(long jaReservadas, int solicitadas, Integer limiteMensal) {
        if (limiteMensal == null) {
            throw new IllegalStateException("O associado não possui plano com limite de reservas definido.");
        }
        if (jaReservadas + solicitadas > limiteMensal) {
            throw new IllegalStateException("Limite mensal de reservas do plano excedido (limite " + limiteMensal
                    + ", já utilizadas " + jaReservadas + ", solicitadas " + solicitadas + ").");
        }
    }

    // RF-AS04: elegibilidade avaliada na DATA DE USO, não na data da solicitação
    public static boolean dependenteElegivel(int idadeNaDataDeUso, int idadeLimite) {
        return idadeNaDataDeUso < idadeLimite;
    }
}
