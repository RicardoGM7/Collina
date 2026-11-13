package br.ufs.dcomp.collina.controller.financeiro;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import br.ufs.dcomp.collina.service.financeiro.MovimentacaoFinanceiraService;
import br.ufs.dcomp.collina.dto.financeiro.MovimentacaoFinanceiraResponseDTO;
import br.ufs.dcomp.collina.dto.financeiro.FiltroMovimentacaoRequestDTO;
import br.ufs.dcomp.collina.dto.financeiro.SaldoClubeResponseDTO;

import java.util.List;

@RestController
@RequestMapping("/api/financeiro/consulta-operacoes")
public class ConsultaOperacoesController {
    
    @Autowired 
    private MovimentacaoFinanceiraService movimentacaoService;

    @GetMapping("/historico")
    public ResponseEntity<List<MovimentacaoFinanceiraResponseDTO>> listarHistorico (
                                    FiltroMovimentacaoRequestDTO filtro) {
        return ResponseEntity.ok(movimentacaoService.listarHistorico(filtro));
    }
    
    @GetMapping("/saldo")
    public ResponseEntity<SaldoClubeResponseDTO> consultarSaldo() {
        return ResponseEntity.ok(movimentacaoService.consultarSaldo());
    }
}
