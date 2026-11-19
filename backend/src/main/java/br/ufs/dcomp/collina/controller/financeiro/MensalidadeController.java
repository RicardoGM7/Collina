package br.ufs.dcomp.collina.controller.financeiro;

import java.time.YearMonth;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.ufs.dcomp.collina.dto.financeiro.GastoAvulsoRequestDTO;
import br.ufs.dcomp.collina.dto.financeiro.MensalidadeResponseDTO;
import br.ufs.dcomp.collina.service.financeiro.MensalidadeService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/mensalidades")
public class MensalidadeController {

    @Autowired
    private MensalidadeService mensalidadeService;

    // RF-F06: listar as mensalidades de um titular em uma competência (200 OK).
    // Ex.: GET /api/mensalidades?associadoId=1&competencia=2026-11
    // Devolve lista vazia se não houver nenhuma. Erros (tratados pelo GlobalExceptionHandler):
    //   400 -> parâmetro ausente ou em formato inválido (competencia deve ser AAAA-MM)
    @GetMapping
    public ResponseEntity<List<MensalidadeResponseDTO>> listar(
            @RequestParam("associadoId") Long associadoId,
            @RequestParam("competencia") YearMonth competencia) {
        return ResponseEntity.ok(mensalidadeService.listarMensalidades(associadoId, competencia));
    }

    // RF-F08: lançar gasto avulso (201 Created). Titular ou dependente: o gasto vai para a mensalidade
    // EM_ABERTO do titular responsável. Devolve a mensalidade atualizada. Erros possíveis:
    //   400 -> corpo inválido (@Valid), header ausente ou dado recusado pela entidade
    //   404 -> não há mensalidade em aberto para o titular
    //   409 -> mensalidade fora de EM_ABERTO
    // O funcionarioId vem no header, como nos demais controllers do financeiro (provisório até a autenticação).
    @PostMapping("/gastos-avulsos")
    public ResponseEntity<MensalidadeResponseDTO> incluirGastoAvulso(
            @Valid @RequestBody GastoAvulsoRequestDTO dto,
            @RequestHeader("funcionarioId") Long funcionarioId) {
        MensalidadeResponseDTO mensalidade = mensalidadeService.incluirGastoAvulso(dto, funcionarioId);
        return ResponseEntity.status(HttpStatus.CREATED).body(mensalidade);
    }
}