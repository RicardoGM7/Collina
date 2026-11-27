import { useCallback, useEffect, useRef, useState } from 'react'
import toast from 'react-hot-toast'
import { Check, Copy, CreditCard, QrCode, RefreshCw } from 'lucide-react'
import { Modal } from '../../../components/Modal/Modal'
import { getApiErrorMessage } from '../../../services/api'
import { financeiroService } from '../../../services/financeiroService'
import type {
  Cobranca,
  FormaPagamentoGateway,
  Mensalidade,
  RespostaSimulacaoGateway,
  ResultadoSimulacaoGateway,
} from '../../../types/financeiro'
import { formatCurrency, parseCurrencyInput } from '../../../utils/formatters'
import { formatarCompetencia } from '../mensalidadesUtils'
import './mensalidadesShared.css'

interface PagamentoGatewayModalProps {
  mensalidade: Mensalidade | null
  formaInicial?: FormaPagamentoGateway | null
  onClose: () => void
  onPago: () => void
}

function formatarContagem(segundosRestantes: number): string {
  const min = Math.floor(segundosRestantes / 60)
  const seg = segundosRestantes % 60
  return `${String(min).padStart(2, '0')}:${String(seg).padStart(2, '0')}`
}

function segundosAte(expiraEm: string): number {
  const alvo = new Date(expiraEm).getTime()
  if (Number.isNaN(alvo)) return 0
  return Math.max(0, Math.floor((alvo - Date.now()) / 1000))
}

export function PagamentoGatewayModal({
  mensalidade,
  formaInicial = null,
  onClose,
  onPago,
}: PagamentoGatewayModalProps) {
  const isOpen = mensalidade !== null
  const [forma, setForma] = useState<FormaPagamentoGateway>('PIX')
  const [cobranca, setCobranca] = useState<Cobranca | null>(null)
  const [iniciando, setIniciando] = useState(false)
  const [erro, setErro] = useState('')
  const [estadoFinal, setEstadoFinal] = useState<'RECUSADO' | 'EXPIRADO' | null>(null)
  const [segundos, setSegundos] = useState(0)
  const [copiado, setCopiado] = useState(false)

  // Painel de simulação (somente VITE_GATEWAY_STUB === 'true')
  const stubAtivo = import.meta.env.VITE_GATEWAY_STUB === 'true'
  const [valorSimulacaoTxt, setValorSimulacaoTxt] = useState('')
  const [simulando, setSimulando] = useState(false)
  const [ultimaRespostaStub, setUltimaRespostaStub] = useState<RespostaSimulacaoGateway | null>(null)

  const ultimaConsultaZeradoRef = useRef(false)

  const iniciarCobranca = useCallback(
    async (formaEscolhida: FormaPagamentoGateway) => {
      if (!mensalidade) return
      setIniciando(true)
      setErro('')
      setEstadoFinal(null)
      setUltimaRespostaStub(null)
      ultimaConsultaZeradoRef.current = false
      try {
        const resp = await financeiroService.iniciarCobranca(mensalidade.id, formaEscolhida)
        setCobranca(resp)
        setSegundos(segundosAte(resp.expiraEm))
      } catch (err) {
        setErro(getApiErrorMessage(err, 'Não foi possível iniciar a cobrança.'))
      } finally {
        setIniciando(false)
      }
    },
    [mensalidade],
  )

  useEffect(() => {
    if (!isOpen) return
    setCobranca(null)
    setErro('')
    setEstadoFinal(null)
    setCopiado(false)
    setValorSimulacaoTxt('')
    setUltimaRespostaStub(null)
    ultimaConsultaZeradoRef.current = false

    if (formaInicial) {
      setForma(formaInicial)
      void iniciarCobranca(formaInicial)
    } else {
      setForma('PIX')
    }
  }, [isOpen, formaInicial, iniciarCobranca])

  const consultarStatusPagamento = useCallback(async () => {
    if (!cobranca) return
    try {
      const pagamentos = await financeiroService.meusPagamentos()
      const atual = pagamentos.find((p) => p.id === cobranca.pagamentoId)
      if (!atual) return

      if (atual.status === 'CONFIRMADO') {
        toast.success('Pagamento confirmado com sucesso!')
        onPago()
        onClose()
      } else if (atual.status === 'RECUSADO' || atual.status === 'EXPIRADO') {
        setEstadoFinal(atual.status)
      }
    } catch {
      // Falha pontual no polling não interrompe a próxima tentativa
    }
  }, [cobranca, onClose, onPago])

  // Polling a cada 4s enquanto houver cobrança ativa
  useEffect(() => {
    if (!isOpen || !cobranca || estadoFinal !== null) return
    const timer = window.setInterval(() => {
      void consultarStatusPagamento()
    }, 4000)
    return () => window.clearInterval(timer)
  }, [isOpen, cobranca, estadoFinal, consultarStatusPagamento])

  // Contagem regressiva até expiraEm
  useEffect(() => {
    if (!isOpen || !cobranca || estadoFinal !== null) return
    const tick = window.setInterval(() => {
      const rest = segundosAte(cobranca.expiraEm)
      setSegundos(rest)
      if (rest === 0 && !ultimaConsultaZeradoRef.current) {
        ultimaConsultaZeradoRef.current = true
        void consultarStatusPagamento().then(() => {
          setEstadoFinal((prev) => prev ?? 'EXPIRADO')
        })
      }
    }, 1000)
    return () => window.clearInterval(tick)
  }, [isOpen, cobranca, estadoFinal, consultarStatusPagamento])

  async function copiarCodigoPix() {
    if (!cobranca?.codigoPix) return
    try {
      await navigator.clipboard.writeText(cobranca.codigoPix)
      setCopiado(true)
      toast.success('Código Pix copiado!')
      window.setTimeout(() => setCopiado(false), 2000)
    } catch {
      toast.error('Não foi possível copiar o código automaticamente.')
    }
  }

  async function simular(resultado: ResultadoSimulacaoGateway) {
    if (!cobranca) return
    let valorDivergente: number | undefined
    if (valorSimulacaoTxt.trim()) {
      const parsed = parseCurrencyInput(valorSimulacaoTxt)
      if (!Number.isFinite(parsed) || parsed < 0) {
        toast.error('Informe um valor válido para simulação.')
        return
      }
      valorDivergente = parsed
    }

    setSimulando(true)
    try {
      const resp = await financeiroService.simularGateway(
        cobranca.pagamentoId,
        resultado,
        valorDivergente,
      )
      setUltimaRespostaStub(resp)
      await consultarStatusPagamento()
    } catch (err) {
      toast.error(getApiErrorMessage(err, 'Erro ao simular pagamento no stub.'))
    } finally {
      setSimulando(false)
    }
  }

  function voltarParaEscolha() {
    setCobranca(null)
    setEstadoFinal(null)
    setErro('')
    setUltimaRespostaStub(null)
  }

  if (!mensalidade) return null

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Pagamento de mensalidade"
      subtitle={`Competência ${formatarCompetencia(mensalidade.competencia)} · Total ${formatCurrency(mensalidade.valor)}`}
      maxWidth={520}
    >
      <div className="mens-modal-body">
        {erro && (
          <div className="mens-alert mens-alert-erro" role="alert">
            {erro}
          </div>
        )}

        {!cobranca ? (
          <>
            <div className="mens-pay-summary">
              <span>Valor total a ser pago</span>
              <strong>{formatCurrency(mensalidade.valor)}</strong>
            </div>

            <div className="mens-field">
              <span className="mens-field-label">Forma de pagamento</span>
              <div className="mens-pay-methods">
                <button
                  type="button"
                  className={`mens-pay-method-btn${forma === 'PIX' ? ' is-selected' : ''}`}
                  onClick={() => setForma('PIX')}
                  disabled={iniciando}
                >
                  <QrCode size={18} />
                  <span>PIX</span>
                </button>
                <button
                  type="button"
                  className={`mens-pay-method-btn${forma === 'CARTAO' ? ' is-selected' : ''}`}
                  onClick={() => setForma('CARTAO')}
                  disabled={iniciando}
                >
                  <CreditCard size={18} />
                  <span>CARTÃO</span>
                </button>
              </div>
            </div>

            <div className="mens-modal-actions">
              <button
                type="button"
                className="mens-btn-secondary"
                onClick={onClose}
                disabled={iniciando}
              >
                Cancelar
              </button>
              <button
                type="button"
                className="mens-btn-primary"
                onClick={() => void iniciarCobranca(forma)}
                disabled={iniciando}
              >
                {iniciando ? 'Gerando cobrança...' : 'Confirmar'}
              </button>
            </div>
          </>
        ) : estadoFinal !== null ? (
          <div className="mens-pay-final">
            <div className="mens-alert mens-alert-erro" role="alert">
              {estadoFinal === 'RECUSADO'
                ? 'O pagamento foi recusado pelo gateway.'
                : 'O tempo limite da cobrança expirou.'}
            </div>
            <div className="mens-modal-actions">
              <button type="button" className="mens-btn-secondary" onClick={onClose}>
                Fechar
              </button>
              <button type="button" className="mens-btn-primary" onClick={voltarParaEscolha}>
                <RefreshCw size={15} />
                <span>Tentar novamente</span>
              </button>
            </div>
          </div>
        ) : (
          <>
            <div className="mens-pay-countdown">
              <span>Aguardando confirmação</span>
              <strong>Expira em {formatarContagem(segundos)}</strong>
            </div>

            {cobranca.formaPagamento === 'PIX' && (
              <div className="mens-field">
                <label className="mens-field-label" htmlFor="mens-pix-code">
                  Código Pix (copia e cola)
                </label>
                <div className="mens-copy-row">
                  <input
                    id="mens-pix-code"
                    type="text"
                    readOnly
                    className="mens-input"
                    value={cobranca.codigoPix ?? ''}
                  />
                  <button
                    type="button"
                    className="mens-btn-secondary"
                    onClick={() => void copiarCodigoPix()}
                  >
                    {copiado ? <Check size={15} /> : <Copy size={15} />}
                    <span>{copiado ? 'Copiado' : 'Copiar'}</span>
                  </button>
                </div>
              </div>
            )}

            {cobranca.formaPagamento === 'CARTAO' && (
              <div className="mens-field">
                <label className="mens-field-label" htmlFor="mens-card-url">
                  Link do checkout (ambiente stub)
                </label>
                <input
                  id="mens-card-url"
                  type="text"
                  readOnly
                  className="mens-input"
                  value={cobranca.urlPagamento ?? ''}
                />
                <span className="mens-field-hint">
                  No modo stub esta URL é fictícia e não deve ser aberta em outra aba.
                </span>
              </div>
            )}

            {stubAtivo && (
              <div className="mens-stub-panel">
                <div className="mens-stub-header">
                  <strong>Simular pagamento (Stub)</strong>
                  {ultimaRespostaStub && (
                    <span className="mens-stub-resp">Resposta: {ultimaRespostaStub}</span>
                  )}
                </div>
                <div className="mens-field">
                  <label className="mens-field-label" htmlFor="mens-stub-valor">
                    Valor opcional (para simular divergência)
                  </label>
                  <input
                    id="mens-stub-valor"
                    type="text"
                    className="mens-input"
                    placeholder={`Padrão: ${formatCurrency(cobranca.valor)}`}
                    value={valorSimulacaoTxt}
                    onChange={(e) => setValorSimulacaoTxt(e.target.value)}
                    disabled={simulando}
                  />
                </div>
                <div className="mens-stub-actions">
                  <button
                    type="button"
                    className="mens-btn-stub-ok"
                    onClick={() => void simular('APROVADO')}
                    disabled={simulando}
                  >
                    Aprovado
                  </button>
                  <button
                    type="button"
                    className="mens-btn-stub-err"
                    onClick={() => void simular('RECUSADO')}
                    disabled={simulando}
                  >
                    Recusado
                  </button>
                  <button
                    type="button"
                    className="mens-btn-stub-warn"
                    onClick={() => void simular('EXPIRADO')}
                    disabled={simulando}
                  >
                    Expirado
                  </button>
                  <button
                    type="button"
                    className="mens-btn-secondary"
                    onClick={() => void simular('PENDENTE')}
                    disabled={simulando}
                  >
                    Pendente
                  </button>
                </div>
              </div>
            )}

            <div className="mens-modal-actions">
              <button type="button" className="mens-btn-secondary" onClick={voltarParaEscolha}>
                Trocar forma
              </button>
              <button type="button" className="mens-btn-secondary" onClick={onClose}>
                Fechar
              </button>
            </div>
          </>
        )}
      </div>
    </Modal>
  )
}
