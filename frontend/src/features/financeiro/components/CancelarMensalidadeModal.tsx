import { useEffect, useState, type FormEvent } from 'react'
import toast from 'react-hot-toast'
import { Modal } from '../../../components/Modal/Modal'
import { getApiErrorMessage } from '../../../services/api'
import { financeiroService } from '../../../services/financeiroService'
import type { Mensalidade } from '../../../types/financeiro'
import { formatCurrency, formatDecimalInput, parseCurrencyInput, toIsoLocalDate } from '../../../utils/formatters'
import { formatarCompetencia } from '../mensalidadesUtils'
import './mensalidadesShared.css'

interface CancelarMensalidadeModalProps {
  mensalidade: Mensalidade | null
  onClose: () => void
  onSuccess: () => void
}

export function CancelarMensalidadeModal({
  mensalidade,
  onClose,
  onSuccess,
}: CancelarMensalidadeModalProps) {
  const [novoValorTxt, setNovoValorTxt] = useState('')
  const [novoVencimento, setNovoVencimento] = useState('')
  const [justificativa, setJustificativa] = useState('')
  const [salvando, setSalvando] = useState(false)
  const [erro, setErro] = useState('')

  useEffect(() => {
    if (!mensalidade) return
    setNovoValorTxt(formatDecimalInput(mensalidade.valor))
    setNovoVencimento(mensalidade.vencimento ?? toIsoLocalDate(new Date()))
    setJustificativa('')
    setErro('')
    setSalvando(false)
  }, [mensalidade])

  if (!mensalidade) return null

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    if (!mensalidade) return

    const valor = parseCurrencyInput(novoValorTxt)
    if (!Number.isFinite(valor) || valor < 0) {
      setErro('Informe um novo valor válido (maior ou igual a zero).')
      return
    }
    if (Math.round(valor * 100) / 100 !== valor) {
      setErro('O valor deve ter no máximo 2 casas decimais.')
      return
    }
    if (!novoVencimento) {
      setErro('Informe a nova data de vencimento.')
      return
    }
    if (!justificativa.trim()) {
      setErro('Informe a justificativa do cancelamento.')
      return
    }

    setSalvando(true)
    setErro('')
    try {
      const substituta = await financeiroService.cancelarEReemitir(mensalidade.id, {
        justificativa: justificativa.trim(),
        novoVencimento,
        novoValor: valor,
      })
      toast.success(
        `Mensalidade cancelada. Nova mensalidade #${substituta.id} (${formatCurrency(substituta.valor)}) gerada como PENDENTE.`,
      )
      onSuccess()
      onClose()
    } catch (err) {
      setErro(getApiErrorMessage(err, 'Não foi possível cancelar e reemitir a mensalidade.'))
    } finally {
      setSalvando(false)
    }
  }

  return (
    <Modal
      isOpen={mensalidade !== null}
      onClose={onClose}
      title="Cancelamento de mensalidade"
      subtitle={`Mensalidade #${mensalidade.id} · Competência ${formatarCompetencia(mensalidade.competencia)}`}
      maxWidth={540}
    >
      <form className="mens-modal-body" onSubmit={handleSubmit} noValidate>
        <div className="mens-alert mens-alert-info">
          Será gerada uma nova mensalidade PENDENTE com o valor e vencimento informados.
        </div>

        {erro && (
          <div className="mens-alert mens-alert-erro" role="alert">
            {erro}
          </div>
        )}

        <div className="mens-detalhes-grid">
          <div className="mens-field">
            <label className="mens-field-label" htmlFor="mens-canc-valor">
              Novo valor
            </label>
            <input
              id="mens-canc-valor"
              type="text"
              className="mens-input"
              placeholder="0,00"
              value={novoValorTxt}
              onChange={(e) => setNovoValorTxt(e.target.value)}
              disabled={salvando}
            />
          </div>

          <div className="mens-field">
            <label className="mens-field-label" htmlFor="mens-canc-venc">
              Nova data de vencimento
            </label>
            <input
              id="mens-canc-venc"
              type="date"
              className="mens-input"
              value={novoVencimento}
              onChange={(e) => setNovoVencimento(e.target.value)}
              disabled={salvando}
            />
          </div>
        </div>

        <div className="mens-field">
          <label className="mens-field-label" htmlFor="mens-canc-just">
            Justificativa do cancelamento
          </label>
          <textarea
            id="mens-canc-just"
            className="mens-input"
            rows={4}
            maxLength={500}
            placeholder="Descreva o motivo do cancelamento..."
            value={justificativa}
            onChange={(e) => setJustificativa(e.target.value)}
            disabled={salvando}
          />
          <span className="mens-field-hint">{justificativa.length} / 500 caracteres</span>
        </div>

        <div className="mens-modal-actions">
          <button
            type="button"
            className="mens-btn-danger"
            onClick={onClose}
            disabled={salvando}
          >
            CANCELAR
          </button>
          <button type="submit" className="mens-btn-primary" disabled={salvando}>
            {salvando ? 'CONFIRMANDO...' : 'CONFIRMAR'}
          </button>
        </div>
      </form>
    </Modal>
  )
}
