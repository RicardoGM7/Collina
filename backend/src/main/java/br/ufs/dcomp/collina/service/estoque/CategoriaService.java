package br.ufs.dcomp.collina.service.estoque;

import br.ufs.dcomp.collina.dto.estoque.CategoriaRequestDTO;
import br.ufs.dcomp.collina.dto.estoque.CategoriaResponseDTO;
import br.ufs.dcomp.collina.exception.ResourceNotFoundException;
import br.ufs.dcomp.collina.model.estoque.Categoria;
import br.ufs.dcomp.collina.repository.estoque.CategoriaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoriaService {

    @Autowired
    private CategoriaRepository categoriaRepository;

    // 1. Cadastrar nova categoria
    @Transactional
    public CategoriaResponseDTO adicionarCategoria(CategoriaRequestDTO dto) {
        if (dto == null || dto.nome() == null || dto.nome().trim().isBlank()) {
            throw new IllegalArgumentException("O nome da categoria é obrigatório.");
        }

        String nomeFormatado = dto.nome().trim();

        if (categoriaRepository.existsByNomeIgnoreCase(nomeFormatado)) {
            throw new IllegalArgumentException("Já existe uma categoria cadastrada com o nome: " + nomeFormatado);
        }

        Categoria novaCategoria = new Categoria();
        novaCategoria.setNome(nomeFormatado);

        Categoria categoriaSalva = categoriaRepository.save(novaCategoria);
        return new CategoriaResponseDTO(categoriaSalva);
    }

    // 2. Listar todas as categorias (findAll)
    @Transactional(readOnly = true)
    public List<CategoriaResponseDTO> listarTodas() {
        return categoriaRepository.findAll()
                .stream()
                .map(CategoriaResponseDTO::new)
                .toList();
    }

    // 3. Buscar categoria por ID (findById)
    @Transactional(readOnly = true)
    public CategoriaResponseDTO buscarPorId(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("O ID da categoria é obrigatório.");
        }

        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada com o ID: " + id));

        return new CategoriaResponseDTO(categoria);
    }

    // 4. Editar / Atualizar categoria
    @Transactional
    public CategoriaResponseDTO editarCategoria(Integer id, CategoriaRequestDTO dto) {
        if (id == null) {
            throw new IllegalArgumentException("O ID da categoria é obrigatório.");
        }
        if (dto == null || dto.nome() == null || dto.nome().trim().isBlank()) {
            throw new IllegalArgumentException("O novo nome da categoria é obrigatório.");
        }

        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada com o ID: " + id));

        String novoNome = dto.nome().trim();

        // Se o nome foi alterado, valida se o novo nome já não está em uso por outra categoria
        if (!categoria.getNome().equalsIgnoreCase(novoNome) && categoriaRepository.existsByNomeIgnoreCase(novoNome)) {
            throw new IllegalArgumentException("Já existe outra categoria cadastrada com o nome: " + novoNome);
        }

        categoria.setNome(novoNome);
        Categoria categoriaAtualizada = categoriaRepository.save(categoria);

        return new CategoriaResponseDTO(categoriaAtualizada);
    }

    // 5. Remover categoria por ID (delete)
    @Transactional
    public void removerCategoria(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("O ID da categoria é obrigatório.");
        }

        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada com o ID: " + id));

        // Regra de integridade: não permitir deletar categoria se houver produtos associados
        if (categoria.getProdutos() != null && !categoria.getProdutos().isEmpty()) {
            throw new IllegalStateException("Não é possível remover uma categoria que possui produtos vinculados.");
        }

        categoriaRepository.delete(categoria);
    }
}
