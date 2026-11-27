import { useEffect, useState, type FormEvent } from 'react'
import toast from 'react-hot-toast'
import { Modal } from '../../../components/Modal/Modal'
import { getApiErrorMessage } from '../../../services/api'
import { financeiroService } from '../../../services/financeiroService'
import type { FormaPagamentoManual, Mensalidade } from '../../../types/financeiro'
import { formatCurrency } from '../../../utils/formatters'
import { formatarCompetencia } from '../mensalidadesUtils'
import './mensalidadesShared.css'

const FORMAS_MANUAIS: { valor: FormaPagamentoManual; label: string }[] = [
  { valor: 'PIX', label: 'PIX' },
  { valor: 'DINHEIRO', label: 'Dinheiro' },
  { valor: 'CARTAO', label: 'Cartão' },
  { valor: 'BOLETO', label: 'Boleto' },
]

interface RegistrarPagamentoModalProps {
  mensalidade: Mensalidade | null
  onClose: () => void
  onSuccess: () => void
}

export function RegistrarPagamentoModal({
  mensalidade,
  onClose,
  onSuccess,
}: RegistrarPagamentoModalProps) {
  const [formaPagamento, setFormaPagamento] = useState<FormaPagamentoManual>('PIX')
  const [salvando, setSalvando] = useState(false)
  const [erro, setErro] = useState('')

  useEffect(() => {
    if (!mensalidade) return
    setFormaPagamento('PIX')
    setErro('')
    setSalvando(false)
  }, [mensalidade])

  if (!mensalidade) return null

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    if (!mensalidade) return

    setSalvando(true)
    setErro('')
    try {
      await financeiroService.confirmarPagamento(mensalidade.id, {
        valor: mensalidade.valor,
        formaPagamento,
      })
      toast.success('Pagamento registrado com sucesso!')
      onSuccess()
      onClose()
    } catch (err) {
      setErro(getApiErrorMessage(err, 'Não foi possível registrar o pagamento.'))
    } finally {
      setSalvando(false)
    }
  }

  return (
    <Modal
      isOpen={mensalidade !== null}
      onClose={onClose}
      title="Pagamento"
      subtitle={`Mensalidade #${mensalidade.id} · Competência ${formatarCompetencia(mensalidade.competencia)}`}
      maxWidth={440}
    >
      <form className="mens-modal-body" onSubmit={handleSubmit} noValidate>
        {erro && (
          <div className="mens-alert mens-alert-erro" role="alert">
            {erro}
          </div>
        )}

        <div className="mens-pay-summary">
          <span>Valor total a ser pago</span>
          <strong>{formatCurrency(mensalidade.valor)}</strong>
        </div>

        <div className="mens-field">
          <label className="mens-field-label" htmlFor="mens-reg-forma">
            Forma de pagamento
          </label>
          <select
            id="mens-reg-forma"
            className="mens-input"
            value={formaPagamento}
            onChange={(e) => setFormaPagamento(e.target.value as FormaPagamentoManual)}
            disabled={salvando}
          >
            {FORMAS_MANUAIS.map((f) => (
              <option key={f.valor} value={f.valor}>
                {f.label}
              </option>
            ))}
          </select>
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
