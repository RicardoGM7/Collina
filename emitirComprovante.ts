// =====================================================================
// Tela: Emitir Comprovante de Pagamento (Associado)
// Decorre da pós-condição do UC05 (Realizar pagamento de mensalidade)
// e do UC06 (Acompanhar status do pagamento), quando o gateway confirma
// a transação e o associado deseja obter o comprovante.
// Requisitos relacionados: RF-F13, RF-F14, RI-C01
// =====================================================================

import React from "react";
import type { Mensalidade, ResultadoGateway } from "./RealizarPagamento";

interface EmitirComprovanteProps {
  associadoNome: string;
  mensalidade: Mensalidade;
  resultado: ResultadoGateway;
  onVoltar: () => void;
}

const formatarValor = (valor: number) =>
  valor.toLocaleString("pt-BR", { style: "currency", currency: "BRL" });

export default function EmitirComprovante({
  associadoNome,
  mensalidade,
  resultado,
  onVoltar,
}: EmitirComprovanteProps) {
  const confirmado = resultado.status === "confirmado";

  const handleBaixar = () => {
    // Em produção: gerar PDF (ex.: via lib de geração de PDF) e disparar
    // o download. Aqui deixamos o hook de integração isolado.
    window.print();
  };

  const handleCompartilhar = async () => {
    const texto = `Comprovante de pagamento — ${mensalidade.referencia} — ${formatarValor(
      mensalidade.valor
    )} — Transação ${resultado.codigoTransacao ?? ""}`;
    if (navigator.share) {
      await navigator.share({ title: "Comprovante de pagamento", text: texto });
    } else {
      await navigator.clipboard.writeText(texto);
    }
  };

  return (
    <div className="min-h-screen bg-white flex flex-col">
      <header className="bg-[#101c3d] text-white px-4 py-3 flex items-center justify-between">
        <button aria-label="Menu" onClick={onVoltar} className="text-xl">
          ☰
        </button>
        <h1 className="text-base font-semibold">Emitir Comprovante</h1>
        <div className="w-7 h-7 rounded-full bg-white/80" />
      </header>

      <main className="flex-1 p-4">
        <section className="rounded-2xl border border-gray-200 p-5 shadow-sm space-y-4">
          <div className="text-center">
            <span
              className={`inline-block text-xs px-3 py-1 rounded-full font-medium ${
                confirmado
                  ? "bg-emerald-100 text-emerald-700"
                  : "bg-red-100 text-red-700"
              }`}
            >
              {confirmado ? "Pagamento confirmado" : "Pagamento não confirmado"}
            </span>
          </div>

          <div className="divide-y divide-gray-100">
            <LinhaComprovante label="Associado" valor={associadoNome} />
            <LinhaComprovante
              label="Referência"
              valor={mensalidade.referencia}
            />
            <LinhaComprovante
              label="Valor pago"
              valor={formatarValor(mensalidade.valor)}
            />
            <LinhaComprovante
              label="Data da confirmação"
              valor={resultado.dataConfirmacao ?? "—"}
            />
            <LinhaComprovante
              label="Código da transação"
              valor={resultado.codigoTransacao ?? "—"}
            />
          </div>
        </section>

        {!confirmado && (
          <p className="text-sm text-gray-500 text-center mt-4">
            Não é possível emitir comprovante para uma transação que não foi
            confirmada pelo gateway de pagamentos.
          </p>
        )}
      </main>

      <footer className="p-4 space-y-2">
        <button
          disabled={!confirmado}
          onClick={handleBaixar}
          className="w-full rounded-xl bg-[#101c3d] disabled:bg-gray-300 text-white font-semibold py-3"
        >
          Baixar comprovante
        </button>
        <button
          disabled={!confirmado}
          onClick={handleCompartilhar}
          className="w-full rounded-xl border border-[#101c3d] disabled:border-gray-300 disabled:text-gray-400 text-[#101c3d] font-semibold py-3"
        >
          Compartilhar
        </button>
      </footer>
    </div>
  );
}

function LinhaComprovante({ label, valor }: { label: string; valor: string }) {
  return (
    <div className="flex items-center justify-between py-2">
      <span className="text-sm text-gray-500">{label}</span>
      <span className="text-sm font-medium text-gray-800">{valor}</span>
    </div>
  );
}
