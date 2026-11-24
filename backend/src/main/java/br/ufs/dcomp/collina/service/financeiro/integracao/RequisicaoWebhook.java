package br.ufs.dcomp.collina.service.financeiro.integracao;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * A requisição HTTP do webhook, sem depender de classes do Spring MVC. A validação de assinatura
 * de cada provedor usa partes diferentes (corpo, cabeçalhos, parâmetros de consulta), então o
 * controller entrega tudo e a implementação do gateway escolhe o que precisa.
 *
 * Nomes de cabeçalho não diferenciam maiúsculas de minúsculas.
 */
public record RequisicaoWebhook(String corpo, Map<String, String> cabecalhos, Map<String, String> parametros) {

    public RequisicaoWebhook {
        corpo = corpo == null ? "" : corpo;

        Map<String, String> h = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        if (cabecalhos != null) {
            h.putAll(cabecalhos);
        }
        cabecalhos = Collections.unmodifiableMap(h);

        parametros = parametros == null
                ? Map.of()
                : Collections.unmodifiableMap(new HashMap<>(parametros));
    }

    public String cabecalho(String nome) {
        return cabecalhos.get(nome);
    }
}
