package br.ufs.dcomp.collina.controller.financeiro;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.ufs.dcomp.collina.dto.financeiro.CancelamentoDeMensalidadeRequestDTO;
import br.ufs.dcomp.collina.dto.financeiro.MensalidadeResponseDTO;
import br.ufs.dcomp.collina.service.financeiro.CancelamentoMensalidadeService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/mensalidades")
public class CancelamentoDeMensalidadeController {

    @Autowired
    private CancelamentoMensalidadeService cancelamentoService;

    // UC-FIN05 / RF-F11: cancelar uma mensalidade PENDENTE ou ATRASADA e reemiti-la (200 OK).
    // Devolve a mensalidade SUBSTITUTA. Erros possíveis (tratados pelo GlobalExceptionHandler):
    //   400 -> corpo inválido (@Valid) ou dado recusado pela entidade
    //   404 -> mensalidade inexistente
    //   409 -> mensalidade em estado que não permite cancelamento
    @PostMapping("/{id}/cancelar-e-reemitir")
    public ResponseEntity<MensalidadeResponseDTO> cancelarEReemitir(
            @PathVariable Long id,
            @Valid @RequestBody CancelamentoDeMensalidadeRequestDTO dto) {
        return ResponseEntity.ok(cancelamentoService.cancelarEReemitir(dto, id));
    }
}
