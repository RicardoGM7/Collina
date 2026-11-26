package br.ufs.dcomp.collina.service.funcionarios;

import br.ufs.dcomp.collina.dto.funcionarios.FuncionarioRequestDTO;
import br.ufs.dcomp.collina.dto.funcionarios.FuncionarioResponseDTO;
import br.ufs.dcomp.collina.exception.ResourceNotFoundException;
import br.ufs.dcomp.collina.model.associados.Pessoa;
import br.ufs.dcomp.collina.model.funcionarios.Funcionario;
import br.ufs.dcomp.collina.model.funcionarios.RelacaoContratual;
import br.ufs.dcomp.collina.model.funcionarios.StatusFuncionario;
import br.ufs.dcomp.collina.repository.associados.PessoaRepository;
import br.ufs.dcomp.collina.repository.funcionarios.FuncionarioRepository;
import br.ufs.dcomp.collina.repository.funcionarios.RelacaoContratualRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FuncionarioService {

    @Autowired
    private FuncionarioRepository funcionarioRepository;

    @Autowired
    private PessoaRepository pessoaRepository;

    @Autowired
    private RelacaoContratualRepository relacaoContratualRepository;

    @Transactional
    public FuncionarioResponseDTO cadastrar(FuncionarioRequestDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Os dados do funcionário são obrigatórios.");
        }

        String cpfLimpo = dto.cpf().replaceAll("\\D", "");
        if (funcionarioRepository.existsByPessoaCpf(cpfLimpo)) {
            throw new IllegalArgumentException("Já existe um funcionário cadastrado com este CPF.");
        }

        // Localiza ou cria a entidade Pessoa (relação 1:1)
        Pessoa pessoa = pessoaRepository.findByCpf(cpfLimpo).orElseGet(() -> {
            Pessoa novaPessoa = new Pessoa();
            novaPessoa.setCpf(cpfLimpo);
            novaPessoa.setNome(dto.nome().trim());
            novaPessoa.setDataNascimento(dto.dataNascimento());
            return pessoaRepository.save(novaPessoa);
        });

        // Atualiza os dados civis caso já existisse previamente no banco
        pessoa.setNome(dto.nome().trim());
        pessoa.setDataNascimento(dto.dataNascimento());
        pessoa = pessoaRepository.save(pessoa);

        Funcionario f = new Funcionario();
        f.setPessoa(pessoa);
        f.setDataAdmissao(dto.dataAdmissao());
        f.setCargo(dto.cargo().trim());
        f.setStatus(dto.status() != null ? dto.status() : StatusFuncionario.ATIVO);

        Funcionario salvo = funcionarioRepository.save(f);

        RelacaoContratual contratoCriado = null;
        if (dto.salario() != null && dto.tipoContrato() != null) {
            RelacaoContratual contrato = new RelacaoContratual();
            contrato.setFuncionario(salvo);
            contrato.setSalario(dto.salario());
            contrato.setTipo(dto.tipoContrato());
            contrato.setDataInicio(dto.dataAdmissao());
            contratoCriado = relacaoContratualRepository.save(contrato);
        }

        return new FuncionarioResponseDTO(salvo, contratoCriado);
    }

    @Transactional
    public FuncionarioResponseDTO alterar(Integer id, FuncionarioRequestDTO dto) {
        Funcionario f = funcionarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Funcionário não encontrado com ID: " + id));

        String cpfLimpo = dto.cpf().replaceAll("\\D", "");
        if (!f.getPessoa().getCpf().equals(cpfLimpo) && funcionarioRepository.existsByPessoaCpf(cpfLimpo)) {
            throw new IllegalArgumentException("Já existe outro funcionário cadastrado com este CPF.");
        }

        Pessoa pessoa = f.getPessoa();
        pessoa.setNome(dto.nome().trim());
        pessoa.setCpf(cpfLimpo);
        pessoa.setDataNascimento(dto.dataNascimento());
        pessoaRepository.save(pessoa);

        f.setDataAdmissao(dto.dataAdmissao());
        f.setCargo(dto.cargo().trim());
        if (dto.status() != null) {
            f.setStatus(dto.status());
        }

        Funcionario atualizado = funcionarioRepository.save(f);
        RelacaoContratual contrato = relacaoContratualRepository
                .findFirstByFuncionarioIdAndDataFimIsNullOrderByDataInicioDesc(atualizado.getId())
                .orElse(null);

        return new FuncionarioResponseDTO(atualizado, contrato);
    }

    @Transactional(readOnly = true)
    public FuncionarioResponseDTO consultar(Integer id) {
        Funcionario f = funcionarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Funcionário não encontrado com ID: " + id));

        RelacaoContratual contrato = relacaoContratualRepository
                .findFirstByFuncionarioIdAndDataFimIsNullOrderByDataInicioDesc(f.getId())
                .orElse(null);

        return new FuncionarioResponseDTO(f, contrato);
    }

    @Transactional(readOnly = true)
    public List<FuncionarioResponseDTO> listarTodos() {
        return funcionarioRepository.findAll()
                .stream()
                .map(f -> {
                    RelacaoContratual c = relacaoContratualRepository
                            .findFirstByFuncionarioIdAndDataFimIsNullOrderByDataInicioDesc(f.getId())
                            .orElse(null);
                    return new FuncionarioResponseDTO(f, c);
                })
                .toList();
    }
}
