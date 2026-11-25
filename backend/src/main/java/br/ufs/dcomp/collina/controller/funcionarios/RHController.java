package br.ufs.dcomp.collina.controller.funcionarios;

import br.ufs.dcomp.collina.dto.funcionarios.*;
import br.ufs.dcomp.collina.service.funcionarios.RHService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/rh")
public class RHController {

    @Autowired
    private RHService rhService;

    // ==========================================
    // 1. Presença e Ponto (RF-FU08)
    // ==========================================

    @PostMapping("/presencas")
    public ResponseEntity<PresencaResponseDTO> registrarPresenca(@Valid @RequestBody PresencaRequestDTO dto) {
        PresencaResponseDTO presenca = rhService.registrarPresenca(dto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(presenca.id())
                .toUri();
        return ResponseEntity.created(location).body(presenca);
    }

    @PostMapping("/ausencias")
    public ResponseEntity<PresencaResponseDTO> registrarAusencia(@Valid @RequestBody AusenciaRequestDTO dto) {
        return ResponseEntity.ok(rhService.registrarAusencia(dto));
    }

    @GetMapping("/presencas/alocacao/{alocacaoId}")
    public ResponseEntity<PresencaResponseDTO> buscarPresencaPorAlocacao(@PathVariable Integer alocacaoId) {
        return ResponseEntity.ok(rhService.buscarPresencaPorAlocacao(alocacaoId));
    }

    // ==========================================
    // 2. Relatório de Presença (RF-FU09)
    // ==========================================

    @GetMapping("/relatorio")
    public ResponseEntity<RelatorioPresencaResponseDTO> gerarRelatorio(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {
        return ResponseEntity.ok(rhService.gerarRelatorio(dataInicio, dataFim));
    }

    // ==========================================
    // 3. Férias e Afastamentos (RF-FU10, RF-FU11)
    // ==========================================

    @PostMapping("/ferias")
    public ResponseEntity<FeriasResponseDTO> registrarFerias(@Valid @RequestBody FeriasRequestDTO dto) {
        FeriasResponseDTO ferias = rhService.registrarFerias(dto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(ferias.id())
                .toUri();
        return ResponseEntity.created(location).body(ferias);
    }

    @GetMapping("/ferias/funcionario/{funcionarioId}")
    public ResponseEntity<List<FeriasResponseDTO>> listarFeriasPorFuncionario(@PathVariable Integer funcionarioId) {
        return ResponseEntity.ok(rhService.listarFeriasPorFuncionario(funcionarioId));
    }

    @PostMapping("/afastamentos")
    public ResponseEntity<AfastamentoResponseDTO> registrarAfastamento(@Valid @RequestBody AfastamentoRequestDTO dto) {
        AfastamentoResponseDTO afastamento = rhService.registrarAfastamento(dto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(afastamento.id())
                .toUri();
        return ResponseEntity.created(location).body(afastamento);
    }

    @GetMapping("/afastamentos/funcionario/{funcionarioId}")
    public ResponseEntity<List<AfastamentoResponseDTO>> listarAfastamentosPorFuncionario(@PathVariable Integer funcionarioId) {
        return ResponseEntity.ok(rhService.listarAfastamentosPorFuncionario(funcionarioId));
    }

    // ==========================================
    // 4. Consulta de Disponibilidade (RF-FU12)
    // ==========================================

    @GetMapping("/disponibilidade")
    public ResponseEntity<DisponibilidadeResponseDTO> consultarDisponibilidade(
            @RequestParam Integer funcionarioId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return ResponseEntity.ok(rhService.consultarDisponibilidade(funcionarioId, data));
    }
}
