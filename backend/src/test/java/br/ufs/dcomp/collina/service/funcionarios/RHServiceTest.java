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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RHServiceTest {

    @Mock
    private PresencaRepository presencaRepository;

    @Mock
    private AlocacaoRepository alocacaoRepository;

    @Mock
    private FuncionarioRepository funcionarioRepository;

    @Mock
    private FeriasRepository feriasRepository;

    @Mock
    private AfastamentoRepository afastamentoRepository;

    @InjectMocks
    private RHService rhService;

    private Funcionario funcionario;
    private Alocacao alocacao;
    private Turno turno;

    @BeforeEach
    void setUp() {
        funcionario = new Funcionario();
        funcionario.setId(1);
        funcionario.setPessoa(new br.ufs.dcomp.collina.model.associados.Pessoa(1, "Roberto Alves", "11122233344", LocalDate.of(1992, 1, 1)));
        funcionario.setStatus(StatusFuncionario.ATIVO);

        turno = new Turno(1, "Integral", LocalTime.of(8, 0), LocalTime.of(17, 0));

        alocacao = new Alocacao();
        alocacao.setId(10);
        alocacao.setData(LocalDate.of(2026, 11, 20));
        alocacao.setStatus(StatusAlocacao.CONFIRMADA);
        alocacao.setFuncionario(funcionario);
        alocacao.setTurno(turno);
    }

    @Test
    @DisplayName("RF-FU08: Registro de presença com cálculo de duração")
    void testRegistrarPresencaCalculoDuracao() {
        LocalDateTime entrada = LocalDateTime.of(2026, 11, 20, 8, 5, 0);
        LocalDateTime saida = LocalDateTime.of(2026, 11, 20, 17, 10, 0);

        PresencaRequestDTO dto = new PresencaRequestDTO(10, entrada, saida, StatusPresenca.PRESENTE);

        when(alocacaoRepository.findById(10)).thenReturn(Optional.of(alocacao));
        when(presencaRepository.findByAlocacaoId(10)).thenReturn(Optional.empty());
        when(presencaRepository.save(any(Presenca.class))).thenAnswer(inv -> {
            Presenca p = inv.getArgument(0);
            p.setId(1);
            return p;
        });

        PresencaResponseDTO resposta = rhService.registrarPresenca(dto);

        assertNotNull(resposta);
        assertEquals(StatusPresenca.PRESENTE, resposta.status());
        assertEquals(545, resposta.duracaoPresencaMinutos()); // 9 horas e 5 minutos = 545 min
        assertEquals("09h05", resposta.duracaoPresenca());
    }

    @Test
    @DisplayName("RF-FU08: Registro de ausência justificada")
    void testRegistrarAusencia() {
        AusenciaRequestDTO dto = new AusenciaRequestDTO(10, true, "Atestado médico");

        when(alocacaoRepository.findById(10)).thenReturn(Optional.of(alocacao));
        when(presencaRepository.findByAlocacaoId(10)).thenReturn(Optional.empty());
        when(presencaRepository.save(any(Presenca.class))).thenAnswer(inv -> {
            Presenca p = inv.getArgument(0);
            p.setId(2);
            return p;
        });

        PresencaResponseDTO resposta = rhService.registrarAusencia(dto);

        assertNotNull(resposta);
        assertEquals(StatusPresenca.JUSTIFICADA, resposta.status());
        assertEquals(0, resposta.duracaoPresencaMinutos());
    }

    @Test
    @DisplayName("RF-FU12: Consulta de disponibilidade do funcionário")
    void testConsultarDisponibilidade() {
        LocalDate data = LocalDate.of(2026, 11, 25);

        when(funcionarioRepository.findById(1)).thenReturn(Optional.of(funcionario));
        when(feriasRepository.isFuncionarioEmFerias(1, data)).thenReturn(false);
        when(afastamentoRepository.isFuncionarioAfastado(1, data)).thenReturn(false);
        when(alocacaoRepository.findAtivasByFuncionarioIdAndData(1, data)).thenReturn(List.of());

        DisponibilidadeResponseDTO resposta = rhService.consultarDisponibilidade(1, data);

        assertTrue(resposta.disponivel());
        assertEquals("Disponível para alocação.", resposta.motivoIndisponibilidade());
    }

    @Test
    @DisplayName("RF-FU10: Registro de férias e rejeição de conflito de datas")
    void testRegistrarFeriasEConflito() {
        LocalDate inicio = LocalDate.of(2026, 12, 1);
        LocalDate fim = LocalDate.of(2026, 12, 15);
        FeriasRequestDTO dto = new FeriasRequestDTO(1, inicio, fim, TipoFerias.REGULAR);

        when(funcionarioRepository.findById(1)).thenReturn(Optional.of(funcionario));
        when(feriasRepository.findConflitantes(1, inicio, fim)).thenReturn(List.of(new Ferias()));

        assertThrows(IllegalArgumentException.class, () -> rhService.registrarFerias(dto));
    }
}
