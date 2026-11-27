package br.ufs.dcomp.collina.controller.financeiro;

import java.time.LocalDate;
import java.time.YearMonth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.ufs.dcomp.collina.dto.financeiro.ResumoFechamentoDTO;
import br.ufs.dcomp.collina.service.financeiro.FechamentoMensalidadeService;

@RestController
@RequestMapping("/api/financeiro/fechamento")
public class FechamentoMensalidadeController {

    @Autowired
    private FechamentoMensalidadeService fechamentoService;

    // RF-F05/F20: reexecução do fechamento mensal (200 OK).
    // As capturas de data corrente ficam agrupadas no controller e passadas por parâmetro ao service.
    // O vencimento padrão utiliza o último dia útil do mês (segunda a sexta-feira).
    @PostMapping
    public ResponseEntity<ResumoFechamentoDTO> executar(
            @RequestParam(name = "competencia", required = false) YearMonth competencia,
            @RequestParam(name = "vencimento", required = false) LocalDate vencimento) {
        YearMonth comp = (competencia != null) ? competencia : YearMonth.now();
        LocalDate venc = (vencimento != null) ? vencimento : FechamentoMensalidadeService.calcularUltimoDiaUtil(comp);
        return ResponseEntity.ok(fechamentoService.fecharEAbrirCompetencia(comp, venc));
    }

    @GetMapping("/ultimo")
    public ResponseEntity<ResumoFechamentoDTO> ultimo() {
        return fechamentoService.buscarUltimoFechamento()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
