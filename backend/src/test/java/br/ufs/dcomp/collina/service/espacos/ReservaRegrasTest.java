package br.ufs.dcomp.collina.service.espacos;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReservaRegrasTest {

    private static final LocalDate HOJE = LocalDate.parse("2026-11-19");

    @Test
    void antecedencia_hojeAteSetteDiasOk_passadoEOitoDiasRejeitados() {
        assertDoesNotThrow(() -> ReservaRegras.validarAntecedencia(HOJE, HOJE, 7));
        assertDoesNotThrow(() -> ReservaRegras.validarAntecedencia(LocalDate.parse("2026-11-26"), HOJE, 7));
        assertThrows(IllegalStateException.class,
                () -> ReservaRegras.validarAntecedencia(LocalDate.parse("2026-11-27"), HOJE, 7));
        assertThrows(IllegalStateException.class,
                () -> ReservaRegras.validarAntecedencia(LocalDate.parse("2026-11-18"), HOJE, 7));
    }

    @Test
    void antecedencia_respeitaOParametroConfiguravel() {
        assertDoesNotThrow(() -> ReservaRegras.validarAntecedencia(LocalDate.parse("2026-11-29"), HOJE, 10));
        assertThrows(IllegalStateException.class,
                () -> ReservaRegras.validarAntecedencia(LocalDate.parse("2026-11-30"), HOJE, 10));
    }

    @Test
    void cancelamento_exigeAoMenosUmDiaDeAntecedencia() {
        assertDoesNotThrow(() -> ReservaRegras.validarPrazoCancelamento(LocalDate.parse("2026-11-20"), HOJE));
        assertThrows(IllegalStateException.class, () -> ReservaRegras.validarPrazoCancelamento(HOJE, HOJE));
        assertThrows(IllegalStateException.class,
                () -> ReservaRegras.validarPrazoCancelamento(LocalDate.parse("2026-11-18"), HOJE));
    }

    @Test
    void dependente_deixaDeSerElegivelNoDiaEmQueCompleta21Anos() {
        assertTrue(ReservaRegras.dependenteElegivel(20, 21));
        assertFalse(ReservaRegras.dependenteElegivel(21, 21));
        assertFalse(ReservaRegras.dependenteElegivel(30, 21));
    }

    @Test
    void bloco_precisaTerExatamenteDuasHoras() {
        assertDoesNotThrow(() -> ReservaRegras.validarDuracaoBloco(LocalTime.of(8, 0), LocalTime.of(10, 0)));
        assertThrows(IllegalStateException.class,
                () -> ReservaRegras.validarDuracaoBloco(LocalTime.of(8, 0), LocalTime.of(9, 0)));
        assertThrows(IllegalStateException.class,
                () -> ReservaRegras.validarDuracaoBloco(LocalTime.of(8, 0), LocalTime.of(11, 0)));
        assertThrows(IllegalStateException.class,
                () -> ReservaRegras.validarDuracaoBloco(LocalTime.of(10, 0), LocalTime.of(8, 0)));
    }

    @Test
    void blocos_sobrepoemSomenteQuandoOsIntervalosSeCruzam() {
        LocalTime i = LocalTime.of(8, 0);
        LocalTime f = LocalTime.of(10, 0);
        assertTrue(ReservaRegras.blocosSeSobrepoem(i, f, LocalTime.of(9, 0), LocalTime.of(11, 0)));
        assertFalse(ReservaRegras.blocosSeSobrepoem(i, f, LocalTime.of(10, 0), LocalTime.of(12, 0)));
    }

    @Test
    void espaco_capacidadeObrigatoriaSeNaoPublico_eConvidadosNaoExcedemCapacidade() {
        assertDoesNotThrow(() -> ReservaRegras.validarEspaco(false, 4, 10));
        assertThrows(IllegalArgumentException.class, () -> ReservaRegras.validarEspaco(false, 4, null));
        assertThrows(IllegalArgumentException.class, () -> ReservaRegras.validarEspaco(false, 11, 10));
        assertThrows(IllegalArgumentException.class, () -> ReservaRegras.validarEspaco(false, -1, 10));
        // público: sem controle de ocupação
        assertDoesNotThrow(() -> ReservaRegras.validarEspaco(true, 50, null));
    }

    @Test
    void ocupacao_titularMaisDependenteMaisConvidadosNaoExcedeCapacidade() {
        assertDoesNotThrow(() -> ReservaRegras.validarOcupacao(false, 10, true, 8)); // 1+1+8 = 10
        assertThrows(IllegalStateException.class, () -> ReservaRegras.validarOcupacao(false, 10, true, 9)); // 11
        assertDoesNotThrow(() -> ReservaRegras.validarOcupacao(false, 10, false, 9)); // 10
        assertDoesNotThrow(() -> ReservaRegras.validarOcupacao(true, null, true, 999)); // público
    }

    @Test
    void cota_somaReservasExistentesMaisBlocosSolicitados() {
        assertDoesNotThrow(() -> ReservaRegras.validarCota(2, 2, 4));
        assertThrows(IllegalStateException.class, () -> ReservaRegras.validarCota(3, 2, 4));
        assertThrows(IllegalStateException.class, () -> ReservaRegras.validarCota(0, 1, null));
    }

    @Test
    void convidados_acimaDoLimiteDoEspacoERejeitado() {
        assertDoesNotThrow(() -> ReservaRegras.validarConvidados(5, 5));
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> ReservaRegras.validarConvidados(5, 6));
        assertTrue(e.getMessage().contains("5"));
    }

    @Test
    void fuso_hojeEmBrasiliaNaoEODiaSeguinteEmUtc() {
        // 01:00 UTC de 20/11 ainda é 22:00 de 19/11 em Brasília
        java.time.Clock c = java.time.Clock.fixed(java.time.Instant.parse("2026-11-20T01:00:00Z"), ReservaRegras.FUSO);
        assertEquals(LocalDate.parse("2026-11-19"), LocalDate.now(c));
    }
}
