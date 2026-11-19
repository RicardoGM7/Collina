package br.ufs.dcomp.collina.controller.financeiro;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.ufs.dcomp.collina.dto.financeiro.ResumoFechamentoDTO;
import br.ufs.dcomp.collina.service.financeiro.FechamentoMensalidadeService;

@RestController
@RequestMapping("/api/financeiro/fechamento")
public class FechamentoMensalidadeController {

    @Autowired
    private FechamentoMensalidadeService fechamentoService;

    // RF-F05/F20: reexecução manual do fechamento mensal (200 OK), para o caso de o agendador não ter
    // rodado (aplicação fora do ar no dia 1) ou ter terminado com falhas. Sempre atua na competência
    // corrente e é idempotente: titulares já processados são ignorados, nada é duplicado.
    // TODO: restringir a perfis administrativos quando a autenticação (Firebase) estiver integrada.
    @PostMapping
    public ResponseEntity<ResumoFechamentoDTO> executar() {
        return ResponseEntity.ok(fechamentoService.fecharCompetenciaAtual());
    }
}