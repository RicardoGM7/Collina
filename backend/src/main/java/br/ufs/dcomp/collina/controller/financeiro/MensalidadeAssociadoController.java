package br.ufs.dcomp.collina.controller.financeiro;

import java.time.YearMonth;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.ufs.dcomp.collina.dto.financeiro.MensalidadeResponseDTO;
import br.ufs.dcomp.collina.dto.financeiro.PagamentoResponseDTO;
import br.ufs.dcomp.collina.exception.ResourceNotFoundException;
import br.ufs.dcomp.collina.model.associados.Associado;
import br.ufs.dcomp.collina.model.auth.UsuarioAutenticado;
import br.ufs.dcomp.collina.repository.associados.AssociadoRepository;
import br.ufs.dcomp.collina.service.financeiro.MensalidadeService;
import br.ufs.dcomp.collina.service.financeiro.PagamentoService;

/**
 * Endpoints financeiros acessíveis pelo próprio associado titular autenticado (RF-AS05 / RF-F06).
 * O titular é resolvido a partir do UID do Firebase no token, nunca por ID enviado pelo cliente.
 */
@RestController
@RequestMapping("/api/associados/me")
public class MensalidadeAssociadoController {

    @Autowired
    private MensalidadeService mensalidadeService;

    @Autowired
    private PagamentoService pagamentoService;

    @Autowired
    private AssociadoRepository associadoRepository;

    @GetMapping("/mensalidades")
    public ResponseEntity<List<MensalidadeResponseDTO>> listarMinhasMensalidades(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(name = "competencia", required = false) YearMonth competencia) {
        Associado titular = obterTitularAutenticado(usuario);
        return ResponseEntity.ok(mensalidadeService.listarMensalidadesDoAssociado(titular.getId(), competencia));
    }

    @GetMapping("/pagamentos")
    public ResponseEntity<List<PagamentoResponseDTO>> listarMeusPagamentos(
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        Associado titular = obterTitularAutenticado(usuario);
        return ResponseEntity.ok(pagamentoService.listarPorTitular(titular.getId()));
    }

    private Associado obterTitularAutenticado(UsuarioAutenticado usuario) {
        if (usuario == null || usuario.uid() == null || usuario.uid().isBlank()) {
            throw new IllegalArgumentException("Usuário não autenticado.");
        }
        return associadoRepository.findByUsuarioId(usuario.uid())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Nenhum cadastro de associado vinculado ao usuário autenticado."));
    }
}
