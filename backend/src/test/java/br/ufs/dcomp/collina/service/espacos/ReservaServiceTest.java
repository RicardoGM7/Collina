package br.ufs.dcomp.collina.service.espacos;

import br.ufs.dcomp.collina.dto.espacos.ReservaRequestDTO;
import br.ufs.dcomp.collina.dto.espacos.ReservaResponseDTO;
import br.ufs.dcomp.collina.exception.ResourceNotFoundException;
import br.ufs.dcomp.collina.model.associados.SituacaoCadastralEnum;
import br.ufs.dcomp.collina.model.associados.StatusVinculoEnum;
import br.ufs.dcomp.collina.model.auth.PerfilUsuarioEnum;
import br.ufs.dcomp.collina.model.espacos.EstadoEspacoEnum;
import br.ufs.dcomp.collina.model.espacos.Reserva;
import br.ufs.dcomp.collina.model.espacos.StatusReservaEnum;
import br.ufs.dcomp.collina.service.espacos.EspacosFakes.Cenario;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// "Hoje" nos testes é 19/11/2026 (EspacosFakes.RELOGIO).
class ReservaServiceTest {

    private static final String UID1 = "uid-1";
    private static final String UID2 = "uid-2";
    private static final String DATA = "2026-11-21";

    private static ReservaRequestDTO req(int espacoId, String data, List<Integer> blocos, Integer convidados, Integer dep) {
        return new ReservaRequestDTO(espacoId, java.time.LocalDate.parse(data), blocos, convidados, dep);
    }

    private static void erro409(Class<? extends RuntimeException> tipo, org.junit.jupiter.api.function.Executable ex, String trecho) {
        RuntimeException e = assertThrows(tipo, ex);
        assertTrue(e.getMessage().contains(trecho), "mensagem: " + e.getMessage());
    }

    // ------------------------------------------------------------ solicitar

    @Test
    void solicitar_reservaUmBloco() {
        Cenario c = new Cenario();
        List<ReservaResponseDTO> r = c.reservaService().solicitar(UID1, req(c.quadra.getId(), DATA, List.of(c.b0810.getId()), 2, null));
        assertEquals(1, r.size());
        assertEquals(StatusReservaEnum.CONFIRMADA, r.get(0).status());
        assertEquals("Quadra 1", r.get(0).espacoNome());
        assertEquals("08:00", r.get(0).horaInicio());
        assertEquals("10:00", r.get(0).horaFim());
        assertEquals(1, c.reservas.dados.size());
        assertEquals(Boolean.TRUE, c.reservas.dados.get(0).getSlotAtivo());
        assertNotNull(c.reservas.dados.get(0).getCriadaEm());
    }

    @Test
    void solicitar_variosBlocosOrdenadosPorHorario() {
        Cenario c = new Cenario();
        List<ReservaResponseDTO> r = c.reservaService().solicitar(UID1,
                req(c.quadra.getId(), DATA, List.of(c.b1012.getId(), c.b0810.getId()), 0, null));
        assertEquals(List.of("08:00", "10:00"), r.stream().map(ReservaResponseDTO::horaInicio).toList());
    }

    @Test
    void solicitar_usuarioSemCadastroDeAssociado_404() {
        Cenario c = new Cenario();
        assertThrows(ResourceNotFoundException.class, () ->
                c.reservaService().solicitar("uid-desconhecido", req(c.quadra.getId(), DATA, List.of(c.b0810.getId()), 0, null)));
    }

    @Test
    void solicitar_blocosRepetidos_400() {
        Cenario c = new Cenario();
        assertThrows(IllegalArgumentException.class, () ->
                c.reservaService().solicitar(UID1, req(c.quadra.getId(), DATA, List.of(c.b0810.getId(), c.b0810.getId()), 0, null)));
    }

    @Test
    void solicitar_antecedenciaMaximaDeSeteDiasEDataPassada() {
        Cenario c = new Cenario();
        ReservaService s = c.reservaService();
        Integer q = c.quadra.getId();
        Integer b = c.b0810.getId();
        erro409(IllegalStateException.class, () -> s.solicitar(UID1, req(q, "2026-11-27", List.of(b), 0, null)), "7 dias");
        erro409(IllegalStateException.class, () -> s.solicitar(UID1, req(q, "2026-11-18", List.of(b), 0, null)), "passada");
        s.solicitar(UID1, req(q, "2026-11-26", List.of(b), 0, null)); // exatamente 7 dias
    }

    @Test
    void solicitar_horarioJaReservadoPorOutroTitular() {
        Cenario c = new Cenario();
        ReservaService s = c.reservaService();
        s.solicitar(UID2, req(c.quadra.getId(), DATA, List.of(c.b0810.getId()), 0, null));
        erro409(IllegalStateException.class,
                () -> s.solicitar(UID1, req(c.quadra.getId(), DATA, List.of(c.b0810.getId()), 0, null)),
                "não estão mais disponíveis");
    }

    @Test
    void solicitar_seUmBlocoEstaOcupadoNenhumaReservaEGravada() {
        Cenario c = new Cenario();
        ReservaService s = c.reservaService();
        s.solicitar(UID2, req(c.quadra.getId(), DATA, List.of(c.b1012.getId()), 0, null));
        int antes = c.reservas.dados.size();
        assertThrows(IllegalStateException.class, () ->
                s.solicitar(UID1, req(c.quadra.getId(), DATA, List.of(c.b0810.getId(), c.b1012.getId()), 0, null)));
        assertEquals(antes, c.reservas.dados.size());
    }

    @Test
    void solicitar_restricaoUnicaDoBancoVira409_naCorrida() {
        Cenario c = new Cenario();
        // reserva já existente que a checagem da aplicação "não enxerga" (outra transação simultânea)
        c.reservaService().solicitar(UID2, req(c.quadra.getId(), DATA, List.of(c.b0810.getId()), 0, null));
        EspacosFakes.ReservaRepoFake cega = new EspacosFakes.ReservaRepoFake() {
            @Override
            public List<Reserva> findByEspacoIdAndDataUsoAndStatusAndBlocoIdIn(
                    Integer e, java.time.LocalDate d, StatusReservaEnum s, Collection<Integer> ids) {
                return List.of();
            }
        };
        cega.dados.addAll(c.reservas.dados);
        ReservaService s = new ReservaService(c.associadoService(), c.associados, c.vinculoRepo(), c.espacos,
                c.blocos, cega, id -> false, EspacosFakes.RELOGIO, 7);
        erro409(IllegalStateException.class,
                () -> s.solicitar(UID1, req(c.quadra.getId(), DATA, List.of(c.b0810.getId()), 0, null)),
                "não estão mais disponíveis");
    }

    @Test
    void solicitar_titularSuspensoOuInativoNaoReserva() {
        Cenario c = new Cenario();
        ReservaService s = c.reservaService();
        ReservaRequestDTO dto = req(c.quadra.getId(), DATA, List.of(c.b0810.getId()), 0, null);
        c.titular(1).setSituacaoCadastral(SituacaoCadastralEnum.SUSPENSO);
        erro409(IllegalStateException.class, () -> s.solicitar(UID1, dto), "suspenso");
        c.titular(1).setSituacaoCadastral(SituacaoCadastralEnum.INATIVO);
        erro409(IllegalStateException.class, () -> s.solicitar(UID1, dto), "inativo");
    }

    @Test
    void solicitar_inadimplenteNaoReserva_RFAS07() {
        Cenario c = new Cenario();
        c.inadimplentes.add(1);
        erro409(IllegalStateException.class,
                () -> c.reservaService().solicitar(UID1, req(c.quadra.getId(), DATA, List.of(c.b0810.getId()), 0, null)),
                "inadimpl");
        // o outro titular não é afetado
        c.reservaService().solicitar(UID2, req(c.quadra.getId(), DATA, List.of(c.b0810.getId()), 0, null));
    }

    @Test
    void solicitar_espacoEmManutencaoInativoOuExclusivoDeTreino() {
        Cenario c = new Cenario();
        ReservaService s = c.reservaService();
        ReservaRequestDTO dto = req(c.quadra.getId(), DATA, List.of(c.b0810.getId()), 0, null);
        c.estado(EstadoEspacoEnum.EM_MANUTENCAO);
        erro409(IllegalStateException.class, () -> s.solicitar(UID1, dto), "manuten");
        c.estado(EstadoEspacoEnum.INATIVO);
        assertThrows(IllegalStateException.class, () -> s.solicitar(UID1, dto));
        c.estado(EstadoEspacoEnum.ATIVO);
        c.quadra.setUsoExclusivoTreino(true);
        erro409(IllegalStateException.class, () -> s.solicitar(UID1, dto), "treino");
    }

    @Test
    void solicitar_blocoDeTreinoManutencaoOuDeOutroEspaco() {
        Cenario c = new Cenario();
        ReservaService s = c.reservaService();
        erro409(IllegalStateException.class,
                () -> s.solicitar(UID1, req(c.quadra.getId(), DATA, List.of(c.bTreino.getId()), 0, null)), "treino ou manuten");
        erro409(IllegalStateException.class,
                () -> s.solicitar(UID1, req(c.quadra.getId(), DATA, List.of(c.bManutencao.getId()), 0, null)), "treino ou manuten");
        // bloco da quadra pedido para a piscina
        assertThrows(ResourceNotFoundException.class,
                () -> s.solicitar(UID1, req(c.piscina.getId(), DATA, List.of(c.b1416.getId()), 0, null)));
    }

    @Test
    void solicitar_convidadosAcimaDoLimiteDoEspaco() {
        Cenario c = new Cenario();
        erro409(IllegalStateException.class,
                () -> c.reservaService().solicitar(UID1, req(c.quadra.getId(), DATA, List.of(c.b0810.getId()), 6, null)),
                "convidados");
    }

    @Test
    void solicitar_ocupacaoTotalAcimaDaCapacidadeComDependente() {
        Cenario c = new Cenario();
        c.quadra.setLimiteConvidados(9);
        ReservaService s = c.reservaService();
        // 1 titular + 1 dependente + 8 convidados = 10 -> ok
        s.solicitar(UID1, req(c.quadra.getId(), DATA, List.of(c.b0810.getId()), 8, 100));
        // 1 + 1 + 9 = 11 -> excede
        erro409(IllegalStateException.class,
                () -> s.solicitar(UID1, req(c.quadra.getId(), DATA, List.of(c.b1012.getId()), 9, 100)), "capacidade");
    }

    @Test
    void solicitar_espacoPublicoNaoControlaOcupacaoMasRespeitaLimiteDeConvidados() {
        Cenario c = new Cenario();
        ReservaService s = c.reservaService();
        s.solicitar(UID1, req(c.piscina.getId(), DATA, List.of(c.bPiscina.getId()), 20, null));
        erro409(IllegalStateException.class,
                () -> s.solicitar(UID1, req(c.piscina.getId(), "2026-11-22", List.of(c.bPiscina.getId()), 21, null)),
                "convidados");
    }

    // ------------------------------------------------------------------ cota

    @Test
    void cota_somaReservasExistentesMaisBlocosPedidos_limite4() {
        Cenario c = new Cenario();
        ReservaService s = c.reservaService();
        Integer q = c.quadra.getId();
        s.solicitar(UID1, req(q, DATA, List.of(c.b0810.getId(), c.b1012.getId(), c.b1416.getId()), 0, null)); // 3 de 4
        erro409(IllegalStateException.class,
                () -> s.solicitar(UID1, req(q, "2026-11-22", List.of(c.b0810.getId(), c.b1012.getId()), 0, null)), "Limite mensal");
        s.solicitar(UID1, req(q, "2026-11-22", List.of(c.b0810.getId()), 0, null)); // 4 de 4
        erro409(IllegalStateException.class,
                () -> s.solicitar(UID1, req(q, "2026-11-23", List.of(c.b0810.getId()), 0, null)), "Limite mensal");
    }

    @Test
    void cota_reservaCanceladaDeixaDeContar() {
        Cenario c = new Cenario();
        ReservaService s = c.reservaService();
        Integer q = c.quadra.getId();
        List<ReservaResponseDTO> r = s.solicitar(UID1, req(q, DATA, List.of(c.b0810.getId(), c.b1012.getId(), c.b1416.getId()), 0, null));
        s.cancelar(PerfilUsuarioEnum.ASSOCIADO, UID1, r.get(0).id());
        s.solicitar(UID1, req(q, "2026-11-22", List.of(c.b0810.getId(), c.b1012.getId()), 0, null)); // 2 + 2 = 4
    }

    @Test
    void cota_eContadaPeloMesDaDataDeUso() {
        Cenario c = new Cenario();
        Clock fimDoMes = Clock.fixed(Instant.parse("2026-11-27T15:00:00Z"), ReservaRegras.FUSO);
        ReservaService s = new ReservaService(c.associadoService(), c.associados, c.vinculoRepo(), c.espacos,
                c.blocos, c.reservas, id -> false, fimDoMes, 7);
        Integer q = c.quadra.getId();
        List<Integer> tres = List.of(c.b0810.getId(), c.b1012.getId(), c.b1416.getId());
        s.solicitar(UID1, req(q, "2026-11-30", tres, 0, null));
        s.solicitar(UID1, req(q, "2026-12-01", tres, 0, null)); // mês seguinte: cota própria
    }

    // ------------------------------------------------------------ dependente

    @Test
    void dependente_precisaSerDoTitular_estarAtivo_eTerMenosDe21AnosNaDataDeUso() {
        Cenario c = new Cenario();
        ReservaService s = c.reservaService();
        Integer q = c.quadra.getId();
        Integer b = c.b0810.getId();
        erro409(IllegalStateException.class, () -> s.solicitar(UID1, req(q, DATA, List.of(b), 0, 102)), "não está vinculado");
        erro409(IllegalStateException.class, () -> s.solicitar(UID1, req(q, DATA, List.of(b), 0, 999)), "não está vinculado");
        erro409(IllegalStateException.class, () -> s.solicitar(UID1, req(q, DATA, List.of(b), 0, 103)), "inativo");
        erro409(IllegalStateException.class, () -> s.solicitar(UID1, req(q, DATA, List.of(b), 0, 101)), "21 anos");
        ReservaResponseDTO ok = s.solicitar(UID1, req(q, DATA, List.of(b), 0, 100)).get(0);
        assertEquals(100, ok.dependenteId());
        assertEquals("Dependente 100", ok.dependenteNome());
    }

    @Test
    void dependente_elegibilidadeEAvaliadaNaDataDeUso() {
        Cenario c = new Cenario();
        // completa 21 anos em 22/11/2026: elegível em 21/11, inelegível em 22/11
        c.novoVinculo(110, 1, "2005-11-22", StatusVinculoEnum.ATIVO);
        ReservaService s = c.reservaService();
        s.solicitar(UID1, req(c.quadra.getId(), "2026-11-21", List.of(c.b0810.getId()), 0, 110));
        erro409(IllegalStateException.class,
                () -> s.solicitar(UID1, req(c.quadra.getId(), "2026-11-22", List.of(c.b0810.getId()), 0, 110)), "21 anos");
    }

    // -------------------------------------------------------------- cancelar

    @Test
    void cancelar_titularCancelaASuaEOHorarioVoltaALivre() {
        Cenario c = new Cenario();
        ReservaService s = c.reservaService();
        ReservaResponseDTO r = s.solicitar(UID1, req(c.quadra.getId(), DATA, List.of(c.b0810.getId()), 0, null)).get(0);
        ReservaResponseDTO cancelada = s.cancelar(PerfilUsuarioEnum.ASSOCIADO, UID1, r.id());
        assertEquals(StatusReservaEnum.CANCELADA, cancelada.status());
        Reserva gravada = c.reservas.dados.get(0);
        assertNull(gravada.getSlotAtivo());
        assertNotNull(gravada.getCanceladaEm());
        // o mesmo bloco pode ser reservado por outro titular
        s.solicitar(UID2, req(c.quadra.getId(), DATA, List.of(c.b0810.getId()), 0, null));
    }

    @Test
    void cancelar_noDiaDaReservaEBloqueado() {
        Cenario c = new Cenario();
        ReservaService s = c.reservaService();
        ReservaResponseDTO r = s.solicitar(UID1, req(c.quadra.getId(), "2026-11-19", List.of(c.b0810.getId()), 0, null)).get(0);
        erro409(IllegalStateException.class, () -> s.cancelar(PerfilUsuarioEnum.ASSOCIADO, UID1, r.id()), "Prazo");
    }

    @Test
    void cancelar_titularNaoCancelaReservaDeOutro_403() {
        Cenario c = new Cenario();
        ReservaService s = c.reservaService();
        ReservaResponseDTO r = s.solicitar(UID1, req(c.quadra.getId(), DATA, List.of(c.b0810.getId()), 0, null)).get(0);
        assertThrows(AccessDeniedException.class, () -> s.cancelar(PerfilUsuarioEnum.ASSOCIADO, UID2, r.id()));
    }

    @Test
    void cancelar_recepcaoEAdminCancelamEmNomeDoTitular_respeitandoOPrazo() {
        Cenario c = new Cenario();
        ReservaService s = c.reservaService();
        ReservaResponseDTO a = s.solicitar(UID1, req(c.quadra.getId(), DATA, List.of(c.b0810.getId()), 0, null)).get(0);
        ReservaResponseDTO b = s.solicitar(UID1, req(c.quadra.getId(), DATA, List.of(c.b1012.getId()), 0, null)).get(0);
        assertEquals(StatusReservaEnum.CANCELADA, s.cancelar(PerfilUsuarioEnum.RECEPCAO, "uid-recepcao", a.id()).status());
        assertEquals(StatusReservaEnum.CANCELADA, s.cancelar(PerfilUsuarioEnum.ADMIN, "uid-admin", b.id()).status());
        ReservaResponseDTO hoje = s.solicitar(UID1, req(c.quadra.getId(), "2026-11-19", List.of(c.b1416.getId()), 0, null)).get(0);
        erro409(IllegalStateException.class, () -> s.cancelar(PerfilUsuarioEnum.RECEPCAO, "uid-recepcao", hoje.id()), "Prazo");
    }

    @Test
    void cancelar_inexistenteEJaCancelada() {
        Cenario c = new Cenario();
        ReservaService s = c.reservaService();
        assertThrows(ResourceNotFoundException.class, () -> s.cancelar(PerfilUsuarioEnum.RECEPCAO, "x", 99999));
        ReservaResponseDTO r = s.solicitar(UID1, req(c.quadra.getId(), DATA, List.of(c.b0810.getId()), 0, null)).get(0);
        s.cancelar(PerfilUsuarioEnum.ASSOCIADO, UID1, r.id());
        erro409(IllegalStateException.class, () -> s.cancelar(PerfilUsuarioEnum.ASSOCIADO, UID1, r.id()), "já está cancelada");
    }

    // -------------------------------------------------------------- consultar

    @Test
    void listar_minhasSoDoTitular_ordenadasPorDataDecrescente_eIncluemCanceladas() {
        Cenario c = new Cenario();
        ReservaService s = c.reservaService();
        Integer q = c.quadra.getId();
        ReservaResponseDTO antiga = s.solicitar(UID1, req(q, "2026-11-20", List.of(c.b0810.getId()), 0, null)).get(0);
        s.solicitar(UID1, req(q, "2026-11-22", List.of(c.b1012.getId(), c.b0810.getId()), 0, null));
        s.solicitar(UID2, req(q, "2026-11-23", List.of(c.b0810.getId()), 0, null));
        s.cancelar(PerfilUsuarioEnum.ASSOCIADO, UID1, antiga.id());

        List<ReservaResponseDTO> minhas = s.listarMinhas(UID1);
        assertEquals(3, minhas.size());
        assertEquals(java.time.LocalDate.parse("2026-11-22"), minhas.get(0).dataUso());
        assertEquals("08:00", minhas.get(0).horaInicio()); // mesma data: horário crescente
        assertEquals(StatusReservaEnum.CANCELADA, minhas.get(2).status());
        assertTrue(minhas.stream().allMatch(r -> r.titularId() == 1));
    }

    @Test
    void listar_inadimplenciaNaoImpedeAConsulta_RFAS11() {
        Cenario c = new Cenario();
        ReservaService s = c.reservaService();
        s.solicitar(UID1, req(c.quadra.getId(), DATA, List.of(c.b0810.getId()), 0, null));
        c.inadimplentes.add(1);
        assertEquals(1, s.listarMinhas(UID1).size());
    }

    @Test
    void listar_porAssociado_recepcaoEAdmin_e404ParaInexistente() {
        Cenario c = new Cenario();
        ReservaService s = c.reservaService();
        s.solicitar(UID2, req(c.quadra.getId(), DATA, List.of(c.b0810.getId()), 0, null));
        assertEquals(1, s.listarPorAssociado(2).size());
        assertEquals(0, s.listarPorAssociado(1).size());
        assertThrows(ResourceNotFoundException.class, () -> s.listarPorAssociado(99));
    }
}
