package br.ufs.dcomp.collina.controller.associados;

import br.ufs.dcomp.collina.dto.associados.AlterarSituacaoRequestDTO;
import br.ufs.dcomp.collina.dto.associados.AssociadoRequestDTO;
import br.ufs.dcomp.collina.dto.associados.AssociadoResponseDTO;
import br.ufs.dcomp.collina.dto.associados.AssociadoUpdateDTO;
import br.ufs.dcomp.collina.dto.associados.VinculoDependenciaResponseDTO;
import br.ufs.dcomp.collina.model.associados.SituacaoCadastralEnum;
import br.ufs.dcomp.collina.model.associados.StatusVinculoEnum;
import br.ufs.dcomp.collina.model.auth.UsuarioAutenticado;
import br.ufs.dcomp.collina.service.associados.AssociadoService;
import br.ufs.dcomp.collina.service.associados.VinculoDependenciaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/associados")
public class AssociadoController {

    @Autowired
    private AssociadoService associadoService;

    @Autowired
    private VinculoDependenciaService vinculoService;

    // ===== Endpoints do próprio associado (perfil ASSOCIADO). O titular é identificado pelo token (uid),
    // nunca por um ID enviado pelo cliente, então um associado não acessa dados de outro (UC-ASSO01, RNF07).

    // 0.1 Dados do associado autenticado
    @GetMapping("/me")
    public ResponseEntity<AssociadoResponseDTO> buscarMeuPerfil(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ResponseEntity.ok(associadoService.buscarPorUsuarioId(usuario.uid()));
    }

    // 0.1b Autocadastro do associado titular autenticado (vincula automaticamente ao uid do token)
    @PostMapping("/me")
    public ResponseEntity<AssociadoResponseDTO> cadastrarMeuPerfil(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody AssociadoRequestDTO dto) {
        AssociadoRequestDTO comUid = new AssociadoRequestDTO(
                dto.nome(),
                dto.cpf(),
                dto.dataNascimento(),
                dto.categoria(),
                dto.tipoPlano(),
                usuario.uid()
        );
        AssociadoResponseDTO novo = associadoService.cadastrar(comUid);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().build().toUri();
        return ResponseEntity.created(location).body(novo);
    }

    // 0.2 RF-AS05: o titular altera os próprios dados pessoais
    @PutMapping("/me")
    public ResponseEntity<AssociadoResponseDTO> atualizarMeusDados(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                                                   @Valid @RequestBody AssociadoUpdateDTO dto) {
        return ResponseEntity.ok(associadoService.atualizarMeusDados(usuario.uid(), dto));
    }

    // 0.3 RF-AS05/AS08: o titular consulta (sem alterar) os próprios dependentes
    @GetMapping("/me/dependentes")
    public ResponseEntity<List<VinculoDependenciaResponseDTO>> listarMeusDependentes(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(required = false) StatusVinculoEnum status) {
        Integer titularId = associadoService.obterIdPorUsuarioId(usuario.uid());
        return ResponseEntity.ok(vinculoService.listarPorTitular(titularId, status));
    }

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

    // 6. RF-AS06: alterar situação cadastral - funcionário autorizado (200 OK)
    @PatchMapping("/{id}/situacao")
    public ResponseEntity<AssociadoResponseDTO> alterarSituacao(@PathVariable Integer id,
                                                                @Valid @RequestBody AlterarSituacaoRequestDTO dto) {
        return ResponseEntity.ok(associadoService.alterarSituacao(id, dto.situacao()));
    }
}
