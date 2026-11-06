package br.ufs.dcomp.collina.service.estoque;

import br.ufs.dcomp.collina.dto.estoque.ProdutoRequestDTO;
import br.ufs.dcomp.collina.dto.estoque.ProdutoResponseDTO;
import br.ufs.dcomp.collina.exception.ResourceNotFoundException;
import br.ufs.dcomp.collina.model.estoque.Categoria;
import br.ufs.dcomp.collina.model.estoque.Estoque;
import br.ufs.dcomp.collina.model.estoque.Produto;
import br.ufs.dcomp.collina.repository.estoque.CategoriaRepository;
import br.ufs.dcomp.collina.repository.estoque.EstoqueRepository;
import br.ufs.dcomp.collina.repository.estoque.ProdutoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ProdutoService {

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private EstoqueRepository estoqueRepository;

    // 1. RF-E09: Cadastrar novo produto e inicializar o Estoque 1:1 automaticamente
    @Transactional
    public ProdutoResponseDTO criarProduto(ProdutoRequestDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Os dados do produto não foram informados.");
        }
        if (dto.nome() == null || dto.nome().trim().isBlank()) {
            throw new IllegalArgumentException("O nome do produto é obrigatório.");
        }
        if (dto.preco() == null || dto.preco() < 0) {
            throw new IllegalArgumentException("O preço do produto não pode ser nulo ou negativo.");
        }

        Produto produto = new Produto();
        produto.setNome(dto.nome().trim());
        produto.setDescricao(dto.descricao() != null ? dto.descricao().trim() : null);
        produto.setPreco(dto.preco());

        // Vincula as categorias informadas (RF-E08) com validação
        if (dto.categoriaIds() != null && !dto.categoriaIds().isEmpty()) {
            List<Categoria> categoriasEncontradas = categoriaRepository.findAllById(dto.categoriaIds());
            if (categoriasEncontradas.size() != dto.categoriaIds().size()) {
                throw new IllegalArgumentException("Uma ou mais categorias informadas não foram encontradas.");
            }
            produto.setCategorias(new HashSet<>(categoriasEncontradas));
        }

        Produto produtoSalvo = produtoRepository.save(produto);

        // Cria e associa o registro de estoque inicial (saldo 0 e estoqueMinimo configurado)
        Estoque estoque = new Estoque();
        estoque.setQuantidade(0);
        estoque.setEstoqueMinimo(dto.estoqueMinimo() != null && dto.estoqueMinimo() >= 0 ? dto.estoqueMinimo() : 0);
        estoque.setProduto(produtoSalvo);

        estoqueRepository.save(estoque);
        produtoSalvo.setEstoque(estoque);

        return new ProdutoResponseDTO(produtoSalvo);
    }

    // 2. Listar todos os produtos
    @Transactional(readOnly = true)
    public List<ProdutoResponseDTO> listarTodos() {
        return produtoRepository.findAll()
                .stream()
                .map(ProdutoResponseDTO::new)
                .toList();
    }

    // 3. Buscar produto por ID
    @Transactional(readOnly = true)
    public ProdutoResponseDTO buscarPorId(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("O ID do produto é obrigatório.");
        }

        Produto produto = produtoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado com o ID: " + id));

        return new ProdutoResponseDTO(produto);
    }

    // 4. Atualizar dados do produto
    @Transactional
    public ProdutoResponseDTO atualizarProduto(Integer id, ProdutoRequestDTO dto) {
        if (id == null) {
            throw new IllegalArgumentException("O ID do produto é obrigatório.");
        }
        if (dto == null) {
            throw new IllegalArgumentException("Os dados do produto para atualização são obrigatórios.");
        }

        Produto produto = produtoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado com o ID: " + id));

        if (dto.nome() != null && !dto.nome().trim().isBlank()) {
            produto.setNome(dto.nome().trim());
        }
        if (dto.descricao() != null) {
            produto.setDescricao(dto.descricao().trim());
        }
        if (dto.preco() != null) {
            if (dto.preco() < 0) {
                throw new IllegalArgumentException("O preço do produto não pode ser negativo.");
            }
            produto.setPreco(dto.preco());
        }

        if (dto.categoriaIds() != null) {
            List<Categoria> categorias = categoriaRepository.findAllById(dto.categoriaIds());
            if (categorias.size() != dto.categoriaIds().size()) {
                throw new IllegalArgumentException("Uma ou mais categorias informadas não foram encontradas.");
            }
            produto.setCategorias(new HashSet<>(categorias));
        }

        // Atualiza estoque mínimo se fornecido
        if (dto.estoqueMinimo() != null) {
            if (dto.estoqueMinimo() < 0) {
                throw new IllegalArgumentException("O estoque mínimo não pode ser negativo.");
            }
            if (produto.getEstoque() != null) {
                produto.getEstoque().setEstoqueMinimo(dto.estoqueMinimo());
                estoqueRepository.save(produto.getEstoque());
            }
        }

        Produto produtoAtualizado = produtoRepository.save(produto);
        return new ProdutoResponseDTO(produtoAtualizado);
    }

    // 5. Remover produto
    @Transactional
    public void removerProduto(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("O ID do produto é obrigatório.");
        }

        Produto produto = produtoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado com o ID: " + id));

        // Regra de integridade 1: não remover produto já vinculado a pedidos
        if (produto.getItemPedidoList() != null && !produto.getItemPedidoList().isEmpty()) {
            throw new IllegalStateException("Não é possível remover produto já vinculado a pedidos existentes.");
        }

        // Regra de integridade 2: não remover produto que possui histórico de movimentação no estoque
        if (produto.getEstoque() != null) {
            if (produto.getEstoque().getMovimentacaoEstoque() != null && !produto.getEstoque().getMovimentacaoEstoque().isEmpty()) {
                throw new IllegalStateException("Não é possível remover produto que possui histórico de movimentações no estoque.");
            }
            Estoque estoque = produto.getEstoque();
            produto.setEstoque(null);
            estoqueRepository.delete(estoque);
        }

        produtoRepository.delete(produto);
    }
}
