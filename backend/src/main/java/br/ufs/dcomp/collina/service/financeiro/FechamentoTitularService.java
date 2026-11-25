package br.ufs.dcomp.collina.service.financeiro;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.ufs.dcomp.collina.model.associados.Associado;
import br.ufs.dcomp.collina.model.financeiro.Mensalidade;
import br.ufs.dcomp.collina.repository.financeiro.MensalidadeRepository;

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
     * Fecha a mensalidade da competência anterior (se houver EM_ABERTO) com a data de vencimento
     * informada e abre a mensalidade da nova competência com o valor do plano vigente.
     */
    @Transactional
    public Resultado processarTitular(Associado associado, YearMonth novaCompetencia, LocalDate vencimento) {
        if (associado == null) {
            throw new IllegalArgumentException("O associado titular é obrigatório.");
        }
        if (novaCompetencia == null) {
            throw new IllegalArgumentException("A nova competência é obrigatória.");
        }
        if (vencimento == null) {
            throw new IllegalArgumentException("A data de vencimento é obrigatória.");
        }

        Integer titularId = associado.getId();
        YearMonth competenciaFechada = novaCompetencia.minusMonths(1);

        // 1. Repetição segura: se o titular já tem mensalidade da nova competência (que não seja
        //    cancelada), este fechamento já foi feito para ele. Não fecha nem abre de novo.
        boolean jaProcessado = mensalidadeRepository
                .findByAssociadoTitular_IdAndCompetencia(titularId, novaCompetencia.toString())
                .stream()
                .anyMatch(m -> !m.estaCancelada());
        if (jaProcessado) {
            log.info("Titular {} já possui mensalidade de {}; ignorado.", titularId, novaCompetencia);
            return Resultado.IGNORADO;
        }

        // 2. Fecha a mensalidade EM_ABERTO da competência anterior (a entidade valida o estado).
        Optional<Mensalidade> emAberto = mensalidadeRepository
                .findByAssociadoTitular_IdAndCompetencia(titularId, competenciaFechada.toString())
                .stream()
                .filter(Mensalidade::estaEmAberto)
                .findFirst();

        if (emAberto.isPresent()) {
            Mensalidade mensalidade = emAberto.get();
            mensalidade.fechar(vencimento);
            mensalidadeRepository.saveAndFlush(mensalidade);
        } else {
            log.warn("Titular {} sem mensalidade EM_ABERTO em {}; apenas abrindo {}.",
                    titularId, competenciaFechada, novaCompetencia);
        }

        // 3. Abre a mensalidade da nova competência com o valor base do plano vigente lido agora (snapshot).
        mensalidadeRepository.save(new Mensalidade(
                novaCompetencia.toString(),
                associado.getPlano().getMensalidade(),
                associado
        ));

        return Resultado.PROCESSADO;
    }
}
