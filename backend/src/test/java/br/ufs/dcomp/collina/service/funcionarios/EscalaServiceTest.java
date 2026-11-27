package br.ufs.dcomp.collina.service.funcionarios;

import br.ufs.dcomp.collina.dto.funcionarios.*;
import br.ufs.dcomp.collina.model.funcionarios.*;
import br.ufs.dcomp.collina.repository.funcionarios.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EscalaServiceTest {

    @Mock
    private EscalaRepository escalaRepository;

    @Mock
    private TurnoRepository turnoRepository;

    @Mock
    private AlocacaoRepository alocacaoRepository;

    @Mock
    private FuncionarioRepository funcionarioRepository;

    @Mock
    private FeriasRepository feriasRepository;

    @Mock
    private AfastamentoRepository afastamentoRepository;

    @InjectMocks
    private EscalaService escalaService;

    private Funcionario funcionarioAtivo;
    private Funcionario funcionarioInativo;
    private Turno turnoManha;
    private Turno turnoTarde;
    private Turno turnoNoturno;
    private Escala escalaNovembro;

    @BeforeEach
    void setUp() {
        br.ufs.dcomp.collina.model.associados.Pessoa p1 = new br.ufs.dcomp.collina.model.associados.Pessoa(1, "Carlos Silva", "12345678901", LocalDate.of(1990, 5, 20));
        funcionarioAtivo = new Funcionario();
        funcionarioAtivo.setId(1);
        funcionarioAtivo.setPessoa(p1);
        funcionarioAtivo.setDataAdmissao(LocalDate.of(2020, 1, 10));
        funcionarioAtivo.setCargo("Guarda-vidas");
        funcionarioAtivo.setStatus(StatusFuncionario.ATIVO);

        br.ufs.dcomp.collina.model.associados.Pessoa p2 = new br.ufs.dcomp.collina.model.associados.Pessoa(2, "Maria Souza", "98765432100", LocalDate.of(1995, 3, 10));
        funcionarioInativo = new Funcionario();
        funcionarioInativo.setId(2);
        funcionarioInativo.setPessoa(p2);
        funcionarioInativo.setDataAdmissao(LocalDate.of(2021, 2, 1));
        funcionarioInativo.setCargo("Atendente");
        funcionarioInativo.setStatus(StatusFuncionario.INATIVO);

        turnoManha = new Turno(1, "Manhã", LocalTime.of(8, 0), LocalTime.of(14, 0)); // 6 horas
        turnoTarde = new Turno(2, "Tarde", LocalTime.of(14, 0), LocalTime.of(22, 0)); // 8 horas
        turnoNoturno = new Turno(3, "Noite", LocalTime.of(22, 0), LocalTime.of(6, 0)); // 8 horas (cruza meia-noite)

        escalaNovembro = new Escala();
        escalaNovembro.setId(1);
        escalaNovembro.setPeriodo("2026-11");
        escalaNovembro.setStatus(StatusEscala.PENDENTE);
    }

    @Test
    @DisplayName("Cálculo de duração de turnos diurno e noturno")
    void testCalculoDuracaoTurnos() {
        assertEquals(360, turnoManha.getDuracaoMinutos());
        assertEquals(6.0, turnoManha.getDuracaoHoras());
        assertEquals("06h00", turnoManha.getDuracao());

        assertEquals(480, turnoTarde.getDuracaoMinutos());
        assertEquals(8.0, turnoTarde.getDuracaoHoras());
        assertEquals("08h00", turnoTarde.getDuracao());

        // Noturno (22h às 06h = 8h)
        assertEquals(480, turnoNoturno.getDuracaoMinutos());
        assertEquals(8.0, turnoNoturno.getDuracaoHoras());
        assertEquals("08h00", turnoNoturno.getDuracao());
    }

    @Test
    @DisplayName("RF-FU07: Deve rejeitar alocação de funcionário inativo")
    void testRejeitarAlocacaoFuncionarioInativo() {
        AlocacaoRequestDTO dto = new AlocacaoRequestDTO(2, 1, 1, LocalDate.of(2026, 11, 10));

        when(funcionarioRepository.findById(2)).thenReturn(Optional.of(funcionarioInativo));
        when(turnoRepository.findById(1)).thenReturn(Optional.of(turnoManha));
        when(escalaRepository.findById(1)).thenReturn(Optional.of(escalaNovembro));

        ValidacaoAlocacaoResponseDTO validacao = escalaService.validarAlocacao(dto);

        assertFalse(validacao.valido());
        assertTrue(validacao.motivos().stream().anyMatch(m -> m.contains("INATIVO")));

        assertThrows(IllegalArgumentException.class, () -> escalaService.alocarFuncionario(dto));
    }

    @Test
    @DisplayName("RF-FU10: Deve rejeitar alocação em período de férias")
    void testRejeitarAlocacaoEmFerias() {
        LocalDate data = LocalDate.of(2026, 11, 15);
        AlocacaoRequestDTO dto = new AlocacaoRequestDTO(1, 1, 1, data);

        when(funcionarioRepository.findById(1)).thenReturn(Optional.of(funcionarioAtivo));
        when(turnoRepository.findById(1)).thenReturn(Optional.of(turnoManha));
        when(escalaRepository.findById(1)).thenReturn(Optional.of(escalaNovembro));
        when(feriasRepository.isFuncionarioEmFerias(1, data)).thenReturn(true);

        ValidacaoAlocacaoResponseDTO validacao = escalaService.validarAlocacao(dto);

        assertFalse(validacao.valido());
        assertTrue(validacao.motivos().stream().anyMatch(m -> m.contains("FÉRIAS")));
    }

    @Test
    @DisplayName("RF-FU11: Deve rejeitar alocação em período de afastamento")
    void testRejeitarAlocacaoEmAfastamento() {
        LocalDate data = LocalDate.of(2026, 11, 18);
        AlocacaoRequestDTO dto = new AlocacaoRequestDTO(1, 1, 1, data);

        when(funcionarioRepository.findById(1)).thenReturn(Optional.of(funcionarioAtivo));
        when(turnoRepository.findById(1)).thenReturn(Optional.of(turnoManha));
        when(escalaRepository.findById(1)).thenReturn(Optional.of(escalaNovembro));
        when(feriasRepository.isFuncionarioEmFerias(1, data)).thenReturn(false);
        when(afastamentoRepository.isFuncionarioAfastado(1, data)).thenReturn(true);

        ValidacaoAlocacaoResponseDTO validacao = escalaService.validarAlocacao(dto);

        assertFalse(validacao.valido());
        assertTrue(validacao.motivos().stream().anyMatch(m -> m.contains("AFASTAMENTO")));
    }

    @Test
    @DisplayName("RD-03 / CLT: Deve rejeitar jornada diária excedendo 8 horas")
    void testRejeitarLimiteDiarioCLT() {
        LocalDate data = LocalDate.of(2026, 11, 10);
        AlocacaoRequestDTO dto = new AlocacaoRequestDTO(1, 1, 2, data); // Turno tarde = 8h

        // Já existe um turno manhã (6h) no mesmo dia -> 6h + 8h = 14h > 8h
        Alocacao alocExistente = new Alocacao(10, data, StatusAlocacao.CONFIRMADA, funcionarioAtivo, escalaNovembro, turnoManha, null);

        when(funcionarioRepository.findById(1)).thenReturn(Optional.of(funcionarioAtivo));
        when(turnoRepository.findById(2)).thenReturn(Optional.of(turnoTarde));
        when(escalaRepository.findById(1)).thenReturn(Optional.of(escalaNovembro));
        when(alocacaoRepository.findAtivasByFuncionarioIdAndData(1, data)).thenReturn(List.of(alocExistente));

        ValidacaoAlocacaoResponseDTO validacao = escalaService.validarAlocacao(dto);

        assertFalse(validacao.valido());
        assertTrue(validacao.motivos().stream().anyMatch(m -> m.contains("Limite diário de 8 horas")));
    }

    @Test
    @DisplayName("RD-03 / CLT: Deve rejeitar descanso interjornada inferior a 11 horas")
    void testRejeitarDescansoInterjornadaMenor11h() {
        LocalDate dataHoje = LocalDate.of(2026, 11, 11);
        LocalDate dataOntem = LocalDate.of(2026, 11, 10);

        // Turno ontem: 14:00 às 22:00
        Alocacao alocOntem = new Alocacao(10, dataOntem, StatusAlocacao.CONFIRMADA, funcionarioAtivo, escalaNovembro, turnoTarde, null);

        // Novo turno hoje às 08:00 (descanso entre 22:00 e 08:00 = 10 horas < 11 horas!)
        AlocacaoRequestDTO dto = new AlocacaoRequestDTO(1, 1, 1, dataHoje);

        when(funcionarioRepository.findById(1)).thenReturn(Optional.of(funcionarioAtivo));
        when(turnoRepository.findById(1)).thenReturn(Optional.of(turnoManha));
        when(escalaRepository.findById(1)).thenReturn(Optional.of(escalaNovembro));
        when(alocacaoRepository.findAtivasByFuncionarioIdAndData(1, dataHoje)).thenReturn(new ArrayList<>());
        when(alocacaoRepository.findAtivasByFuncionarioIdAndPeriodo(eq(1), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(alocOntem));

        ValidacaoAlocacaoResponseDTO validacao = escalaService.validarAlocacao(dto);

        assertFalse(validacao.valido());
        assertTrue(validacao.motivos().stream().anyMatch(m -> m.contains("Descanso interjornada inferior a 11 horas")));
    }

    @Test
    @DisplayName("Alocação válida deve ser salva com sucesso")
    void testAlocacaoValidaSucesso() {
        LocalDate data = LocalDate.of(2026, 11, 12);
        AlocacaoRequestDTO dto = new AlocacaoRequestDTO(1, 1, 1, data);

        when(funcionarioRepository.findById(1)).thenReturn(Optional.of(funcionarioAtivo));
        when(turnoRepository.findById(1)).thenReturn(Optional.of(turnoManha));
        when(escalaRepository.findById(1)).thenReturn(Optional.of(escalaNovembro));
        when(alocacaoRepository.findAtivasByFuncionarioIdAndData(1, data)).thenReturn(new ArrayList<>());
        when(alocacaoRepository.findAtivasByFuncionarioIdAndPeriodo(eq(1), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new ArrayList<>());

        when(alocacaoRepository.save(any(Alocacao.class))).thenAnswer(invocation -> {
            Alocacao a = invocation.getArgument(0);
            a.setId(100);
            return a;
        });

        AlocacaoResponseDTO resposta = escalaService.alocarFuncionario(dto);

        assertNotNull(resposta);
        assertEquals(100, resposta.id());
        assertEquals("Carlos Silva", resposta.nomeFuncionario());
        assertEquals("Manhã", resposta.nomeTurno());
        assertEquals(StatusAlocacao.PENDENTE, resposta.status());
    }

    @Test
    @DisplayName("Publicar escala deve atualizar status da escala e confirmar alocações pendentes")
    void testPublicarEscala() {
        Alocacao alocPendente = new Alocacao(1, LocalDate.of(2026, 11, 5), StatusAlocacao.PENDENTE, funcionarioAtivo, escalaNovembro, turnoManha, null);
        escalaNovembro.setAlocacoes(new ArrayList<>(List.of(alocPendente)));

        when(escalaRepository.findById(1)).thenReturn(Optional.of(escalaNovembro));
        when(escalaRepository.save(any(Escala.class))).thenAnswer(inv -> inv.getArgument(0));

        EscalaResponseDTO resposta = escalaService.publicarEscala(1);

        assertNotNull(resposta);
        assertEquals(StatusEscala.PUBLICADA, resposta.status());
        assertEquals(StatusAlocacao.CONFIRMADA, alocPendente.getStatus());
    }

    @Test
    @DisplayName("Atualizar status da alocação com sucesso")
    void testAtualizarStatusAlocacao() {
        Alocacao alocacao = new Alocacao(1, LocalDate.of(2026, 11, 5), StatusAlocacao.PENDENTE, funcionarioAtivo, escalaNovembro, turnoManha, null);

        when(alocacaoRepository.findById(1)).thenReturn(Optional.of(alocacao));
        when(alocacaoRepository.save(any(Alocacao.class))).thenAnswer(inv -> inv.getArgument(0));

        AlocacaoResponseDTO resposta = escalaService.atualizarStatusAlocacao(1, StatusAlocacao.CONFIRMADA);

        assertNotNull(resposta);
        assertEquals(StatusAlocacao.CONFIRMADA, resposta.status());
        assertEquals(StatusAlocacao.CONFIRMADA, alocacao.getStatus());
    }
}
