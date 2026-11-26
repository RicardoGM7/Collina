package br.ufs.dcomp.collina.service.espacos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * IMPLEMENTAÇÃO PROVISÓRIA: considera todos os associados adimplentes, porque ainda não existe
 * Mensalidade no repositório. Enquanto for esta, o bloqueio por inadimplência (RF-AS07) NÃO é aplicado.
 * Substituir pela implementação do Financeiro (ver ConsultaAdimplencia).
 */
@Component
public class AdimplenciaPadrao implements ConsultaAdimplencia {

    private static final Logger log = LoggerFactory.getLogger(AdimplenciaPadrao.class);

    public AdimplenciaPadrao() {
        log.warn("Bloqueio de reserva por inadimplência INATIVO: nenhum módulo Financeiro "
                + "implementa ConsultaAdimplencia (usando AdimplenciaPadrao).");
    }

    @Override
    public boolean possuiMensalidadeEmAtraso(Integer associadoId) {
        return false;
    }
}
