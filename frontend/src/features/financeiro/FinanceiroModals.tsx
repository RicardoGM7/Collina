import { useEffect, useState, type FormEvent } from 'react'
import toast from 'react-hot-toast'
import { Modal } from '../../components/Modal/Modal'
import { getApiErrorMessage } from '../../services/api'
import { financeiroService } from '../../services/financeiroService'
import type {
  MovimentacaoResponse,
  PrioridadeGasto,
  RelatorioFechamentoResponse,
  SolicitacaoResponse,
  TipoMovimentacao,
} from '../../types/financeiro'
import { formatCurrency, formatDecimalInput, parseCurrencyInput } from '../../utils/formatters'
import {
  inputToApiDateTime,
  monthKey,
  ORIGEM_LABEL,
  PRIORIDADE_LABEL,
  TIPO_LABEL,
  toApiDateTime,
  toInputDateTime,
} from './financeiroUtils'

// Valida o valor digitado: maior que zero e no máximo 2 casas decimais.
function validarValor(texto: string): { valor?: number; erro?: string } {
  const valor = parseCurrencyInput(texto)
  if (!Number.isFinite(valor) || valor <= 0) return { erro: 'Informe um valor maior que zero.' }
  if (Math.round(valor * 100) / 100 !== valor) return { erro: 'O valor deve ter no máximo 2 casas decimais.' }
  return { valor }
}

interface ModalFooterProps {
  formId: string
  salvando: boolean
  rotulo: string
  onClose: () => void
}

function ModalFooter({ formId, salvando, rotulo, onClose }: ModalFooterProps) {
  return (
    <div className="fin-modal-footer">
      <button type="button" className="fin-btn-cancel" onClick={onClose} disabled={salvando}>
        Cancelar
      </button>
      <button type="submit" form={formId} className="fin-btn-save" disabled={salvando}>
        {salvando ? 'Salvando...' : rotulo}
      </button>
    </div>
  )
}

// Cadastrar / editar movimentação (receita ou despesa)

interface MovimentacaoModalProps {
  tipoNovo: TipoMovimentacao | null
  editando: MovimentacaoResponse | null
  onClose: () => void
  onSaved: () => void
}

export function MovimentacaoModal({ tipoNovo, editando, onClose, onSaved }: MovimentacaoModalProps) {
  const isOpen = tipoNovo !== null || editando !== null
  const tipo: TipoMovimentacao = editando?.tipo ?? tipoNovo ?? 'ENTRADA'
  const [descricao, setDescricao] = useState('')
  const [valorTxt, setValorTxt] = useState('')
  const [dataHora, setDataHora] = useState('')
  const [erro, setErro] = useState('')
  const [salvando, setSalvando] = useState(false)

  useEffect(() => {
    if (!isOpen) return
    setErro('')
    setSalvando(false)
    setDescricao(editando?.descricao ?? '')
    setValorTxt(editando ? formatDecimalInput(editando.valor) : '')
    setDataHora(toInputDateTime(new Date()))
  }, [isOpen, editando])

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    const { valor, erro: erroValor } = validarValor(valorTxt)
    if (valor === undefined) return setErro(erroValor ?? '')
    if (!editando && !dataHora) return setErro('Informe a data e a hora.')

    setSalvando(true)
    setErro('')
    try {
      if (editando) {
        await financeiroService.editarMovimentacao(editando.id, {
          tipo: editando.tipo,
          origem: editando.origem,
          valor,
          descricao: descricao.trim() || undefined,
          dataHora: editando.dataHora,
        })
        toast.success('Movimentação atualizada.')
      } else {
        await financeiroService.registrarMovimentacao({
          tipo,
          origem: tipo === 'ENTRADA' ? 'RECEITA_AVULSA' : 'CUSTO_OPERACIONAL',
          valor,
          descricao: descricao.trim() || undefined,
          dataHora: inputToApiDateTime(dataHora),
        })
        toast.success(tipo === 'ENTRADA' ? 'Receita cadastrada.' : 'Despesa cadastrada.')
      }
      onSaved()
      onClose()
    } catch (err) {
      setErro(getApiErrorMessage(err))
      setSalvando(false)
    }
  }

  const titulo = editando
    ? `Editar ${tipo === 'ENTRADA' ? 'receita' : 'despesa'}`
    : tipo === 'ENTRADA'
      ? 'Cadastrar receita'
      : 'Cadastrar despesa'

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={titulo}
      subtitle={
        editando
          ? `${TIPO_LABEL[editando.tipo]} · ${ORIGEM_LABEL[editando.origem]}. O tipo e a origem não podem ser alterados.`
          : 'O saldo do clube é atualizado na hora.'
      }
      maxWidth={460}
      footer={
        <ModalFooter formId="fin-mov-form" salvando={salvando} rotulo={editando ? 'Salvar' : 'Cadastrar'} onClose={onClose} />
      }
    >
      <form id="fin-mov-form" className="fin-form" onSubmit={handleSubmit} noValidate>
        <label className="fin-label">
          Descrição (opcional)
          <input
            className="fin-input"
            value={descricao}
            maxLength={120}
            placeholder={tipo === 'ENTRADA' ? 'Ex.: Aluguel de quadra avulso' : 'Ex.: Conta de energia'}
            onChange={(e) => setDescricao(e.target.value)}
          />
        </label>
        <div className="fin-form-row">
          <label className="fin-label">
            Valor
            <input
              className="fin-input"
              inputMode="decimal"
              value={valorTxt}
              placeholder="R$ 0,00"
              onChange={(e) => setValorTxt(e.target.value)}
            />
          </label>
          {!editando && (
            <label className="fin-label">
              Data e hora
              <input
                className="fin-input"
                type="datetime-local"
                value={dataHora}
                onChange={(e) => setDataHora(e.target.value)}
              />
            </label>
          )}
        </div>
        {erro && <p className="fin-erro" role="alert">{erro}</p>}
      </form>
    </Modal>
  )
}

// Cancelar movimentação (exige motivo; estorna o saldo)

interface CancelarModalProps {
  movimentacao: MovimentacaoResponse | null
  onClose: () => void
  onSaved: () => void
}

export function CancelarMovimentacaoModal({ movimentacao, onClose, onSaved }: CancelarModalProps) {
  const [motivo, setMotivo] = useState('')
  const [erro, setErro] = useState('')
  const [salvando, setSalvando] = useState(false)

  useEffect(() => {
    if (movimentacao) {
      setMotivo('')
      setErro('')
      setSalvando(false)
    }
  }, [movimentacao])

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    if (!movimentacao) return
    if (!motivo.trim()) return setErro('Informe o motivo do cancelamento.')
    setSalvando(true)
    setErro('')
    try {
      await financeiroService.cancelarMovimentacao(movimentacao.id, motivo.trim())
      toast.success('Movimentação cancelada e saldo estornado.')
      onSaved()
      onClose()
    } catch (err) {
      setErro(getApiErrorMessage(err))
      setSalvando(false)
    }
  }

  return (
    <Modal
      isOpen={movimentacao !== null}
      onClose={onClose}
      title="Cancelar movimentação"
      subtitle="O registro é mantido no histórico e o valor é estornado do saldo."
      maxWidth={460}
      footer={<ModalFooter formId="fin-cancel-form" salvando={salvando} rotulo="Cancelar movimentação" onClose={onClose} />}
    >
      <form id="fin-cancel-form" className="fin-form" onSubmit={handleSubmit} noValidate>
        {movimentacao && (
          <p className="fin-resumo">
            {TIPO_LABEL[movimentacao.tipo]} de <strong>{formatCurrency(movimentacao.valor)}</strong>
            {movimentacao.descricao ? ` · ${movimentacao.descricao}` : ''}
          </p>
        )}
        <label className="fin-label">
          Motivo do cancelamento
          <textarea
            className="fin-input fin-textarea"
            value={motivo}
            maxLength={200}
            placeholder="Ex.: Lançamento duplicado"
            onChange={(e) => setMotivo(e.target.value)}
          />
        </label>
        {erro && <p className="fin-erro" role="alert">{erro}</p>}
      </form>
    </Modal>
  )
}

// Nova solicitação de gasto

interface SolicitacaoModalProps {
  isOpen: boolean
  onClose: () => void
  onSaved: () => void
}

export function SolicitacaoGastoModal({ isOpen, onClose, onSaved }: SolicitacaoModalProps) {
  const [descricao, setDescricao] = useState('')
  const [valorTxt, setValorTxt] = useState('')
  const [prioridade, setPrioridade] = useState<PrioridadeGasto>('MEDIA')
  const [erro, setErro] = useState('')
  const [salvando, setSalvando] = useState(false)

  useEffect(() => {
    if (isOpen) {
      setDescricao('')
      setValorTxt('')
      setPrioridade('MEDIA')
      setErro('')
      setSalvando(false)
    }
  }, [isOpen])

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    if (!descricao.trim()) return setErro('Informe a descrição da solicitação.')
    const { valor, erro: erroValor } = validarValor(valorTxt)
    if (valor === undefined) return setErro(erroValor ?? '')
    setSalvando(true)
    setErro('')
    try {
      await financeiroService.cadastrarSolicitacao({
        descricao: descricao.trim(),
        valor,
        prioridade,
        dataSolicitacao: toApiDateTime(new Date()),
      })
      toast.success('Solicitação cadastrada.')
      onSaved()
      onClose()
    } catch (err) {
      setErro(getApiErrorMessage(err))
      setSalvando(false)
    }
  }

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Nova solicitação de gasto"
      subtitle="Ela fica pendente até ser efetivada. Efetivar gera a despesa e desconta do saldo."
      maxWidth={460}
      footer={<ModalFooter formId="fin-sol-form" salvando={salvando} rotulo="Cadastrar" onClose={onClose} />}
    >
      <form id="fin-sol-form" className="fin-form" onSubmit={handleSubmit} noValidate>
        <label className="fin-label">
          Descrição
          <input
            className="fin-input"
            value={descricao}
            maxLength={120}
            placeholder="Ex.: Compra de novas raquetes"
            onChange={(e) => setDescricao(e.target.value)}
          />
        </label>
        <div className="fin-form-row">
          <label className="fin-label">
            Valor
            <input
              className="fin-input"
              inputMode="decimal"
              value={valorTxt}
              placeholder="R$ 0,00"
              onChange={(e) => setValorTxt(e.target.value)}
            />
          </label>
          <label className="fin-label">
            Prioridade
            <select
              className="fin-input"
              value={prioridade}
              onChange={(e) => setPrioridade(e.target.value as PrioridadeGasto)}
            >
              {(Object.keys(PRIORIDADE_LABEL) as PrioridadeGasto[]).map((p) => (
                <option key={p} value={p}>{PRIORIDADE_LABEL[p]}</option>
              ))}
            </select>
          </label>
        </div>
        {erro && <p className="fin-erro" role="alert">{erro}</p>}
      </form>
    </Modal>
  )
}

// Efetivar solicitação: gera a despesa e desconta do saldo

interface EfetivarModalProps {
  solicitacao: SolicitacaoResponse | null
  saldoAtual: number
  onClose: () => void
  onSaved: () => void
}

export function EfetivarSolicitacaoModal({ solicitacao, saldoAtual, onClose, onSaved }: EfetivarModalProps) {
  const [erro, setErro] = useState('')
  const [salvando, setSalvando] = useState(false)

  useEffect(() => {
    if (solicitacao) {
      setErro('')
      setSalvando(false)
    }
  }, [solicitacao])

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    if (!solicitacao) return
    setSalvando(true)
    setErro('')
    try {
      await financeiroService.registrarMovimentacao({
        tipo: 'SAIDA',
        origem: 'SOLICITACAO_GASTO',
        valor: solicitacao.valor,
        descricao: solicitacao.descricao,
        dataHora: toApiDateTime(new Date()),
        solicitacaoId: solicitacao.id,
      })
      toast.success('Solicitação efetivada e despesa registrada.')
      onSaved()
      onClose()
    } catch (err) {
      setErro(getApiErrorMessage(err))
      setSalvando(false)
    }
  }

  return (
    <Modal
      isOpen={solicitacao !== null}
      onClose={onClose}
      title="Efetivar solicitação"
      subtitle="Será registrada uma despesa e o valor sairá do saldo do clube."
      maxWidth={460}
      footer={<ModalFooter formId="fin-efetivar-form" salvando={salvando} rotulo="Efetivar" onClose={onClose} />}
    >
      <form id="fin-efetivar-form" className="fin-form" onSubmit={handleSubmit}>
        {solicitacao && (
          <>
            <p className="fin-resumo">
              <strong>{solicitacao.descricao}</strong>
              <br />
              Prioridade {PRIORIDADE_LABEL[solicitacao.prioridade]} · {formatCurrency(solicitacao.valor)}
            </p>
            <p className="fin-resumo">
              Saldo: {formatCurrency(saldoAtual)} → <strong>{formatCurrency(saldoAtual - solicitacao.valor)}</strong>
            </p>
          </>
        )}
        {erro && <p className="fin-erro" role="alert">{erro}</p>}
      </form>
    </Modal>
  )
}

// Relatório de fechamento financeiro (RF-F03)

interface RelatorioModalProps {
  isOpen: boolean
  onClose: () => void
}

export function RelatorioFechamentoModal({ isOpen, onClose }: RelatorioModalProps) {
  const [competencia, setCompetencia] = useState(monthKey(new Date()))
  const [relatorio, setRelatorio] = useState<RelatorioFechamentoResponse | null>(null)
  const [carregando, setCarregando] = useState(false)
  const [erro, setErro] = useState('')

  useEffect(() => {
    if (!isOpen || !/^\d{4}-\d{2}$/.test(competencia)) return
    let ativo = true
    setCarregando(true)
    setErro('')
    financeiroService
      .gerarFechamento(competencia)
      .then((dados) => ativo && setRelatorio(dados))
      .catch((err) => {
        if (!ativo) return
        setRelatorio(null)
        setErro(getApiErrorMessage(err))
      })
      .finally(() => ativo && setCarregando(false))
    return () => {
      ativo = false
    }
  }, [isOpen, competencia])

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Relatório de fechamento"
      subtitle="Totais do mês (movimentações canceladas não entram)."
      maxWidth={560}
      footer={
        <div className="fin-modal-footer">
          <button type="button" className="fin-btn-cancel" onClick={onClose}>Fechar</button>
        </div>
      }
    >
      <div className="fin-form">
        <label className="fin-label">
          Competência
          <input
            className="fin-input"
            type="month"
            value={competencia}
            onChange={(e) => setCompetencia(e.target.value)}
          />
        </label>
        {erro && <p className="fin-erro" role="alert">{erro}</p>}
        {carregando && <p className="fin-resumo">Carregando...</p>}
        {relatorio && !carregando && (
          <>
            <div className="fin-rel-totais">
              <div>
                <span className="fin-rel-label">Receitas do período</span>
                <strong className="fin-rel-entrada">{formatCurrency(relatorio.totalEntradas)}</strong>
              </div>
              <div>
                <span className="fin-rel-label">Despesas do período</span>
                <strong className="fin-rel-saida">{formatCurrency(relatorio.totalSaidas)}</strong>
              </div>
              <div>
                <span className="fin-rel-label">Saldo do período</span>
                <strong>{formatCurrency(relatorio.saldoPeriodo)}</strong>
              </div>
            </div>
            {relatorio.porCategoria.length === 0 ? (
              <p className="fin-resumo">Nenhuma movimentação neste mês.</p>
            ) : (
              <ul className="fin-rel-lista">
                {relatorio.porCategoria.map((item) => (
                  <li key={`${item.tipo}-${item.origem}`}>
                    <span className={`fin-chip ${item.tipo === 'ENTRADA' ? 'fin-chip-entrada' : 'fin-chip-saida'}`}>
                      {TIPO_LABEL[item.tipo]}
                    </span>
                    <span>{ORIGEM_LABEL[item.origem]}</span>
                    <strong>{formatCurrency(item.total)}</strong>
                  </li>
                ))}
              </ul>
            )}
          </>
        )}
      </div>
    </Modal>
  )
}
