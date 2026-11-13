package br.ufs.dcomp.collina.controller.financeiro;

import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import br.ufs.dcomp.collina.service.financeiro.MovimentacaoFinanceiraService;
import br.ufs.dcomp.collina.dto.financeiro.MovimentacaoFinanceiraResponseDTO;
import br.ufs.dcomp.collina.dto.financeiro.MovimentacaoFinanceiraRequestDTO;

@RestController
@RequestMapping("/api/financeiro/movimentacoes")
public class MovimentacaoFinanceiraController {

    @Autowired 
    private MovimentacaoFinanceiraService movimentacaoService;

    // Registrar Movimentacao
    @PostMapping
    public ResponseEntity<MovimentacaoFinanceiraResponseDTO> registrar(
            @RequestBody MovimentacaoFinanceiraRequestDTO dados, 
            @RequestHeader("funcionarioId") Long funcionarioId) {

        MovimentacaoFinanceiraResponseDTO response = movimentacaoService.registrar(dados, funcionarioId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    // Editar movimentacao
    @PutMapping("/{id}")
    public ResponseEntity<MovimentacaoFinanceiraResponseDTO> editar(
            @PathVariable Long id,
            @RequestBody MovimentacaoFinanceiraRequestDTO dados,
            @RequestHeader("funcionarioId") Long funcionarioId) {

        MovimentacaoFinanceiraResponseDTO response = movimentacaoService.editar(id, dados, funcionarioId);
        return ResponseEntity.ok(response);
    }

    // Remover movimentacao
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelar(
            @PathVariable Long id,
            @RequestParam String motivo,
            @RequestHeader("funcionarioId") Long funcionarioId) {

        movimentacaoService.cancelar(id, motivo, funcionarioId);
        return ResponseEntity.noContent().build();
    }
}
