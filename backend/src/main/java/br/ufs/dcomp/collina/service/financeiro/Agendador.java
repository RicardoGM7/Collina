package br.ufs.dcomp.collina.service.financeiro;

import java.time.Clock;
import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import br.ufs.dcomp.collina.dto.financeiro.ResumoFechamentoDTO;

/**
 * Agendador do financeiro (RF-F05/F20 e RF-F21). Sem regra de negócio: só decide QUANDO
 * chamar os services e registra o resultado em log.
 *
 * O Spring não recupera execuções perdidas: se a aplicação estiver fora do ar no horário,
 * o job daquele dia não roda. Para o fechamento mensal, a recuperação é manual:
 * POST /api/financeiro/fechamento (idempotente, pode ser repetido sem duplicar nada).
 *
 * Expressões cron (6 campos: segundo minuto hora dia-do-mês mês dia-da-semana) e fuso podem ser
 * trocados por propriedades, por exemplo para rodar a cada minuto na demonstração:
 *   collina.agendador.fechamento-cron=0 * * * * *
 */
@Component
public class Agendador {

    private static final Logger log = LoggerFactory.getLogger(Agendador.class);

    @Autowired
    private FechamentoMensalidadeService fechamentoMensalidadeService;

    @Autowired
    private MensalidadeService mensalidadeService;

    @Autowired
    private Clock clock;

    // Dia 1 de cada mês, 00:05: fecha a mensalidade do mês anterior e abre a do mês corrente.
    @Scheduled(cron = "${collina.agendador.fechamento-cron:0 5 0 1 * *}",
               zone = "${collina.agendador.zona:America/Sao_Paulo}")
    public void executarFechamentoMensal() {
        log.info("Agendador: iniciando o fechamento mensal.");
        try {
            ResumoFechamentoDTO resumo = fechamentoMensalidadeService.fecharCompetenciaAtual();
            if (!resumo.titularesComFalha().isEmpty()) {
                log.warn("Agendador: fechamento de {} terminou com falhas nos titulares {}. "
                        + "Reexecute (POST /api/financeiro/fechamento): só os pendentes serão processados.",
                        resumo.competencia(), resumo.titularesComFalha());
            }
        } catch (RuntimeException e) {
            log.error("Agendador: o fechamento mensal falhou antes de processar os titulares. "
                    + "Reexecute manualmente (POST /api/financeiro/fechamento).", e);
        }
    }

    // Todos os dias, 00:30: PENDENTE com vencimento anterior a hoje passa a ATRASADA.
    @Scheduled(cron = "${collina.agendador.vencidas-cron:0 30 0 * * *}",
               zone = "${collina.agendador.zona:America/Sao_Paulo}")
    public void executarVerificacaoDeVencidas() {
        LocalDate hoje = LocalDate.now(clock);
        log.info("Agendador: verificando mensalidades vencidas em {}.", hoje);
        try {
            mensalidadeService.marcarAtrasadas(hoje);
        } catch (RuntimeException e) {
            // marcarAtrasadas roda em uma transação só: se falhar, nada foi gravado e a próxima
            // execução tenta de novo. Por isso o erro precisa ficar visível no log.
            log.error("Agendador: a verificação de mensalidades vencidas de {} falhou.", hoje, e);
        }
    }
}