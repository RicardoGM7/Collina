import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import toast from 'react-hot-toast'
import { financeiroService } from '../../../services/financeiroService'
import type { Inadimplente, ResumoFechamento } from '../../../types/financeiro'
import { formatCurrency } from '../../../utils/formatters'
import { formatarCpf, formatarDataBr, validarCpf } from '../../associados/associadoUtils'
import { agruparTotais, formatarCompetencia } from '../mensalidadesUtils'
import './mensalidadesShared.css'
import './MensalidadesResumoSection.css'

function competenciaAtualIso(): string {
  const hoje = new Date()
  const ano = hoje.getFullYear()
  const mes = String(hoje.getMonth() + 1).padStart(2, '0')
  return `${ano}-${mes}`
}

export function MensalidadesResumoSection() {
  const navigate = useNavigate()
  const [ultimo, setUltimo] = useState<ResumoFechamento | null>(null)
  const [inadimplentes, setInadimplentes] = useState<Inadimplente[]>([])
  const [carregando, setCarregando] = useState(true)
  const [cpf, setCpf] = useState('')

  useEffect(() => {
    let ativo = true
    Promise.allSettled([
      financeiroService.ultimoFechamento(),
      financeiroService.inadimplentes(),
    ])
      .then(([resUltimo, resInad]) => {
        if (!ativo) return
        if (resUltimo.status === 'fulfilled') setUltimo(resUltimo.value)
        if (resInad.status === 'fulfilled') setInadimplentes(resInad.value)
      })
      .finally(() => {
        if (ativo) setCarregando(false)
      })
    return () => {
      ativo = false
    }
  }, [])

  const valorInadimplencia = agruparTotais(inadimplentes.map((i) => i.valor))
  const compExibida = formatarCompetencia(ultimo?.competencia ?? competenciaAtualIso())

  function handleBuscarCpf(e: FormEvent) {
    e.preventDefault()
    if (!validarCpf(cpf)) {
      toast.error('Informe um CPF válido com 11 dígitos.')
      return
    }
    navigate(`/mensalidades?cpf=${encodeURIComponent(cpf)}`)
  }

  return (
    <section className="mens-painel-grid" aria-label="Resumo de mensalidades">
      {/* Cartão 1: Processamento de mensalidades */}
      <article className="mens-painel-card">
        <h3 className="mens-painel-card-title">Processamento de mensalidades</h3>
        <div className="mens-painel-rows">
          <div className="mens-painel-row">
            <span className="mens-painel-pill">Mensalidades processadas</span>
            <strong>{carregando ? '...' : (ultimo?.processados ?? '—')}</strong>
          </div>
          <div className="mens-painel-row">
            <span className="mens-painel-pill">Data do processamento</span>
            <strong>
              {carregando
                ? '...'
                : ultimo?.dataExecucao
                  ? formatarDataBr(ultimo.dataExecucao)
                  : '—'}
            </strong>
          </div>
          <div className="mens-painel-row">
            <span className="mens-painel-pill">Competência atual</span>
            <strong>{compExibida}</strong>
          </div>
          <div className="mens-painel-row">
            <span className="mens-painel-pill mens-painel-pill-danger">
              Mensalidades com erro
            </span>
            <strong>{carregando ? '...' : (ultimo?.titularesComFalha.length ?? 0)}</strong>
          </div>
        </div>
        <button
          type="button"
          className="mens-btn-navy"
          onClick={() => navigate('/financeiro/mensalidades/processamento')}
        >
          Detalhes
        </button>
      </article>

      {/* Cartão 2: Inadimplentes */}
      <article className="mens-painel-card">
        <h3 className="mens-painel-card-title">Inadimplentes</h3>
        <div className="mens-painel-rows">
          <div className="mens-painel-row">
            <span className="mens-painel-pill mens-painel-pill-danger">
              Valor de inadimplência
            </span>
            <strong>{carregando ? '...' : formatCurrency(valorInadimplencia)}</strong>
          </div>
          <div className="mens-painel-row">
            <span className="mens-painel-pill">Total de inadimplentes</span>
            <strong>{carregando ? '...' : inadimplentes.length}</strong>
          </div>
        </div>
        <button
          type="button"
          className="mens-btn-navy"
          onClick={() => navigate('/financeiro/mensalidades/inadimplentes')}
        >
          Detalhes
        </button>
      </article>

      {/* Cartão 3: Buscar histórico de mensalidades e pagamentos */}
      <article className="mens-painel-card">
        <h3 className="mens-painel-card-title">
          Buscar histórico de mensalidades e pagamentos
        </h3>
        <form className="mens-painel-form" onSubmit={handleBuscarCpf}>
          <div className="mens-field">
            <label className="mens-field-label" htmlFor="mens-painel-cpf">
              CPF
            </label>
            <input
              id="mens-painel-cpf"
              type="text"
              className="mens-input"
              placeholder="000.000.000-00"
              value={cpf}
              onChange={(e) => setCpf(formatarCpf(e.target.value))}
            />
          </div>
          <button type="submit" className="mens-btn-navy">
            Buscar
          </button>
        </form>
      </article>
    </section>
  )
}
