package br.ufs.dcomp.collina.service.financeiro;

import java.time.Clock;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import br.ufs.dcomp.collina.dto.financeiro.ResumoFechamentoDTO;
import br.ufs.dcomp.collina.service.financeiro.integracao.ConsultaTitularesElegiveis;
import br.ufs.dcomp.collina.service.financeiro.integracao.TitularElegivel;

/**
 * Orquestrador do fechamento mensal.
 *
 * Fecha a mensalidade EM_ABERTO da competência anterior e abre a da nova competência,
 * para cada titular elegível. NÃO é transacional de propósito: cada titular é processado
 * em sua própria transação, em outro bean ({@link FechamentoTitularService}). Assim a falha
 * de um titular desfaz só o trabalho dele e o lote continua.
 */
@Service
public class FechamentoMensalidadeService {

    private static final Logger log = LoggerFactory.getLogger(FechamentoMensalidadeService.class);

    @Autowired
    private ConsultaTitularesElegiveis consultaTitularesElegiveis;

    @Autowired
    private FechamentoTitularService fechamentoTitularService;

    // Relógio injetado (bean definido em SchedulingConfig): o "mês corrente" vem dele, nunca de YearMonth.now() solto.
    @Autowired
    private Clock clock;

    // Fecha o mês anterior e abre o mês corrente. Usado pelo Agendador (dia 1) e pela reexecução manual.
    // Só aceita a competência corrente de propósito: uma competência arbitrária abriria mensalidades
    // erradas, e as tabelas financeiras não têm exclusão física para desfazer isso.
    public ResumoFechamentoDTO fecharCompetenciaAtual() {
        return fecharEAbrirCompetencia(YearMonth.now(clock));
    }

    public ResumoFechamentoDTO fecharEAbrirCompetencia(YearMonth novaCompetencia) {
        if (novaCompetencia == null) {
            throw new IllegalArgumentException("A nova competência é obrigatória.");
        }

        List<TitularElegivel> titulares = consultaTitularesElegiveis.listarElegiveis(novaCompetencia);

        int processados = 0;
        int ignorados = 0;
        List<Long> titularesComFalha = new ArrayList<>();

        for (TitularElegivel titular : titulares) {
            try {
                switch (fechamentoTitularService.processarTitular(titular, novaCompetencia)) {
                    case PROCESSADO -> processados++;
                    case IGNORADO -> ignorados++;
                }
            } catch (RuntimeException e) {
                Long titularId = titular != null ? titular.associadoId() : null;
                titularesComFalha.add(titularId);
                log.error("Falha ao fechar a competência {} do titular {}: {}",
                        novaCompetencia, titularId, e.getMessage(), e);
            }
        }

        log.info("Fechamento de {} concluído: {} processados, {} ignorados, {} com falha.",
                novaCompetencia, processados, ignorados, titularesComFalha.size());

        return new ResumoFechamentoDTO(novaCompetencia.toString(), processados, ignorados, titularesComFalha);
    }
}