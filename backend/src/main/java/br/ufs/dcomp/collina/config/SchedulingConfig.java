package br.ufs.dcomp.collina.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class SchedulingConfig {

    // Relógio único do sistema, no fuso do clube. Quem precisa de "agora" injeta este bean, em vez de
    // chamar LocalDate.now() direto: nos testes ele é substituído por Clock.fixed(...).
    // O mesmo fuso é usado pelo cron do Agendador (propriedade collina.agendador.zona).
    @Bean
    public Clock clock(@Value("${collina.agendador.zona:America/Sao_Paulo}") String zona) {
        return Clock.system(ZoneId.of(zona));
    }
}