package br.ufs.dcomp.collina.controller.financeiro;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.beans.factory.annotation.Autowired;
import br.ufs.dcomp.collina.service.financeiro.RelatorioFinanceiroService;
import br.ufs.dcomp.collina.dto.financeiro.RelatorioFechamentoResponseDTO;

@RestController
@RequestMapping("/api/financeiro/relatorio") 
public class RelatorioFinanceiroController {
    
    @Autowired 
    private RelatorioFinanceiroService relatorioService;

    @GetMapping("/fechamento")
    public ResponseEntity<RelatorioFechamentoResponseDTO> gerarFechamento (
        @RequestParam String competencia) {
            return ResponseEntity.ok(relatorioService.gerarFechamento(competencia));
        }
}
