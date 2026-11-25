package br.ufs.dcomp.collina.service.funcionarios;

import br.ufs.dcomp.collina.dto.funcionarios.EncerramentoContratoDTO;
import br.ufs.dcomp.collina.dto.funcionarios.RescisaoResponseDTO;
import br.ufs.dcomp.collina.model.funcionarios.Funcionario;
import br.ufs.dcomp.collina.model.funcionarios.RelacaoContratual;
import br.ufs.dcomp.collina.model.funcionarios.TipoContrato;
import br.ufs.dcomp.collina.repository.funcionarios.FuncionarioRepository;
import br.ufs.dcomp.collina.repository.funcionarios.RelacaoContratualRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ContratoServiceTest {

    @Mock
    private RelacaoContratualRepository relacaoContratualRepository;

    @Mock
    private FuncionarioRepository funcionarioRepository;

    @InjectMocks
    private ContratoService contratoService;

    private Funcionario funcionario;
    private RelacaoContratual contrato;

    @BeforeEach
    void setUp() {
        funcionario = new Funcionario();
        funcionario.setId(1);
        funcionario.setNome("Fernanda Lima");

        contrato = new RelacaoContratual();
        contrato.setId(10);
        contrato.setFuncionario(funcionario);
        contrato.setTipo(TipoContrato.CLT);
        contrato.setSalario(BigDecimal.valueOf(3000.00));
        contrato.setDataInicio(LocalDate.of(2025, 1, 1));
        contrato.setDataFim(null);
    }

    @Test
    @DisplayName("RF-FU14: Cálculo de rescisão proporcional a 15 dias trabalhados")
    void testCalculoRescisaoProporcional() {
        // Salário = R$ 3.000,00 -> Diária = 3000 / 30 = 100,00 -> 15 dias = R$ 1.500,00
        BigDecimal rescisao = contratoService.calcularRescisao(BigDecimal.valueOf(3000.00), 15);
        assertEquals(new BigDecimal("1500.00"), rescisao);
    }

    @Test
    @DisplayName("RF-FU14: Encerramento de contrato com cálculo de rescisão")
    void testEncerrarContrato() {
        EncerramentoContratoDTO dto = new EncerramentoContratoDTO(LocalDate.of(2026, 11, 20), 20);

        when(relacaoContratualRepository.findById(10)).thenReturn(Optional.of(contrato));

        RescisaoResponseDTO resposta = contratoService.encerrarContrato(10, dto);

        assertNotNull(resposta);
        assertEquals(10, resposta.contratoId());
        assertEquals(1, resposta.funcionarioId());
        assertEquals("Fernanda Lima", resposta.nomeFuncionario());
        assertEquals(0, new BigDecimal("3000.00").compareTo(resposta.salarioBase()));
        assertEquals(20, resposta.diasTrabalhados());
        assertEquals(0, new BigDecimal("2000.00").compareTo(resposta.valorRescisaoProporcional()));
        assertEquals(LocalDate.of(2026, 11, 20), contrato.getDataFim());
    }
}
