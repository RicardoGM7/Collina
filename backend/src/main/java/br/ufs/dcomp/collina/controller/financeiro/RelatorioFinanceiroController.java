package br.ufs.dcomp.collina.controller.financeiro;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.ufs.dcomp.collina.dto.financeiro.RelatorioFechamentoResponseDTO;
import br.ufs.dcomp.collina.dto.financeiro.RelatorioInadimplenteResponseDTO;
import br.ufs.dcomp.collina.service.financeiro.RelatorioFinanceiroService;

@RestController
@RequestMapping("/api/financeiro/relatorio") 
@PreAuthorize("hasAnyRole('ADMIN', 'FINANCEIRO')")
public class RelatorioFinanceiroController {
    
    @Autowired 
    private RelatorioFinanceiroService relatorioService;

    @GetMapping("/fechamento")
    public ResponseEntity<RelatorioFechamentoResponseDTO> gerarFechamento(
            @RequestParam String competencia) {
        return ResponseEntity.ok(relatorioService.gerarFechamento(competencia));
    }

    // RF-F15 - Relatório de associados inadimplentes (mensalidades ATRASADAS)
    @GetMapping("/inadimplentes")
    public ResponseEntity<List<RelatorioInadimplenteResponseDTO>> listarInadimplentes() {
        return ResponseEntity.ok(relatorioService.listarInadimplentes());
    }
}
