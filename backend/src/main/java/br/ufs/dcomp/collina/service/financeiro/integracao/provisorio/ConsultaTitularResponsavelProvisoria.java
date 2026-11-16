package br.ufs.dcomp.collina.service.financeiro.integracao.provisorio;

import org.springframework.stereotype.Component;

import br.ufs.dcomp.collina.service.financeiro.integracao.ConsultaTitularResponsavel;

/**
 * PROVISÓRIA. Trata todo associado como titular (devolve o próprio id).
 * Dependentes NÃO são resolvidos para o titular enquanto esta classe existir.
 *
 * REMOVER (apagar o arquivo) no mesmo PR que trouxer a implementação real em
 * associados; com dois beans do mesmo tipo a aplicação não sobe.
 */
@Component
public class ConsultaTitularResponsavelProvisoria implements ConsultaTitularResponsavel {

    @Override
    public Long buscarIdTitularResponsavel(Long associadoId) {
        if (associadoId == null) {
            throw new IllegalArgumentException("O id do associado é obrigatório.");
        }
        return associadoId;
    }
}
