package br.ufs.dcomp.collina.controller.financeiro;

import br.ufs.dcomp.collina.dto.financeiro.SolicitacaoGastoRequestDTO;
import br.ufs.dcomp.collina.dto.financeiro.SolicitacaoGastoResponseDTO;
import br.ufs.dcomp.collina.service.financeiro.SolicitacaoGastoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/financeiro/solicitacoes-gasto")
public class SolicitacaoGastoController {

    @Autowired
    private SolicitacaoGastoService solicitacaoService;

    @PostMapping
    public ResponseEntity<SolicitacaoGastoResponseDTO> cadastrar(
            @Valid @RequestBody SolicitacaoGastoRequestDTO dados,
            @RequestHeader("funcionarioId") Long funcionarioId) {
        
        SolicitacaoGastoResponseDTO response = solicitacaoService.cadastrarDados(dados, funcionarioId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/prioridade")
    public ResponseEntity<List<SolicitacaoGastoResponseDTO>> listarPorPrioridade() {
        
        List<SolicitacaoGastoResponseDTO> response = solicitacaoService.listarPorPrioridade();
        return ResponseEntity.ok(response);
    }
}