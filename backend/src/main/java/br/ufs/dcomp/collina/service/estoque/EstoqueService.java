package br.ufs.dcomp.collina.service.estoque;

import br.ufs.dcomp.collina.dto.estoque.EstoqueResponseDTO;
import br.ufs.dcomp.collina.dto.estoque.MovimentacaoEstoqueRequestDTO;
import br.ufs.dcomp.collina.dto.estoque.MovimentacaoEstoqueResponseDTO;
import br.ufs.dcomp.collina.exception.ResourceNotFoundException;
import br.ufs.dcomp.collina.model.estoque.Estoque;
import br.ufs.dcomp.collina.model.estoque.MovimentacaoEstoque;
import br.ufs.dcomp.collina.model.estoque.MovimentacaoEstoqueEnum;
import br.ufs.dcomp.collina.repository.estoque.EstoqueRepository;
import br.ufs.dcomp.collina.repository.estoque.MovimentacaoEstoqueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class EstoqueService {

    @Autowired
    private EstoqueRepository estoqueRepository;

    @Autowired
    private MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;

    // ==========================================
    // 1. Operações de Saldo de Estoque
    // ==========================================

    @Transactional(readOnly = true)
    public List<EstoqueResponseDTO> listarTodos() {
        return estoqueRepository.findAll()
                .stream()
                .map(EstoqueResponseDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public EstoqueResponseDTO buscarPorId(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("O ID do estoque é obrigatório.");
        }

        Estoque estoque = estoqueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Estoque não encontrado com o ID: " + id));

        return new EstoqueResponseDTO(estoque);
    }

    @Transactional(readOnly = true)
    public EstoqueResponseDTO buscarPorProdutoId(Integer produtoId) {
        if (produtoId == null) {
            throw new IllegalArgumentException("O ID do produto é obrigatório.");
        }

        Estoque estoque = estoqueRepository.findByProdutoId(produtoId)
                .orElseThrow(() -> new ResourceNotFoundException("Estoque não encontrado para o produto com ID: " + produtoId));

        return new EstoqueResponseDTO(estoque);
    }

    // RF-E13: Alerta de estoque baixo (produtos atingindo ou abaixo do estoque mínimo)
    @Transactional(readOnly = true)
    public List<EstoqueResponseDTO> listarItensComEstoqueBaixo() {
        return estoqueRepository.findItensComEstoqueBaixo()
                .stream()
                .map(EstoqueResponseDTO::new)
                .toList();
    }

    // Configurar / Atualizar estoque mínimo de um produto
    @Transactional
    public EstoqueResponseDTO atualizarEstoqueMinimo(Integer produtoId, Integer novoMinimo) {
        if (produtoId == null) {
            throw new IllegalArgumentException("O ID do produto é obrigatório.");
        }
        if (novoMinimo == null || novoMinimo < 0) {
            throw new IllegalArgumentException("O estoque mínimo não pode ser nulo ou negativo.");
        }

        Estoque estoque = estoqueRepository.findByProdutoId(produtoId)
                .orElseThrow(() -> new ResourceNotFoundException("Estoque não encontrado para o produto com ID: " + produtoId));

        estoque.setEstoqueMinimo(novoMinimo);
        Estoque atualizado = estoqueRepository.save(estoque);

        return new EstoqueResponseDTO(atualizado);
    }

    // ==========================================
    // 2. Operações de Movimentação (Auditoria de Estoque)
    // ==========================================

    // RF-E11 e RF-E12: Registrar movimentação (Entrada por compra ou Saída por venda/uso/perda)
    @Transactional
    public MovimentacaoEstoqueResponseDTO registrarMovimentacao(MovimentacaoEstoqueRequestDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Os dados da movimentação são obrigatórios.");
        }
        if (dto.produtoID() == null) {
            throw new IllegalArgumentException("O ID do produto é obrigatório.");
        }
        if (dto.tipo() == null) {
            throw new IllegalArgumentException("O tipo de movimentação é obrigatório.");
        }
        if (dto.quantidade() == null || dto.quantidade() <= 0) {
            throw new IllegalArgumentException("A quantidade movimentada deve ser maior que zero.");
        }

        Estoque estoque = estoqueRepository.findByProdutoId(dto.produtoID())
                .orElseThrow(() -> new ResourceNotFoundException("Estoque não encontrado para o produto com ID: " + dto.produtoID()));

        int saldoAtual = estoque.getQuantidade() != null ? estoque.getQuantidade() : 0;

        // Processa o saldo conforme o tipo de movimentação
        if (dto.tipo() == MovimentacaoEstoqueEnum.COMPRA_FORNECEDOR) {
            // ENTRADA: soma no saldo
            estoque.setQuantidade(saldoAtual + dto.quantidade());
        } else {
            // SAÍDA (VENDA, USO_INTERNO, PERDA_DANO): valida disponibilidade (RF-E04)
            if (saldoAtual < dto.quantidade()) {
                throw new IllegalArgumentException("Saldo insuficiente em estoque. Saldo atual: "
                        + saldoAtual + ", quantidade solicitada: " + dto.quantidade());
            }
            estoque.setQuantidade(saldoAtual - dto.quantidade());
        }

        // Salva o novo saldo do estoque
        estoqueRepository.save(estoque);

        // Registra o histórico da movimentação (auditoria)
        MovimentacaoEstoque mov = new MovimentacaoEstoque();
        mov.setData(Instant.now());
        mov.setTipo(dto.tipo());
        mov.setQuantidade(dto.quantidade());
        mov.setObservacao(dto.observacao());
        mov.setEstoque(estoque);

        MovimentacaoEstoque movSalva = movimentacaoEstoqueRepository.save(mov);

        return new MovimentacaoEstoqueResponseDTO(movSalva);
    }

    @Transactional(readOnly = true)
    public List<MovimentacaoEstoqueResponseDTO> listarTodasMovimentacoes() {
        return movimentacaoEstoqueRepository.findAll()
                .stream()
                .map(MovimentacaoEstoqueResponseDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public MovimentacaoEstoqueResponseDTO buscarMovimentacaoPorId(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("O ID da movimentação é obrigatório.");
        }

        MovimentacaoEstoque mov = movimentacaoEstoqueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movimentação de estoque não encontrada com o ID: " + id));

        return new MovimentacaoEstoqueResponseDTO(mov);
    }

    // Histórico de movimentações de um produto
    @Transactional(readOnly = true)
    public List<MovimentacaoEstoqueResponseDTO> listarMovimentacoesPorProduto(Integer produtoId) {
        if (produtoId == null) {
            throw new IllegalArgumentException("O ID do produto é obrigatório.");
        }

        return movimentacaoEstoqueRepository.findByEstoqueProdutoIdOrderByDataDesc(produtoId)
                .stream()
                .map(MovimentacaoEstoqueResponseDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MovimentacaoEstoqueResponseDTO> listarMovimentacoesPorTipo(MovimentacaoEstoqueEnum tipo) {
        if (tipo == null) {
            throw new IllegalArgumentException("O tipo de movimentação é obrigatório.");
        }

        return movimentacaoEstoqueRepository.findByTipo(tipo)
                .stream()
                .map(MovimentacaoEstoqueResponseDTO::new)
                .toList();
    }
}
