package br.ufs.dcomp.collina.service.financeiro;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.ufs.dcomp.collina.repository.financeiro.SolicitacaoGastoRepository;
import br.ufs.dcomp.collina.dto.financeiro.SolicitacaoGastoRequestDTO;
import br.ufs.dcomp.collina.dto.financeiro.SolicitacaoGastoResponseDTO;
import br.ufs.dcomp.collina.exception.ResourceNotFoundException;
import br.ufs.dcomp.collina.model.financeiro.SolicitacaoGasto;
import br.ufs.dcomp.collina.model.financeiro.StatusSolicitacaoGastoEnum;

import java.math.BigDecimal;
import java.util.List;
import java.util.Comparator;

@Service
public class SolicitacaoGastoService {
    
    @Autowired 
    private SolicitacaoGastoRepository solicitacaoRepository;

    /* Não precisei usar até o momento
    @Autowired
    private FuncionarioService funcionarioService
    */

    // RF-F02/RF-F19 - Cadastrar a Operação de Custo com sua Prioridade
    @Transactional
    public SolicitacaoGastoResponseDTO cadastrarDados(
                    SolicitacaoGastoRequestDTO dados, Long funcionarioId) {

        // Verificação dos dados
        if (dados.descricao() == null || dados.descricao().trim().isBlank()) {
            throw new IllegalArgumentException("A descrição é obrigatoria.");
        }
         if (dados.valor() == null || dados.valor().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor deve ser maior que zero.");
        }
        if (dados.prioridade() == null) {
            throw new IllegalArgumentException("A prioridade da solicitação não pode ser nula.");
        }
        if (dados.dataSolicitacao() == null) {
            throw new IllegalArgumentException("A data da solicitação não pode ser nula.");
        }
        if (funcionarioId == null) {
            throw new IllegalArgumentException("O id do funcionário é obrigatório.");
        }

        // Criacao da nova solicitacao de gasto
        SolicitacaoGasto solicitacao = new SolicitacaoGasto();
        solicitacao.setDescricao(dados.descricao());
        solicitacao.setValor(dados.valor());
        solicitacao.setPrioridade(dados.prioridade());
        solicitacao.setStatus(StatusSolicitacaoGastoEnum.PENDENTE);
        solicitacao.setDataSolicitacao(dados.dataSolicitacao());
        solicitacao.setFuncionarioId(funcionarioId);
        
        return new SolicitacaoGastoResponseDTO(solicitacaoRepository.save(solicitacao));
    }

    // Listar as operações por prioridade
    @Transactional(readOnly = true)
    public List<SolicitacaoGastoResponseDTO> listarPorPrioridade() {
        return solicitacaoRepository
                .findByStatus(StatusSolicitacaoGastoEnum.PENDENTE)
                .stream()
                .sorted(Comparator
                        .comparing(SolicitacaoGasto::getPrioridade)
                        .thenComparing(SolicitacaoGasto::getDataSolicitacao))
                .map(SolicitacaoGastoResponseDTO::new)
                .toList();
    }

    // RF-F19 - Marcar Operações Registradas como Efetivada
    public void marcarComoEfetivada(Long id){
        SolicitacaoGasto solicitacao = solicitacaoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                    "Solicitação de gasto não encontrada com ID: " + id));
        
        if(!solicitacao.estaPendente()) {
            throw new IllegalStateException("Só solicitações pendentes podem ser efetivadas.");
        }

        solicitacao.efetivar();
        solicitacaoRepository.save(solicitacao);
    }
}
