package br.ufs.dcomp.collina.config;

import br.ufs.dcomp.collina.service.auth.UsuarioService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

// Ao subir a API, promove a ADMIN os e-mails listados em collina.auth.admin-emails (primeiro administrador do sistema)
@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private final UsuarioService usuarioService;

    public AdminBootstrapRunner(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @Override
    public void run(ApplicationArguments args) {
        usuarioService.promoverAdministradoresConfigurados();
    }
}
