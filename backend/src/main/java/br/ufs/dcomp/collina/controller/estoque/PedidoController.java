package br.ufs.dcomp.collina.controller.estoque;

import br.ufs.dcomp.collina.dto.estoque.PagamentoPedidoResponseDTO;
import br.ufs.dcomp.collina.dto.estoque.PedidoRequestDTO;
import br.ufs.dcomp.collina.dto.estoque.PedidoResponseDTO;
import br.ufs.dcomp.collina.service.estoque.PedidoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    @Autowired
    private PedidoService pedidoService;

    // ==========================================
    // 1. Ciclo de Vida do Pedido
    // ==========================================

    // RF-E01: Criar novo pedido (201 Created)
    @PostMapping
    public ResponseEntity<PedidoResponseDTO> criar(@Valid @RequestBody PedidoRequestDTO dto) {
        PedidoResponseDTO novoPedido = pedidoService.criarPedido(dto);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(novoPedido.id())
                .toUri();

        return ResponseEntity.created(location).body(novoPedido);
    }

    // RF-E16 e RF-E17: Listar todos os pedidos (200 OK)
    @GetMapping
    public ResponseEntity<List<PedidoResponseDTO>> listarTodos() {
        return ResponseEntity.ok(pedidoService.listarTodos());
    }

    // Buscar pedido por ID (200 OK)
    @GetMapping("/{id}")
    public ResponseEntity<PedidoResponseDTO> buscarPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(pedidoService.buscarPorId(id));
    }

    // RF-E07: Cancelar pedido (200 OK)
    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<PedidoResponseDTO> cancelar(@PathVariable Integer id) {
        return ResponseEntity.ok(pedidoService.cancelarPedido(id));
    }

    // ==========================================
    // 2. Gerenciamento de Itens do Pedido
    // ==========================================

    // RF-E02: Adicionar item ao pedido (201 Created)
    @PostMapping("/{id}/itens")
    public ResponseEntity<PedidoResponseDTO.ItemPedidoResponseDTO> adicionarItem(
            @PathVariable Integer id,
            @RequestParam Integer produtoId,
            @RequestParam Integer quantidade) {
        PedidoResponseDTO.ItemPedidoResponseDTO itemSalvo = pedidoService.adicionarItemAoPedido(id, produtoId, quantidade);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{itemId}")
                .buildAndExpand(itemSalvo.produtoId())
                .toUri();

        return ResponseEntity.created(location).body(itemSalvo);
    }

    // RF-E14: Alterar quantidade de um item do pedido (200 OK)
    @PatchMapping("/{id}/itens/{itemId}")
    public ResponseEntity<PedidoResponseDTO.ItemPedidoResponseDTO> alterarQuantidadeItem(
            @PathVariable Integer id,
            @PathVariable Integer itemId,
            @RequestParam Integer novaQuantidade) {
        return ResponseEntity.ok(pedidoService.alterarQuantidadeItem(id, itemId, novaQuantidade));
    }

    // RF-E15: Remover item de um pedido (204 No Content)
    @DeleteMapping("/{id}/itens/{itemId}")
    public ResponseEntity<Void> removerItem(
            @PathVariable Integer id,
            @PathVariable Integer itemId) {
        pedidoService.removerItem(id, itemId);
        return ResponseEntity.noContent().build();
    }

    // Listar itens de um pedido (200 OK)
    @GetMapping("/{id}/itens")
    public ResponseEntity<List<PedidoResponseDTO.ItemPedidoResponseDTO>> listarItens(@PathVariable Integer id) {
        return ResponseEntity.ok(pedidoService.listarItensPorPedido(id));
    }

    // ==========================================
    // 3. Pagamento do Pedido
    // ==========================================

    // RF-E05 e RF-E06: Confirmar pagamento e dar baixa automática de estoque (200 OK)
    @PostMapping("/{id}/pagar")
    public ResponseEntity<PagamentoPedidoResponseDTO> pagar(@PathVariable Integer id) {
        return ResponseEntity.ok(pedidoService.confirmarPagamento(id));
    }

    // Consultar dados do pagamento do pedido (200 OK)
    @GetMapping("/{id}/pagamento")
    public ResponseEntity<PagamentoPedidoResponseDTO> buscarPagamento(@PathVariable Integer id) {
        return ResponseEntity.ok(pedidoService.buscarPagamentoPorPedidoId(id));
    }
}
