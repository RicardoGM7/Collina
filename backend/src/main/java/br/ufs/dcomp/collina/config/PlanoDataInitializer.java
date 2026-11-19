package br.ufs.dcomp.collina.config;

import br.ufs.dcomp.collina.model.associados.Plano;
import br.ufs.dcomp.collina.model.associados.TipoPlanoEnum;
import br.ufs.dcomp.collina.repository.associados.PlanoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

// Cria os planos iniciais (RN-ASS-04 a RN-ASS-08) quando ainda não existirem.
// Idempotente: planos já cadastrados (inclusive com valores editados via RF-AS02) não são sobrescritos.
@Configuration
public class PlanoDataInitializer {

    @Bean
    CommandLineRunner carregarPlanosIniciais(PlanoRepository planoRepository) {
        return args -> {
            criarSeAusente(planoRepository, TipoPlanoEnum.BASICO, "200.00", 2, 1);        // RN-ASS-05
            criarSeAusente(planoRepository, TipoPlanoEnum.PLUS, "350.00", 4, 2);          // RN-ASS-06
            criarSeAusente(planoRepository, TipoPlanoEnum.PREMIUM, "500.00", 8, 3);       // RN-ASS-07
            criarSeAusente(planoRepository, TipoPlanoEnum.PROPRIETARIO, "300.00", 10, 5); // RN-ASS-08
        };
    }

    private void criarSeAusente(PlanoRepository repository, TipoPlanoEnum tipo, String valor,
                                int limiteReservas, int maxDependentes) {
        if (repository.findByTipo(tipo).isPresent()) {
            return;
        }
        Plano plano = new Plano();
        plano.setTipo(tipo);
        plano.setMensalidade(new BigDecimal(valor));
        plano.setLimiteMensalReservas(limiteReservas);
        plano.setMaxDependentesAtivos(maxDependentes);
        repository.save(plano);
    }
}
