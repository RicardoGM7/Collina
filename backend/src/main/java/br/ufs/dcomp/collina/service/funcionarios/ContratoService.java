package br.ufs.dcomp.collina.service.funcionarios;

import br.ufs.dcomp.collina.dto.funcionarios.*;
import br.ufs.dcomp.collina.exception.ResourceNotFoundException;
import br.ufs.dcomp.collina.model.funcionarios.Funcionario;
import br.ufs.dcomp.collina.model.funcionarios.RelacaoContratual;
import br.ufs.dcomp.collina.model.funcionarios.StatusFuncionario;
import br.ufs.dcomp.collina.repository.funcionarios.FuncionarioRepository;
import br.ufs.dcomp.collina.repository.funcionarios.RelacaoContratualRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class ContratoService {

    @Autowired
    private RelacaoContratualRepository relacaoContratualRepository;

    @Autowired
    private FuncionarioRepository funcionarioRepository;

    @Transactional
    public RelacaoContratualResponseDTO cadastrarContrato(RelacaoContratualRequestDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Os dados do contrato são obrigatórios.");
        }

        Funcionario f = funcionarioRepository.findById(dto.funcionarioId())
                .orElseThrow(() -> new ResourceNotFoundException("Funcionário não encontrado com ID: " + dto.funcionarioId()));

        RelacaoContratual contrato = new RelacaoContratual();
        contrato.setFuncionario(f);
        contrato.setTipo(dto.tipo());
        contrato.setSalario(dto.salario());
        contrato.setDataInicio(dto.dataInicio());
        contrato.setDataFim(dto.dataFim());

        RelacaoContratual salvo = relacaoContratualRepository.save(contrato);
        return new RelacaoContratualResponseDTO(salvo);
    }

    @Transactional(readOnly = true)
    public List<RelacaoContratualResponseDTO> consultarHistorico(Integer funcionarioId) {
        if (funcionarioId == null) {
            throw new IllegalArgumentException("O ID do funcionário é obrigatório.");
        }
        if (!funcionarioRepository.existsById(funcionarioId)) {
            throw new ResourceNotFoundException("Funcionário não encontrado com ID: " + funcionarioId);
        }

        return relacaoContratualRepository.findByFuncionarioId(funcionarioId)
                .stream()
                .map(RelacaoContratualResponseDTO::new)
                .toList();
    }

    /**
     * RF-FU14 e Critério de Aceite FUNC01:
     * Encerramento de relação contratual com inativação do funcionário e cálculo de salário proporcional.
     */
    @Transactional
    public RescisaoResponseDTO encerrarContrato(Integer contratoId, EncerramentoContratoDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Os dados de encerramento são obrigatórios.");
        }

        RelacaoContratual contrato = relacaoContratualRepository.findById(contratoId)
                .orElseThrow(() -> new ResourceNotFoundException("Contrato não encontrado com ID: " + contratoId));

        if (contrato.getDataFim() != null && contrato.getDataFim().isBefore(dto.dataFim())) {
            throw new IllegalStateException("O contrato já foi encerrado na data " + contrato.getDataFim());
        }

        contrato.setDataFim(dto.dataFim());
        relacaoContratualRepository.save(contrato);

        // Inativa o funcionário ao encerrar a relação contratual vigente
        Funcionario funcionario = contrato.getFuncionario();
        if (funcionario != null) {
            funcionario.setStatus(StatusFuncionario.INATIVO);
            funcionarioRepository.save(funcionario);
        }

        BigDecimal valorRescisao = calcularRescisao(contrato.getSalario(), dto.diasTrabalhados());

        return new RescisaoResponseDTO(
                contrato.getId(),
                funcionario != null ? funcionario.getId() : null,
                funcionario != null ? funcionario.getNome() : null,
                contrato.getSalario(),
                dto.dataFim(),
                dto.diasTrabalhados(),
                valorRescisao
        );
    }

    /**
     * Cálculo da rescisão proporcional: (salário / 30) * diasTrabalhados
     */
    public BigDecimal calcularRescisao(BigDecimal salario, int diasTrabalhados) {
        if (salario == null || diasTrabalhados <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return salario.divide(BigDecimal.valueOf(30), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(diasTrabalhados))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
