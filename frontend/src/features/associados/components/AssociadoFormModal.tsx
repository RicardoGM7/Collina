import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { associadosApi, planosApi } from '../services/associadosApi'
import type { Associado, CategoriaAssociado, Plano, TipoPlano } from '../types'
import {
  CATEGORIA_LABEL,
  PLANOS_POR_CATEGORIA,
  TIPO_PLANO_LABEL,
  formatarMoeda,
  hojeISO,
  mensagemDeErro,
  somenteDigitos,
} from '../utils'
import Aviso from './Aviso'
import Modal from './Modal'

interface Props {
  onCadastrado: (associado: Associado) => void
  onFechar: () => void
}

// RF-AS01 + RF-AS02: cadastro de titular já vinculado a um plano
export default function AssociadoFormModal({ onCadastrado, onFechar }: Props) {
  const [nome, setNome] = useState('')
  const [cpf, setCpf] = useState('')
  const [dataNascimento, setDataNascimento] = useState('')
  const [categoria, setCategoria] = useState<CategoriaAssociado>('CONTRIBUINTE')
  const [tipoPlano, setTipoPlano] = useState<TipoPlano>('BASICO')
  const [planos, setPlanos] = useState<Plano[]>([])
  const [erro, setErro] = useState<string | null>(null)
  const [salvando, setSalvando] = useState(false)

  useEffect(() => {
    planosApi
      .listar()
      .then(setPlanos)
      .catch((e) => setErro(mensagemDeErro(e)))
  }, [])

  const trocarCategoria = (nova: CategoriaAssociado) => {
    setCategoria(nova)
    setTipoPlano(PLANOS_POR_CATEGORIA[nova][0])
  }

  const planoDe = (tipo: TipoPlano) => planos.find((p) => p.tipo === tipo)

  const rotuloPlano = (tipo: TipoPlano) => {
    const plano = planoDe(tipo)
    if (!plano) return TIPO_PLANO_LABEL[tipo]
    return (
      `${TIPO_PLANO_LABEL[tipo]} — ${formatarMoeda(plano.valorPeriodico)}/mês, ` +
      `${plano.limiteMensalReservas} reservas, ${plano.maxDependentesAtivos} dependentes`
    )
  }

  const enviar = async (e: FormEvent) => {
    e.preventDefault()
    setErro(null)
    setSalvando(true)
    try {
      const novo = await associadosApi.cadastrar({
        nome: nome.trim(),
        cpf,
        dataNascimento,
        categoria,
        tipoPlano,
      })
      onCadastrado(novo)
    } catch (err) {
      setErro(mensagemDeErro(err))
    } finally {
      setSalvando(false)
    }
  }

  return (
    <Modal titulo="Novo associado titular" onFechar={onFechar}>
      <form onSubmit={enviar} className="as-form">
        <Aviso tipo="erro" mensagem={erro} />

        <label>
          Nome completo
          <input value={nome} onChange={(e) => setNome(e.target.value)} required />
        </label>

        <div className="as-form-row">
          <label>
            CPF (somente números)
            <input
              value={cpf}
              onChange={(e) => setCpf(somenteDigitos(e.target.value).slice(0, 11))}
              inputMode="numeric"
              pattern="\d{11}"
              title="Informe os 11 dígitos do CPF"
              required
            />
          </label>
          <label>
            Data de nascimento
            <input
              type="date"
              value={dataNascimento}
              max={hojeISO()}
              onChange={(e) => setDataNascimento(e.target.value)}
              required
            />
          </label>
        </div>

        <label>
          Categoria
          <select value={categoria} onChange={(e) => trocarCategoria(e.target.value as CategoriaAssociado)}>
            {(Object.keys(CATEGORIA_LABEL) as CategoriaAssociado[]).map((c) => (
              <option key={c} value={c}>
                {CATEGORIA_LABEL[c]}
              </option>
            ))}
          </select>
        </label>

        <label>
          Plano
          <select value={tipoPlano} onChange={(e) => setTipoPlano(e.target.value as TipoPlano)}>
            {PLANOS_POR_CATEGORIA[categoria].map((t) => (
              <option key={t} value={t}>
                {rotuloPlano(t)}
              </option>
            ))}
          </select>
        </label>

        <p className="as-hint">
          Se o CPF já existir como ex-dependente, os dados pessoais já registrados são reaproveitados (RF-AS04).
        </p>

        <div className="as-form-actions">
          <button type="button" className="as-btn as-btn-secundario" onClick={onFechar}>
            Cancelar
          </button>
          <button type="submit" className="as-btn as-btn-primario" disabled={salvando}>
            {salvando ? 'Salvando…' : 'Cadastrar'}
          </button>
        </div>
      </form>
    </Modal>
  )
}
