import { useCallback, useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import Aviso from '../components/Aviso'
import Modal from '../components/Modal'
import { planosApi } from '../services/associadosApi'
import type { Plano } from '../types'
import { CATEGORIA_LABEL, PLANOS_POR_CATEGORIA, TIPO_PLANO_LABEL, formatarMoeda, mensagemDeErro } from '../utils'

function categoriaDoPlano(plano: Plano): string {
  const categoria = (Object.keys(PLANOS_POR_CATEGORIA) as (keyof typeof PLANOS_POR_CATEGORIA)[]).find((c) =>
    PLANOS_POR_CATEGORIA[c].includes(plano.tipo),
  )
  return categoria ? CATEGORIA_LABEL[categoria] : '—'
}

// RF-AS02: parâmetros de cada plano
export default function PlanosPage() {
  const [planos, setPlanos] = useState<Plano[]>([])
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState<string | null>(null)
  const [aviso, setAviso] = useState<string | null>(null)
  const [editando, setEditando] = useState<Plano | null>(null)

  const carregar = useCallback(async () => {
    setCarregando(true)
    setErro(null)
    try {
      setPlanos(await planosApi.listar())
    } catch (e) {
      setErro(mensagemDeErro(e))
    } finally {
      setCarregando(false)
    }
  }, [])

  useEffect(() => {
    void carregar()
  }, [carregar])

  return (
    <section>
      <div className="as-titulo-linha">
        <h1>Planos</h1>
      </div>

      <Aviso tipo="erro" mensagem={erro} onFechar={() => setErro(null)} />
      <Aviso tipo="sucesso" mensagem={aviso} onFechar={() => setAviso(null)} />

      <div className="as-tabela-wrap">
        <table className="as-tabela">
          <thead>
            <tr>
              <th>Plano</th>
              <th>Categoria</th>
              <th>Valor mensal</th>
              <th>Reservas/mês</th>
              <th>Máx. dependentes ativos</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {carregando && (
              <tr>
                <td colSpan={6} className="as-vazio">
                  Carregando…
                </td>
              </tr>
            )}
            {!carregando && planos.length === 0 && (
              <tr>
                <td colSpan={6} className="as-vazio">
                  Nenhum plano cadastrado.
                </td>
              </tr>
            )}
            {!carregando &&
              planos.map((p) => (
                <tr key={p.id}>
                  <td>{TIPO_PLANO_LABEL[p.tipo]}</td>
                  <td>{categoriaDoPlano(p)}</td>
                  <td>{formatarMoeda(p.valorPeriodico)}</td>
                  <td>{p.limiteMensalReservas}</td>
                  <td>{p.maxDependentesAtivos}</td>
                  <td>
                    <button type="button" className="as-btn as-btn-link" onClick={() => setEditando(p)}>
                      Editar
                    </button>
                  </td>
                </tr>
              ))}
          </tbody>
        </table>
      </div>

      {editando && (
        <PlanoFormModal
          plano={editando}
          onFechar={() => setEditando(null)}
          onSalvo={() => {
            setEditando(null)
            setAviso('Plano atualizado.')
            void carregar()
          }}
        />
      )}
    </section>
  )
}

interface PlanoFormProps {
  plano: Plano
  onSalvo: () => void
  onFechar: () => void
}

function PlanoFormModal({ plano, onSalvo, onFechar }: PlanoFormProps) {
  const [valor, setValor] = useState(String(plano.valorPeriodico))
  const [reservas, setReservas] = useState(String(plano.limiteMensalReservas))
  const [dependentes, setDependentes] = useState(String(plano.maxDependentesAtivos))
  const [erro, setErro] = useState<string | null>(null)
  const [salvando, setSalvando] = useState(false)

  const enviar = async (e: FormEvent) => {
    e.preventDefault()
    setErro(null)
    setSalvando(true)
    try {
      await planosApi.atualizar(plano.id, {
        valorPeriodico: Number(valor),
        limiteMensalReservas: Number(reservas),
        maxDependentesAtivos: Number(dependentes),
      })
      onSalvo()
    } catch (err) {
      setErro(mensagemDeErro(err))
    } finally {
      setSalvando(false)
    }
  }

  return (
    <Modal titulo={`Editar plano ${TIPO_PLANO_LABEL[plano.tipo]}`} onFechar={onFechar}>
      <form onSubmit={enviar} className="as-form">
        <Aviso tipo="erro" mensagem={erro} />

        <label>
          Valor mensal (R$)
          <input type="number" min="0" step="0.01" value={valor} onChange={(e) => setValor(e.target.value)} required />
        </label>
        <div className="as-form-row">
          <label>
            Limite de reservas por mês
            <input
              type="number"
              min="0"
              step="1"
              value={reservas}
              onChange={(e) => setReservas(e.target.value)}
              required
            />
          </label>
          <label>
            Máx. de dependentes ativos
            <input
              type="number"
              min="0"
              step="1"
              value={dependentes}
              onChange={(e) => setDependentes(e.target.value)}
              required
            />
          </label>
        </div>
        <p className="as-hint">
          Reduzir o máximo de dependentes não inativa vínculos existentes: apenas impede novos cadastros enquanto o
          titular estiver acima do limite.
        </p>

        <div className="as-form-actions">
          <button type="button" className="as-btn as-btn-secundario" onClick={onFechar}>
            Cancelar
          </button>
          <button type="submit" className="as-btn as-btn-primario" disabled={salvando}>
            {salvando ? 'Salvando…' : 'Salvar'}
          </button>
        </div>
      </form>
    </Modal>
  )
}
