package br.ufs.dcomp.collina.service.associados;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * Agendador diário de inativação de dependentes por idade limite (21 anos - RF-AS04).
 * Segue o mesmo padrão do Agendador do financeiro: o método agendado não é transacional,
 * delegando cada vínculo a um método @Transactional no VinculoDependenciaService para que
 * a falha em um registro não desfaça os demais.
 *
 * A recuperação manual (idempotente) pode ser feita via POST /api/dependentes/inativar-por-idade.
 * Expressão cron configurável via propriedade collina.agendador.dependentes-idade-cron.
 */
@Component
public class InativacaoDependenteAgendador {

    private static final Logger log = LoggerFactory.getLogger(InativacaoDependenteAgendador.class);

    @Autowired
    private VinculoDependenciaService vinculoDependenciaService;

    @Autowired
    private Clock clock;

    // Todos os dias, 00:15: inativa vínculos ativos de dependentes que já completaram 21 anos.
    @Scheduled(cron = "${collina.agendador.dependentes-idade-cron:0 15 0 * * *}",
               zone = "${collina.agendador.zona:America/Sao_Paulo}")
    public void executarInativacaoPorIdade() {
        LocalDate hoje = LocalDate.now(clock);
        log.info("Agendador: iniciando verificação diária de dependentes por idade limite em {}.", hoje);

        List<Integer> idsVencidos;
        try {
            idsVencidos = vinculoDependenciaService.listarIdsVinculosVencidosPorIdade(hoje);
        } catch (RuntimeException e) {
            log.error("Agendador: falha ao consultar vínculos ativos para verificação de idade em {}.", hoje, e);
            return;
        }

        int inativados = 0;
        int falhas = 0;
        for (Integer vinculoId : idsVencidos) {
            try {
                if (vinculoDependenciaService.inativarVinculoPorIdadeSeVencido(vinculoId, hoje)) {
                    inativados++;
                }
            } catch (RuntimeException e) {
                falhas++;
                log.error("Agendador: falha ao inativar vínculo {} por idade limite em {}.", vinculoId, hoje, e);
            }
        }

        if (falhas > 0) {
            log.warn("Agendador: verificação de idade em {} concluída com {} vínculo(s) inativado(s) e {} falha(s).",
                    hoje, inativados, falhas);
        } else {
            log.info("Agendador: verificação de idade em {} concluída com {} vínculo(s) inativado(s).",
                    hoje, inativados);
        }
    }
}
