package br.ufs.dcomp.collina.controller.funcionarios;

import br.ufs.dcomp.collina.dto.funcionarios.*;
import br.ufs.dcomp.collina.service.funcionarios.ContratoService;
import br.ufs.dcomp.collina.service.funcionarios.FuncionarioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/funcionarios")
public class FuncionarioController {

    @Autowired
    private FuncionarioService funcionarioService;

    @Autowired
    private ContratoService contratoService;

    // ==========================================
    // 1. Dados Pessoais e Cadastrais (RF-FU01, RF-FU02, RF-FU03)
    // ==========================================

    @PostMapping
    public ResponseEntity<FuncionarioResponseDTO> cadastrarFuncionario(@Valid @RequestBody FuncionarioRequestDTO dto) {
        FuncionarioResponseDTO cadastrado = funcionarioService.cadastrar(dto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(cadastrado.id())
                .toUri();
        return ResponseEntity.created(location).body(cadastrado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FuncionarioResponseDTO> alterarFuncionario(@PathVariable Integer id,
                                                                    @Valid @RequestBody FuncionarioRequestDTO dto) {
        return ResponseEntity.ok(funcionarioService.alterar(id, dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FuncionarioResponseDTO> consultarFuncionario(@PathVariable Integer id) {
        return ResponseEntity.ok(funcionarioService.consultar(id));
    }

    @GetMapping
    public ResponseEntity<List<FuncionarioResponseDTO>> listarTodos() {
        return ResponseEntity.ok(funcionarioService.listarTodos());
    }

    // ==========================================
    // 2. Relação Contratual e Rescisão (RF-FU04, RF-FU14)
    // ==========================================

    @PostMapping("/contratos")
    public ResponseEntity<RelacaoContratualResponseDTO> cadastrarContrato(@Valid @RequestBody RelacaoContratualRequestDTO dto) {
        RelacaoContratualResponseDTO contrato = contratoService.cadastrarContrato(dto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(contrato.id())
                .toUri();
        return ResponseEntity.created(location).body(contrato);
    }

    @GetMapping("/{id}/contratos")
    public ResponseEntity<List<RelacaoContratualResponseDTO>> consultarContrato(@PathVariable Integer id) {
        return ResponseEntity.ok(contratoService.consultarHistorico(id));
    }

    @PatchMapping("/contratos/{id}/encerrar")
    public ResponseEntity<RescisaoResponseDTO> encerrarContrato(@PathVariable Integer id,
                                                                @Valid @RequestBody EncerramentoContratoDTO dto) {
        return ResponseEntity.ok(contratoService.encerrarContrato(id, dto));
    }
}
