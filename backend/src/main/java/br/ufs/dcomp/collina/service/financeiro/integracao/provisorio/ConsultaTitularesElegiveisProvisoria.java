package br.ufs.dcomp.collina.service.financeiro.integracao.provisorio;

import java.time.YearMonth;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import br.ufs.dcomp.collina.service.financeiro.integracao.ConsultaTitularesElegiveis;
import br.ufs.dcomp.collina.service.financeiro.integracao.TitularElegivel;

/**
 * PROVISÓRIA. Não há módulo de associados, então não há titulares: devolve
 * lista vazia (o fechamento roda sem efeito). Cenários reais só são testáveis
 * com Mockito simulando a interface. REMOVER junto com a implementação real.
 */
@Component
public class ConsultaTitularesElegiveisProvisoria implements ConsultaTitularesElegiveis {

    private static final Logger log = LoggerFactory.getLogger(ConsultaTitularesElegiveisProvisoria.class);

    @Override
    public List<TitularElegivel> listarElegiveis(YearMonth competencia) {
        if (competencia == null) {
            throw new IllegalArgumentException("A competência é obrigatória.");
        }
        log.warn("Implementação provisória: nenhum titular elegível para {}.", competencia);
        return List.of();
    }
}
