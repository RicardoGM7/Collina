import { useEffect, useState, type FormEvent } from 'react'
import toast from 'react-hot-toast'
import { Modal } from '../../../components/Modal/Modal'
import { getApiErrorMessage } from '../../../services/api'
import { financeiroService } from '../../../services/financeiroService'
import { parseCurrencyInput } from '../../../utils/formatters'
import './mensalidadesShared.css'

export interface PessoaGastoOption {
  pessoaId: number
  rotulo: string
}

interface InserirGastoAvulsoModalProps {
  isOpen: boolean
  pessoas: PessoaGastoOption[]
  onClose: () => void
  onSuccess: () => void
}

export function InserirGastoAvulsoModal({
  isOpen,
  pessoas,
  onClose,
  onSuccess,
}: InserirGastoAvulsoModalProps) {
  const [pessoaId, setPessoaId] = useState<string>('')
  const [valorTxt, setValorTxt] = useState('')
  const [descricao, setDescricao] = useState('')
  const [salvando, setSalvando] = useState(false)
  const [erro, setErro] = useState('')

  useEffect(() => {
    if (!isOpen) return
    setPessoaId(pessoas.length > 0 ? String(pessoas[0].pessoaId) : '')
    setValorTxt('')
    setDescricao('')
    setErro('')
    setSalvando(false)
  }, [isOpen, pessoas])

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()

    const idNum = Number(pessoaId)
    if (!idNum) {
      setErro('Selecione a pessoa responsável pelo gasto.')
      return
    }
    const valor = parseCurrencyInput(valorTxt)
    if (!Number.isFinite(valor) || valor <= 0) {
      setErro('Informe um valor maior que zero.')
      return
    }
    if (Math.round(valor * 100) / 100 !== valor) {
      setErro('O valor deve ter no máximo 2 casas decimais.')
      return
    }
    if (!descricao.trim()) {
      setErro('Informe a descrição do gasto.')
      return
    }

    setSalvando(true)
    setErro('')
    try {
      await financeiroService.lancarGastoAvulso({
        pessoaId: idNum,
        valor,
        descricao: descricao.trim(),
      })
      toast.success('Gasto avulso lançado na mensalidade em aberto!')
      onSuccess()
      onClose()
    } catch (err) {
      setErro(getApiErrorMessage(err, 'Não foi possível lançar o gasto avulso.'))
    } finally {
      setSalvando(false)
    }
  }

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Inserir gasto avulso"
      subtitle="O valor será somado à mensalidade em aberto do titular."
      maxWidth={540}
    >
      <form className="mens-modal-body" onSubmit={handleSubmit} noValidate>
        {erro && (
          <div className="mens-alert mens-alert-erro" role="alert">
            {erro}
          </div>
        )}

        <div className="mens-detalhes-grid">
          <div className="mens-field">
            <label className="mens-field-label" htmlFor="mens-gasto-valor">
              Valor
            </label>
            <input
              id="mens-gasto-valor"
              type="text"
              className="mens-input"
              placeholder="R$ 0,00"
              value={valorTxt}
              onChange={(e) => setValorTxt(e.target.value)}
              disabled={salvando}
            />
          </div>

          <div className="mens-field">
            <label className="mens-field-label" htmlFor="mens-gasto-pessoa">
              Pessoa
            </label>
            <select
              id="mens-gasto-pessoa"
              className="mens-input"
              value={pessoaId}
              onChange={(e) => setPessoaId(e.target.value)}
              disabled={salvando || pessoas.length === 0}
            >
              {pessoas.length === 0 ? (
                <option value="">Lista de pessoas indisponível</option>
              ) : (
                pessoas.map((p) => (
                  <option key={p.pessoaId} value={p.pessoaId}>
                    {p.rotulo}
                  </option>
                ))
              )}
            </select>
          </div>
        </div>

        <div className="mens-field">
          <label className="mens-field-label" htmlFor="mens-gasto-desc">
            Descrição do gasto
          </label>
          <textarea
            id="mens-gasto-desc"
            className="mens-input"
            rows={4}
            maxLength={500}
            placeholder="Ex.: Consumo na lanchonete, taxa de convidado..."
            value={descricao}
            onChange={(e) => setDescricao(e.target.value)}
            disabled={salvando}
          />
          <span className="mens-field-hint">{descricao.length} / 500 caracteres</span>
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
          <button
            type="submit"
            className="mens-btn-primary"
            disabled={salvando || pessoas.length === 0}
          >
            {salvando ? 'SALVANDO...' : 'CONFIRMAR'}
          </button>
        </div>
      </form>
    </Modal>
  )
}
