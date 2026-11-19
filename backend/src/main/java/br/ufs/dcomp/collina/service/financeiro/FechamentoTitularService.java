package br.ufs.dcomp.collina.service.financeiro;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.ufs.dcomp.collina.model.financeiro.Mensalidade;
import br.ufs.dcomp.collina.repository.financeiro.MensalidadeRepository;
import br.ufs.dcomp.collina.service.financeiro.integracao.TitularElegivel;

/**
 * Processa UM titular no fechamento mensal, em uma transação própria.
 *
 * Fica em um bean separado do orquestrador porque o @Transactional só vale quando a chamada
 * passa pelo proxy do Spring; chamar este método de dentro da mesma classe ignoraria a anotação.
 */
@Service
public class FechamentoTitularService {

    public enum Resultado { PROCESSADO, IGNORADO }

    private static final Logger log = LoggerFactory.getLogger(FechamentoTitularService.class);

    @Autowired
    private MensalidadeRepository mensalidadeRepository;

    /**
     * Regra de negócio: a mensalidade de janeiro acumula gastos avulsos durante janeiro, é fechada
     * no início de fevereiro (nova competência) e vence no último dia de fevereiro.
     * (Decisão do grupo: por ora o vencimento não precisa cair em dia útil.)
     */
    @Transactional
    public Resultado processarTitular(TitularElegivel titular, YearMonth novaCompetencia) {
        if (titular == null) {
            throw new IllegalArgumentException("O titular é obrigatório.");
        }
        if (novaCompetencia == null) {
            throw new IllegalArgumentException("A nova competência é obrigatória.");
        }

        Long titularId = titular.associadoId();
        YearMonth competenciaFechada = novaCompetencia.minusMonths(1);

        // 1. Repetição segura: se o titular já tem mensalidade da nova competência (que não seja
        //    cancelada), este fechamento já foi feito para ele. Não fecha nem abre de novo.
        boolean jaProcessado = mensalidadeRepository
                .findByAssociadoTitularAndCompetencia(titularId, novaCompetencia.toString())
                .stream()
                .anyMatch(m -> !m.estaCancelada());
        if (jaProcessado) {
            log.info("Titular {} já possui mensalidade de {}; ignorado.", titularId, novaCompetencia);
            return Resultado.IGNORADO;
        }

        // 2. Fecha a mensalidade EM_ABERTO da competência anterior (a entidade valida o estado).
        Optional<Mensalidade> emAberto = mensalidadeRepository
                .findByAssociadoTitularAndCompetencia(titularId, competenciaFechada.toString())
                .stream()
                .filter(Mensalidade::estaEmAberto)
                .findFirst();

        if (emAberto.isPresent()) {
            Mensalidade mensalidade = emAberto.get();
            mensalidade.fechar(calcularVencimento(competenciaFechada));
            // Grava o fechamento ANTES de inserir a nova mensalidade: sem isso o Hibernate faria o
            // INSERT antes do UPDATE, e um futuro índice único "uma EM_ABERTO por titular" seria violado.
            mensalidadeRepository.saveAndFlush(mensalidade);
        } else {
            // Ex.: titular recém-cadastrado, sem mensalidade na competência anterior.
            log.warn("Titular {} sem mensalidade EM_ABERTO em {}; apenas abrindo {}.",
                    titularId, competenciaFechada, novaCompetencia);
        }

        // 3. Abre a mensalidade da nova competência com o valor base do plano vigente (snapshot).
        mensalidadeRepository.save(new Mensalidade(novaCompetencia.toString(), titular.valorBase(), titularId));

        return Resultado.PROCESSADO;
    }

    // Mensalidade da competência X vence no último dia do mês seguinte (X + 1).
    static LocalDate calcularVencimento(YearMonth competenciaFechada) {
        return competenciaFechada.plusMonths(1).atEndOfMonth();
    }
}