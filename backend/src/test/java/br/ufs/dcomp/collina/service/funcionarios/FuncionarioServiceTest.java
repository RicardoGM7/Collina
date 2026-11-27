package br.ufs.dcomp.collina.service.funcionarios;

import br.ufs.dcomp.collina.dto.funcionarios.FuncionarioRequestDTO;
import br.ufs.dcomp.collina.dto.funcionarios.FuncionarioResponseDTO;
import br.ufs.dcomp.collina.model.auth.Pessoa;
import br.ufs.dcomp.collina.model.funcionarios.Funcionario;
import br.ufs.dcomp.collina.model.funcionarios.RelacaoContratual;
import br.ufs.dcomp.collina.model.funcionarios.StatusFuncionario;
import br.ufs.dcomp.collina.model.funcionarios.TipoContrato;
import br.ufs.dcomp.collina.repository.auth.PessoaRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FuncionarioServiceTest {

    @Mock
    private FuncionarioRepository funcionarioRepository;

    @Mock
    private PessoaRepository pessoaRepository;

    @Mock
    private RelacaoContratualRepository relacaoContratualRepository;

    @InjectMocks
    private FuncionarioService funcionarioService;

    private Pessoa pessoa;
    private Funcionario funcionario;

    @BeforeEach
    void setUp() {
        pessoa = new Pessoa(1, "Juliana Lima", "12345678901", LocalDate.of(1995, 4, 15));
        funcionario = new Funcionario();
        funcionario.setId(10);
        funcionario.setPessoa(pessoa);
        funcionario.setDataAdmissao(LocalDate.of(2024, 1, 10));
        funcionario.setCargo("Recepcionista");
        funcionario.setStatus(StatusFuncionario.ATIVO);
    }

    @Test
    @DisplayName("RF-FU01: Cadastro de funcionário cria vínculo 1:1 com Pessoa e contrato inicial")
    void testCadastrarFuncionarioComPessoaEContrato() {
        FuncionarioRequestDTO dto = new FuncionarioRequestDTO(
                "Juliana Lima",
                "123.456.789-01",
                LocalDate.of(1995, 4, 15),
                LocalDate.of(2024, 1, 10),
                "Recepcionista",
                StatusFuncionario.ATIVO,
                TipoContrato.CLT,
                BigDecimal.valueOf(2500.00)
        );

        when(funcionarioRepository.existsByPessoaCpf("12345678901")).thenReturn(false);
        when(pessoaRepository.findByCpf("12345678901")).thenReturn(Optional.empty());
        when(pessoaRepository.save(any(Pessoa.class))).thenAnswer(inv -> {
            Pessoa p = inv.getArgument(0);
            p.setId(1);
            return p;
        });

        when(funcionarioRepository.save(any(Funcionario.class))).thenAnswer(inv -> {
            Funcionario f = inv.getArgument(0);
            f.setId(10);
            return f;
        });

        when(relacaoContratualRepository.save(any(RelacaoContratual.class))).thenAnswer(inv -> {
            RelacaoContratual c = inv.getArgument(0);
            c.setId(100);
            return c;
        });

        FuncionarioResponseDTO response = funcionarioService.cadastrar(dto);

        assertNotNull(response);
        assertEquals(10, response.id());
        assertEquals(1, response.pessoaId());
        assertEquals("Juliana Lima", response.nome());
        assertEquals("12345678901", response.cpf());
        assertEquals("Recepcionista", response.cargo());
        assertEquals(StatusFuncionario.ATIVO, response.status());
        assertEquals(TipoContrato.CLT, response.tipoContratoVigente());
        assertEquals(BigDecimal.valueOf(2500.00), response.salarioVigente());

        verify(pessoaRepository, atLeastOnce()).save(any(Pessoa.class));
        verify(funcionarioRepository, times(1)).save(any(Funcionario.class));
        verify(relacaoContratualRepository, times(1)).save(any(RelacaoContratual.class));
    }

    @Test
    @DisplayName("RF-FU01: Deve rejeitar cadastro se CPF já estiver cadastrado para outro funcionário")
    void testRejeitarCpfDuplicado() {
        FuncionarioRequestDTO dto = new FuncionarioRequestDTO(
                "Juliana Lima",
                "12345678901",
                LocalDate.of(1995, 4, 15),
                LocalDate.of(2024, 1, 10),
                "Recepcionista",
                StatusFuncionario.ATIVO
        );

        when(funcionarioRepository.existsByPessoaCpf("12345678901")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> funcionarioService.cadastrar(dto));
    }

    @Test
    @DisplayName("RF-FU02: Alteração de funcionário atualiza dados de Pessoa e Funcionario")
    void testAlterarFuncionarioEPessoa() {
        FuncionarioRequestDTO dto = new FuncionarioRequestDTO(
                "Juliana Lima Silva",
                "12345678901",
                LocalDate.of(1995, 4, 15),
                LocalDate.of(2024, 1, 10),
                "Supervisora de Recepção",
                StatusFuncionario.ATIVO
        );

        when(funcionarioRepository.findById(10)).thenReturn(Optional.of(funcionario));
        when(funcionarioRepository.save(any(Funcionario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(relacaoContratualRepository.findFirstByFuncionarioIdAndDataFimIsNullOrderByDataInicioDesc(10))
                .thenReturn(Optional.empty());

        FuncionarioResponseDTO response = funcionarioService.alterar(10, dto);

        assertNotNull(response);
        assertEquals("Juliana Lima Silva", response.nome());
        assertEquals("Supervisora de Recepção", response.cargo());
        verify(pessoaRepository, times(1)).save(pessoa);
    }
}
