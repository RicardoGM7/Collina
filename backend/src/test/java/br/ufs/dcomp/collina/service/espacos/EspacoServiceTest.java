package br.ufs.dcomp.collina.service.espacos;

import br.ufs.dcomp.collina.dto.espacos.BlocoRequestDTO;
import br.ufs.dcomp.collina.dto.espacos.BlocoResponseDTO;
import br.ufs.dcomp.collina.dto.espacos.DisponibilidadeResponseDTO;
import br.ufs.dcomp.collina.dto.espacos.EspacoRequestDTO;
import br.ufs.dcomp.collina.dto.espacos.EspacoResponseDTO;
import br.ufs.dcomp.collina.dto.espacos.ReservaRequestDTO;
import br.ufs.dcomp.collina.exception.ResourceNotFoundException;
import br.ufs.dcomp.collina.model.auth.PerfilUsuarioEnum;
import br.ufs.dcomp.collina.model.espacos.EspacoFisico;
import br.ufs.dcomp.collina.model.espacos.EstadoEspacoEnum;
import br.ufs.dcomp.collina.model.espacos.SituacaoBlocoEnum;
import br.ufs.dcomp.collina.model.espacos.TipoBlocoEnum;
import br.ufs.dcomp.collina.service.espacos.EspacosFakes.Cenario;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EspacoServiceTest {

    private static final LocalDate DIA = LocalDate.parse("2026-11-21");

    private static EspacoRequestDTO espaco(String nome, Boolean publico, int convidados, Integer capacidade) {
        return new EspacoRequestDTO(nome, publico, false, convidados, capacidade);
    }

    // ---------------------------------------------------------------- espaços

    @Test
    void cadastrar_espacoControlado_comNomeAparado() {
        Cenario c = new Cenario();
        EspacoResponseDTO r = c.espacoService().cadastrar(espaco("  Sala de Dança ", false, 3, 12));
        assertEquals("Sala de Dança", r.nome());
        assertFalse(r.publico());
        assertEquals(EstadoEspacoEnum.ATIVO, r.estado());
        assertEquals(12, r.capacidadeMaxima());
    }

    @Test
    void cadastrar_nomeDuplicadoIgnorandoCaixa_409() {
        Cenario c = new Cenario();
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> c.espacoService().cadastrar(espaco("quadra 1", false, 2, 10)));
        assertTrue(e.getMessage().contains("Já existe"));
    }

    @Test
    void cadastrar_regrasDeCapacidade_400() {
        Cenario c = new Cenario();
        EspacoService s = c.espacoService();
        assertThrows(IllegalArgumentException.class, () -> s.cadastrar(espaco("X", false, 4, null)));
        assertThrows(IllegalArgumentException.class, () -> s.cadastrar(espaco("Y", false, 11, 10)));
    }

    @Test
    void cadastrar_espacoPublico_descartaACapacidade() {
        Cenario c = new Cenario();
        EspacoResponseDTO r = c.espacoService().cadastrar(espaco("Parque", true, 50, 5));
        assertTrue(r.publico());
        assertNull(r.capacidadeMaxima());
    }

    @Test
    void atualizar_e_alterarEstado() {
        Cenario c = new Cenario();
        EspacoService s = c.espacoService();
        EspacoResponseDTO r = s.atualizar(c.quadra.getId(), espaco("Quadra Principal", false, 4, 14));
        assertEquals("Quadra Principal", r.nome());
        assertEquals(EstadoEspacoEnum.EM_MANUTENCAO, s.alterarEstado(c.quadra.getId(), EstadoEspacoEnum.EM_MANUTENCAO).estado());
        assertThrows(ResourceNotFoundException.class, () -> s.alterarEstado(999, EstadoEspacoEnum.ATIVO));
        assertThrows(ResourceNotFoundException.class, () -> s.atualizar(999, espaco("Z", false, 1, 5)));
    }

    @Test
    void atualizar_nomeDeOutroEspaco_409_masOProprioNomePodeFicar() {
        Cenario c = new Cenario();
        EspacoService s = c.espacoService();
        assertThrows(IllegalStateException.class, () -> s.atualizar(c.quadra.getId(), espaco("Piscina", false, 2, 10)));
        s.atualizar(c.quadra.getId(), espaco("Quadra 1", false, 2, 10));
    }

    @Test
    void manutencao_bloqueiaNovasReservas_masMantemAsExistentes() {
        Cenario c = new Cenario();
        EspacoService espacos = c.espacoService();
        ReservaService reservas = c.reservaService();
        reservas.solicitar("uid-1", new ReservaRequestDTO(c.quadra.getId(), DIA, List.of(c.b0810.getId()), 0, null));
        espacos.alterarEstado(c.quadra.getId(), EstadoEspacoEnum.EM_MANUTENCAO);
        assertThrows(IllegalStateException.class, () ->
                reservas.solicitar("uid-1", new ReservaRequestDTO(c.quadra.getId(), DIA, List.of(c.b1012.getId()), 0, null)));
        assertEquals(1, c.reservas.dados.size());
    }

    @Test
    void listar_associadoVeSoOsReservaveis_funcionarioVeTodos() {
        Cenario c = new Cenario();
        EspacoFisico inativo = c.novoEspaco("Sauna", false, 1, 4);
        inativo.setEstado(EstadoEspacoEnum.INATIVO);
        EspacoFisico treino = c.novoEspaco("Ginásio de Treino", false, 0, 30);
        treino.setUsoExclusivoTreino(true);

        EspacoService s = c.espacoService();
        assertEquals(List.of("Piscina", "Quadra 1"),
                s.listar(PerfilUsuarioEnum.ASSOCIADO).stream().map(EspacoResponseDTO::nome).toList());
        assertEquals(4, s.listar(PerfilUsuarioEnum.RECEPCAO).size());
        assertThrows(ResourceNotFoundException.class, () -> s.buscarPorId(PerfilUsuarioEnum.ASSOCIADO, inativo.getId()));
        assertEquals("Sauna", s.buscarPorId(PerfilUsuarioEnum.ADMIN, inativo.getId()).nome());
    }

    // ----------------------------------------------------------------- blocos

    private static BlocoRequestDTO bloco(String ini, String fim, TipoBlocoEnum tipo) {
        return new BlocoRequestDTO(ini, fim, tipo);
    }

    @Test
    void bloco_criaComDuasHoras_tipoPadraoLivre() {
        Cenario c = new Cenario();
        BlocoResponseDTO b = c.espacoService().adicionarBloco(c.quadra.getId(), bloco("20:00", "22:00", null));
        assertEquals("20:00", b.horaInicio());
        assertEquals("22:00", b.horaFim());
        assertEquals(TipoBlocoEnum.LIVRE, b.tipo());
        assertEquals(c.quadra.getId(), b.espacoId());
    }

    @Test
    void bloco_duracaoDiferenteDeDuasHoras_409() {
        Cenario c = new Cenario();
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> c.espacoService().adicionarBloco(c.quadra.getId(), bloco("06:00", "07:00", null)));
        assertTrue(e.getMessage().contains("2 horas"));
    }

    @Test
    void bloco_naoPodeSobreporOutroDoMesmoEspaco_masEmOutroEspacoPode() {
        Cenario c = new Cenario();
        EspacoService s = c.espacoService();
        assertThrows(IllegalStateException.class, () -> s.adicionarBloco(c.quadra.getId(), bloco("09:00", "11:00", null)));
        s.adicionarBloco(c.piscina.getId(), bloco("10:00", "12:00", null));
        assertThrows(ResourceNotFoundException.class, () -> s.adicionarBloco(999, bloco("20:00", "22:00", null)));
    }

    @Test
    void bloco_listagemOrdenadaPorHorario() {
        Cenario c = new Cenario();
        List<String> horas = c.espacoService().listarBlocos(PerfilUsuarioEnum.RECEPCAO, c.quadra.getId())
                .stream().map(BlocoResponseDTO::horaInicio).toList();
        assertEquals(List.of("08:00", "10:00", "14:00", "16:00", "18:00"), horas);
    }

    // ---------------------------------------------------------- disponibilidade

    @Test
    void disponibilidade_associadoVeSoOsBlocosLivresEDesocupados() {
        Cenario c = new Cenario();
        c.reservaService().solicitar("uid-1", new ReservaRequestDTO(c.quadra.getId(), DIA, List.of(c.b0810.getId()), 0, null));
        DisponibilidadeResponseDTO r = c.espacoService()
                .consultarDisponibilidade(PerfilUsuarioEnum.ASSOCIADO, c.quadra.getId(), DIA);
        assertEquals(List.of(c.b1012.getId(), c.b1416.getId()),
                r.blocos().stream().map(DisponibilidadeResponseDTO.BlocoDisponibilidadeDTO::blocoId).toList());
        assertTrue(r.blocos().stream().allMatch(b -> b.situacao() == SituacaoBlocoEnum.LIVRE));
        assertNull(r.mensagem());
    }

    @Test
    void disponibilidade_funcionarioVeAAgendaCompletaComASituacaoDeCadaBloco() {
        Cenario c = new Cenario();
        c.reservaService().solicitar("uid-1", new ReservaRequestDTO(c.quadra.getId(), DIA, List.of(c.b0810.getId()), 0, null));
        DisponibilidadeResponseDTO r = c.espacoService()
                .consultarDisponibilidade(PerfilUsuarioEnum.RECEPCAO, c.quadra.getId(), DIA);
        Map<Integer, SituacaoBlocoEnum> mapa = r.blocos().stream().collect(Collectors.toMap(
                DisponibilidadeResponseDTO.BlocoDisponibilidadeDTO::blocoId,
                DisponibilidadeResponseDTO.BlocoDisponibilidadeDTO::situacao));
        assertEquals(SituacaoBlocoEnum.OCUPADO, mapa.get(c.b0810.getId()));
        assertEquals(SituacaoBlocoEnum.LIVRE, mapa.get(c.b1012.getId()));
        assertEquals(SituacaoBlocoEnum.LIVRE, mapa.get(c.b1416.getId()));
        assertEquals(SituacaoBlocoEnum.MANUTENCAO, mapa.get(c.bManutencao.getId()));
        assertEquals(SituacaoBlocoEnum.TREINO, mapa.get(c.bTreino.getId()));
    }

    @Test
    void disponibilidade_reservaCanceladaLiberaOBloco_eOutroDiaNaoEAfetado() {
        Cenario c = new Cenario();
        ReservaService reservas = c.reservaService();
        EspacoService espacos = c.espacoService();
        Integer id = reservas.solicitar("uid-1",
                new ReservaRequestDTO(c.quadra.getId(), DIA, List.of(c.b0810.getId()), 0, null)).get(0).id();
        assertEquals(3, espacos.consultarDisponibilidade(PerfilUsuarioEnum.ASSOCIADO, c.quadra.getId(), DIA.plusDays(1)).blocos().size());
        assertEquals(2, espacos.consultarDisponibilidade(PerfilUsuarioEnum.ASSOCIADO, c.quadra.getId(), DIA).blocos().size());
        reservas.cancelar(PerfilUsuarioEnum.ASSOCIADO, "uid-1", id);
        assertEquals(3, espacos.consultarDisponibilidade(PerfilUsuarioEnum.ASSOCIADO, c.quadra.getId(), DIA).blocos().size());
    }

    @Test
    void disponibilidade_espacoEmManutencao_associadoNaoVeNada_funcionarioVeIndisponivel() {
        Cenario c = new Cenario();
        c.estado(EstadoEspacoEnum.EM_MANUTENCAO);
        EspacoService s = c.espacoService();

        DisponibilidadeResponseDTO assoc = s.consultarDisponibilidade(PerfilUsuarioEnum.ASSOCIADO, c.quadra.getId(), DIA);
        assertTrue(assoc.blocos().isEmpty());
        assertTrue(assoc.mensagem().contains("Não há horários livres"));

        DisponibilidadeResponseDTO func = s.consultarDisponibilidade(PerfilUsuarioEnum.ADMIN, c.quadra.getId(), DIA);
        assertFalse(func.blocos().isEmpty());
        assertTrue(func.blocos().stream().noneMatch(b -> b.situacao() == SituacaoBlocoEnum.LIVRE));
    }

    @Test
    void disponibilidade_dataAusenteE400_espacoInexistenteE404() {
        Cenario c = new Cenario();
        EspacoService s = c.espacoService();
        assertThrows(IllegalArgumentException.class, () -> s.consultarDisponibilidade(PerfilUsuarioEnum.ASSOCIADO, c.quadra.getId(), null));
        assertThrows(ResourceNotFoundException.class, () -> s.consultarDisponibilidade(PerfilUsuarioEnum.ASSOCIADO, 999, DIA));
    }
}
