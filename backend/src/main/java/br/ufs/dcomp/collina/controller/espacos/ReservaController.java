package br.ufs.dcomp.collina.controller.espacos;

import br.ufs.dcomp.collina.dto.espacos.ReservaRequestDTO;
import br.ufs.dcomp.collina.dto.espacos.ReservaResponseDTO;
import br.ufs.dcomp.collina.model.auth.UsuarioAutenticado;
import br.ufs.dcomp.collina.service.espacos.ReservaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Permissões por perfil: ver SecurityConfig (/api/reservas/**).
@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    @Autowired
    private ReservaService reservaService;

    // 1. Solicitar reserva de um ou mais blocos (201 Created) - somente ASSOCIADO (titular)
    @PostMapping
    public ResponseEntity<List<ReservaResponseDTO>> solicitar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                                              @Valid @RequestBody ReservaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reservaService.solicitar(usuario.uid(), dto));
    }

    // 2. Minhas reservas - ASSOCIADO
    @GetMapping("/minhas")
    public ResponseEntity<List<ReservaResponseDTO>> minhas(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ResponseEntity.ok(reservaService.listarMinhas(usuario.uid()));
    }

    // 3. Reservas de um associado (?associadoId=) - RECEPCAO / ADMIN
    @GetMapping
    public ResponseEntity<List<ReservaResponseDTO>> listarPorAssociado(@RequestParam Integer associadoId) {
        return ResponseEntity.ok(reservaService.listarPorAssociado(associadoId));
    }

    // 4. Cancelar reserva (até o dia anterior) - dono, RECEPCAO ou ADMIN
    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<ReservaResponseDTO> cancelar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                                       @PathVariable Integer id) {
        return ResponseEntity.ok(reservaService.cancelar(usuario.perfil(), usuario.uid(), id));
    }
}
