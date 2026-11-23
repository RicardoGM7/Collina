package br.ufs.dcomp.collina.service.financeiro;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import br.ufs.dcomp.collina.dto.financeiro.ResumoFechamentoDTO;
import br.ufs.dcomp.collina.model.associados.Associado;
import br.ufs.dcomp.collina.model.associados.SituacaoCadastralEnum;
import br.ufs.dcomp.collina.repository.associados.AssociadoRepository;

/**
 * Orquestrador do fechamento mensal.
 *
 * Fecha a mensalidade EM_ABERTO da competência anterior e abre a da nova competência,
 * para cada titular elegível (ATIVO ou SUSPENSO). NÃO é transacional de propósito: cada titular é processado
 * em sua própria transação, em outro bean ({@link FechamentoTitularService}). Assim a falha
 * de um titular desfaz só o trabalho dele e o lote continua.
 */
@Service
public class FechamentoMensalidadeService {

    private static final Logger log = LoggerFactory.getLogger(FechamentoMensalidadeService.class);

    @Autowired
    private AssociadoRepository associadoRepository;

    @Autowired
    private FechamentoTitularService fechamentoTitularService;

    @Autowired
    private Clock clock;

    public ResumoFechamentoDTO fecharCompetenciaAtual() {
        YearMonth agora = YearMonth.now(clock);
        return fecharEAbrirCompetencia(agora, agora.atEndOfMonth());
    }

    public ResumoFechamentoDTO fecharEAbrirCompetencia(YearMonth novaCompetencia) {
        if (novaCompetencia == null) {
            throw new IllegalArgumentException("A nova competência é obrigatória.");
        }
        return fecharEAbrirCompetencia(novaCompetencia, novaCompetencia.atEndOfMonth());
    }

    public ResumoFechamentoDTO fecharEAbrirCompetencia(YearMonth novaCompetencia, LocalDate vencimento) {
        if (novaCompetencia == null) {
            throw new IllegalArgumentException("A nova competência é obrigatória.");
        }
        if (vencimento == null) {
            throw new IllegalArgumentException("A data de vencimento é obrigatória.");
        }

        // Carrega titulares ATIVO e SUSPENSO (inativos são ignorados)
        List<Associado> titulares = associadoRepository.findBySituacaoCadastralIn(
                List.of(SituacaoCadastralEnum.ATIVO, SituacaoCadastralEnum.SUSPENSO)
        );

        int processados = 0;
        int ignorados = 0;
        List<Integer> titularesComFalha = new ArrayList<>();

        for (Associado titular : titulares) {
            try {
                switch (fechamentoTitularService.processarTitular(titular, novaCompetencia, vencimento)) {
                    case PROCESSADO -> processados++;
                    case IGNORADO -> ignorados++;
                }
            } catch (RuntimeException e) {
                Integer titularId = titular != null ? titular.getId() : null;
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
