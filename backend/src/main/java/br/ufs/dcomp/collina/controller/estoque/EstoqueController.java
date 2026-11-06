package br.ufs.dcomp.collina.controller.estoque;

import br.ufs.dcomp.collina.dto.estoque.EstoqueResponseDTO;
import br.ufs.dcomp.collina.dto.estoque.MovimentacaoEstoqueRequestDTO;
import br.ufs.dcomp.collina.dto.estoque.MovimentacaoEstoqueResponseDTO;
import br.ufs.dcomp.collina.model.estoque.MovimentacaoEstoqueEnum;
import br.ufs.dcomp.collina.service.estoque.EstoqueService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/estoques")
public class EstoqueController {

    @Autowired
    private EstoqueService estoqueService;

    // ==========================================
    // 1. Endpoints de Saldo de Estoque
    // ==========================================

    // Listar todos os estoques (200 OK)
    @GetMapping
    public ResponseEntity<List<EstoqueResponseDTO>> listarTodos() {
        return ResponseEntity.ok(estoqueService.listarTodos());
    }

    // Buscar estoque por ID próprio (200 OK)
    @GetMapping("/{id}")
    public ResponseEntity<EstoqueResponseDTO> buscarPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(estoqueService.buscarPorId(id));
    }

    // Buscar estoque pelo ID do Produto (200 OK)
    @GetMapping("/produto/{produtoId}")
    public ResponseEntity<EstoqueResponseDTO> buscarPorProdutoId(@PathVariable Integer produtoId) {
        return ResponseEntity.ok(estoqueService.buscarPorProdutoId(produtoId));
    }

    // RF-E13: Alerta de estoque baixo (200 OK)
    @GetMapping("/baixo")
    public ResponseEntity<List<EstoqueResponseDTO>> listarItensComEstoqueBaixo() {
        return ResponseEntity.ok(estoqueService.listarItensComEstoqueBaixo());
    }

    // Atualizar estoque mínimo de um produto (200 OK)
    @PatchMapping("/produto/{produtoId}/minimo")
    public ResponseEntity<EstoqueResponseDTO> atualizarEstoqueMinimo(
            @PathVariable Integer produtoId,
            @RequestParam Integer novoMinimo) {
        return ResponseEntity.ok(estoqueService.atualizarEstoqueMinimo(produtoId, novoMinimo));
    }

    // ==========================================
    // 2. Endpoints de Movimentação de Estoque
    // ==========================================

    // RF-E11 e RF-E12: Registrar movimentação manual de estoque (201 Created)
    @PostMapping("/movimentar")
    public ResponseEntity<MovimentacaoEstoqueResponseDTO> registrarMovimentacao(@Valid @RequestBody MovimentacaoEstoqueRequestDTO dto) {
        MovimentacaoEstoqueResponseDTO mov = estoqueService.registrarMovimentacao(dto);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(mov.id())
                .toUri();

        return ResponseEntity.created(location).body(mov);
    }

    // Listar todas as movimentações da loja (200 OK)
    @GetMapping("/movimentacoes")
    public ResponseEntity<List<MovimentacaoEstoqueResponseDTO>> listarTodasMovimentacoes() {
        return ResponseEntity.ok(estoqueService.listarTodasMovimentacoes());
    }

    // Buscar movimentação por ID (200 OK)
    @GetMapping("/movimentacoes/{id}")
    public ResponseEntity<MovimentacaoEstoqueResponseDTO> buscarMovimentacaoPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(estoqueService.buscarMovimentacaoPorId(id));
    }

    // Histórico de movimentações de um produto específico (200 OK)
    @GetMapping("/produto/{produtoId}/historico")
    public ResponseEntity<List<MovimentacaoEstoqueResponseDTO>> listarHistoricoDoProduto(@PathVariable Integer produtoId) {
        return ResponseEntity.ok(estoqueService.listarMovimentacoesPorProduto(produtoId));
    }

    // Listar movimentações filtradas por tipo (ex: COMPRA_FORNECEDOR, VENDA, etc.) (200 OK)
    @GetMapping("/movimentacoes/tipo/{tipo}")
    public ResponseEntity<List<MovimentacaoEstoqueResponseDTO>> listarPorTipo(@PathVariable MovimentacaoEstoqueEnum tipo) {
        return ResponseEntity.ok(estoqueService.listarMovimentacoesPorTipo(tipo));
    }
}
