package br.ufs.dcomp.collina.controller.associados;

import br.ufs.dcomp.collina.dto.associados.AlterarPlanoRequestDTO;
import br.ufs.dcomp.collina.dto.associados.AlterarSituacaoRequestDTO;
import br.ufs.dcomp.collina.dto.associados.AssociadoRequestDTO;
import br.ufs.dcomp.collina.dto.associados.AssociadoResponseDTO;
import br.ufs.dcomp.collina.dto.associados.AssociadoUpdateDTO;
import br.ufs.dcomp.collina.model.associados.SituacaoCadastralEnum;
import br.ufs.dcomp.collina.service.associados.AssociadoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/associados")
public class AssociadoController {

    @Autowired
    private AssociadoService associadoService;

    // 1. Cadastrar associado titular (201 Created)
    @PostMapping
    public ResponseEntity<AssociadoResponseDTO> cadastrar(@Valid @RequestBody AssociadoRequestDTO dto) {
        AssociadoResponseDTO novo = associadoService.cadastrar(dto);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(novo.id())
                .toUri();

        return ResponseEntity.created(location).body(novo);
    }

    // 2. Listar associados, com filtro opcional ?situacao=ATIVO|SUSPENSO|INATIVO (200 OK)
    @GetMapping
    public ResponseEntity<List<AssociadoResponseDTO>> listar(@RequestParam(required = false) SituacaoCadastralEnum situacao) {
        return ResponseEntity.ok(associadoService.listar(situacao));
    }

    // 3. Buscar por ID (200 OK)
    @GetMapping("/{id}")
    public ResponseEntity<AssociadoResponseDTO> buscarPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(associadoService.buscarPorId(id));
    }

    // 4. Buscar por CPF (200 OK)
    @GetMapping("/cpf/{cpf}")
    public ResponseEntity<AssociadoResponseDTO> buscarPorCpf(@PathVariable String cpf) {
        return ResponseEntity.ok(associadoService.buscarPorCpf(cpf));
    }

    // 5. RF-AS05: titular atualiza seus dados pessoais (200 OK)
    @PutMapping("/{id}")
    public ResponseEntity<AssociadoResponseDTO> atualizarDadosPessoais(@PathVariable Integer id,
                                                                       @Valid @RequestBody AssociadoUpdateDTO dto) {
        return ResponseEntity.ok(associadoService.atualizarDadosPessoais(id, dto));
    }

    // 6. Alterar plano/categoria - funcionário autorizado (200 OK)
    @PatchMapping("/{id}/plano")
    public ResponseEntity<AssociadoResponseDTO> alterarPlano(@PathVariable Integer id,
                                                             @Valid @RequestBody AlterarPlanoRequestDTO dto) {
        return ResponseEntity.ok(associadoService.alterarPlano(id, dto.tipoPlano()));
    }

    // 7. RF-AS06: alterar situação cadastral - funcionário autorizado (200 OK)
    @PatchMapping("/{id}/situacao")
    public ResponseEntity<AssociadoResponseDTO> alterarSituacao(@PathVariable Integer id,
                                                                @Valid @RequestBody AlterarSituacaoRequestDTO dto) {
        return ResponseEntity.ok(associadoService.alterarSituacao(id, dto.situacao()));
    }
}
