package br.ufs.dcomp.collina.service.financeiro.integracao.provisorio;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import br.ufs.dcomp.collina.model.financeiro.FormaPagamentoEnum;
import br.ufs.dcomp.collina.service.financeiro.integracao.CobrancaGateway;
import br.ufs.dcomp.collina.service.financeiro.integracao.GatewayPagamentoClient;
import br.ufs.dcomp.collina.service.financeiro.integracao.NotificacaoGateway;
import br.ufs.dcomp.collina.service.financeiro.integracao.RequisicaoWebhook;
import br.ufs.dcomp.collina.service.financeiro.integracao.StatusNotificacaoGateway;

/**
 * PROVISÓRIA. Simula o gateway (Pix e cartão) sem conta, rede ou túnel. Ativa em qualquer perfil
 * exceto {@code gateway-real}, onde entra a implementação real (dois beans do mesmo tipo
 * impediriam a aplicação de subir).
 *
 * Não guarda estado: tudo o que a cobrança precisa fica no Pagamento, no banco. Isso permite
 * simular notificações mesmo depois de reiniciar a aplicação.
 *
 * Para exercitar a assinatura de verdade, {@link #montarNotificacao} produz uma requisição
 * assinada com HMAC-SHA256 no formato {@code x-signature: ts=<epoch>,v1=<hmac de "ts.corpo">},
 * e {@link #validarAssinatura} a confere. O formato é do stub; a implementação real usará o
 * validador oficial do provedor.
 */
@Component
@Profile("!gateway-real")
public class GatewayPagamentoClientStub implements GatewayPagamentoClient {

    private static final String CABECALHO_ASSINATURA = "x-signature";
    private static final Pattern ID_VALIDO = Pattern.compile("[A-Za-z0-9_-]+");

    private final Clock clock;
    private final String segredo;
    private final long validadeMinutos;

    public GatewayPagamentoClientStub(
            Clock clock,
            @Value("${collina.gateway.segredo-webhook:segredo-de-desenvolvimento}") String segredo,
            @Value("${collina.gateway.stub.validade-minutos:30}") long validadeMinutos) {
        this.clock = clock;
        this.segredo = segredo;
        this.validadeMinutos = validadeMinutos;
    }

    @Override
    public CobrancaGateway criarCobranca(BigDecimal valor, FormaPagamentoEnum forma, String referencia) {
        if (valor == null || valor.signum() <= 0) {
            throw new IllegalArgumentException("O valor da cobrança deve ser maior que zero.");
        }
        if (forma != FormaPagamentoEnum.PIX && forma != FormaPagamentoEnum.CARTAO) {
            throw new IllegalArgumentException("O gateway só atende PIX e CARTAO.");
        }
        if (referencia == null || referencia.isBlank()) {
            throw new IllegalArgumentException("A referência da cobrança é obrigatória.");
        }

        String idExterno = "STUB-" + UUID.randomUUID();
        LocalDateTime expiraEm = LocalDateTime.now(clock).plusMinutes(validadeMinutos);

        if (forma == FormaPagamentoEnum.PIX) {
            return new CobrancaGateway(idExterno, null,
                    "STUBPIX|" + idExterno + "|" + valor.toPlainString(), expiraEm);
        }
        return new CobrancaGateway(idExterno, "https://stub-gateway.local/checkout/" + idExterno, null, expiraEm);
    }

    @Override
    public boolean validarAssinatura(RequisicaoWebhook requisicao) {
        if (requisicao == null) {
            return false;
        }
        String cabecalho = requisicao.cabecalho(CABECALHO_ASSINATURA);
        if (cabecalho == null || cabecalho.isBlank()) {
            return false;
        }

        String ts = null;
        String v1 = null;
        for (String parte : cabecalho.split(",")) {
            String[] chaveValor = parte.trim().split("=", 2);
            if (chaveValor.length == 2) {
                if (chaveValor[0].equals("ts")) {
                    ts = chaveValor[1];
                } else if (chaveValor[0].equals("v1")) {
                    v1 = chaveValor[1];
                }
            }
        }
        if (ts == null || v1 == null) {
            return false;
        }

        String esperado = hmac(ts + "." + requisicao.corpo());
        // Comparação em tempo constante: não revela quantos caracteres coincidiram.
        return MessageDigest.isEqual(
                esperado.getBytes(StandardCharsets.UTF_8), v1.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public NotificacaoGateway interpretarNotificacao(RequisicaoWebhook requisicao) {
        String corpo = requisicao == null ? "" : requisicao.corpo();
        String idExterno = extrair(corpo, "idExterno");
        String status = extrair(corpo, "status");
        String valor = extrair(corpo, "valor");

        if (idExterno == null || status == null || valor == null) {
            throw new IllegalArgumentException("Notificação do gateway malformada.");
        }
        try {
            return new NotificacaoGateway(idExterno, StatusNotificacaoGateway.valueOf(status), new BigDecimal(valor));
        } catch (IllegalArgumentException e) { // inclui NumberFormatException
            throw new IllegalArgumentException("Notificação do gateway com status ou valor inválido.", e);
        }
    }

    /**
     * Monta uma notificação assinada, como o gateway faria. Usada pelo endpoint de simulação e
     * pelos testes. O valor é informado de propósito para permitir simular divergência (RF-F22).
     */
    public RequisicaoWebhook montarNotificacao(String idExterno, StatusNotificacaoGateway status, BigDecimal valor) {
        if (idExterno == null || !ID_VALIDO.matcher(idExterno).matches()) {
            throw new IllegalArgumentException("Id externo inválido.");
        }
        if (status == null) {
            throw new IllegalArgumentException("O status é obrigatório.");
        }
        if (valor == null || valor.signum() < 0) {
            throw new IllegalArgumentException("O valor não pode ser nulo nem negativo.");
        }

        String corpo = "{\"idExterno\":\"" + idExterno + "\",\"status\":\"" + status
                + "\",\"valor\":\"" + valor.toPlainString() + "\"}";
        long ts = Instant.now(clock).getEpochSecond();
        String assinatura = "ts=" + ts + ",v1=" + hmac(ts + "." + corpo);

        return new RequisicaoWebhook(corpo, Map.of(CABECALHO_ASSINATURA, assinatura), Map.of());
    }

    // O stub gera e lê o próprio formato; a implementação real usará o JSON do provedor.
    private static String extrair(String corpo, String campo) {
        Matcher m = Pattern.compile("\"" + Pattern.quote(campo) + "\"\\s*:\\s*\"([^\"]*)\"").matcher(corpo);
        return m.find() ? m.group(1) : null;
    }

    private String hmac(String mensagem) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(segredo.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(mensagem.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Falha ao calcular a assinatura do stub.", e);
        }
    }
}
