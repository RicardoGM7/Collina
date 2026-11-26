import { useCallback, useEffect, useMemo, useState } from 'react'
import { Search } from 'lucide-react'
import { getApiErrorMessage } from '../../services/api'
import { financeiroService } from '../../services/financeiroService'
import type {
  FormaPagamentoGateway,
  Mensalidade,
  Pagamento,
  StatusMensalidade,
  StatusPagamento,
} from '../../types/financeiro'
import { formatCurrency, formatDateFull } from '../../utils/formatters'
import { formatarDataBr } from '../associados/associadoUtils'
import { EstadoVazio } from './components/EstadoVazio'
import { FiltroStatusTabs } from './components/FiltroStatusTabs'
import { MensalidadeDetalhesModal } from './components/MensalidadeDetalhesModal'
import { PagamentoDetalhesModal } from './components/PagamentoDetalhesModal'
import { PagamentoGatewayModal } from './components/PagamentoGatewayModal'
import { StatusMensalidadeBadge } from './components/StatusMensalidadeBadge'
import { StatusPagamentoBadge } from './components/StatusPagamentoBadge'
import { ValorMonetario } from './components/ValorMonetario'
import {
  competenciaDentroDosMeses,
  emAberto,
  formatarCompetencia,
  situacaoFinanceira,
  ultimaFechada,
} from './mensalidadesUtils'
import './components/mensalidadesShared.css'
import './MinhasMensalidadesPanel.css'

type FiltroMensalidade = 'TODOS' | Exclude<StatusMensalidade, 'EM_ABERTO'>
type FiltroPagamento = 'TODOS' | StatusPagamento
type FiltroPeriodo = 'todos' | '3' | '6' | '12'

function formatarSegundosMmSs(expiraEm?: string | null): string | undefined {
  if (!expiraEm) return undefined
  const diff = Math.floor((new Date(expiraEm).getTime() - Date.now()) / 1000)
  if (Number.isNaN(diff) || diff <= 0) return undefined
  const min = Math.floor(diff / 60)
  const seg = diff % 60
  return `${String(min).padStart(2, '0')}:${String(seg).padStart(2, '0')}`
}

export function MinhasMensalidadesPanel() {
  const [mensalidades, setMensalidades] = useState<Mensalidade[]>([])
  const [pagamentos, setPagamentos] = useState<Pagamento[]>([])
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState('')

  // Filtros de mensalidades
  const [statusMensFiltro, setStatusMensFiltro] = useState<FiltroMensalidade>('TODOS')
  const [periodoMens, setPeriodoMens] = useState<FiltroPeriodo>('todos')
  const [buscaMens, setBuscaMens] = useState('')

  // Filtros de pagamentos
  const [statusPagFiltro, setStatusPagFiltro] = useState<FiltroPagamento>('TODOS')
  const [buscaPag, setBuscaPag] = useState('')

  // Modais
  const [mensalidadeParaPagar, setMensalidadeParaPagar] = useState<Mensalidade | null>(null)
  const [formaInicialModal, setFormaInicialModal] = useState<FormaPagamentoGateway | null>(null)
  const [mensalidadeDetalhes, setMensalidadeDetalhes] = useState<Mensalidade | null>(null)
  const [pagamentoDetalhes, setPagamentoDetalhes] = useState<Pagamento | null>(null)

  const carregarDados = useCallback(async () => {
    setCarregando(true)
    setErro('')
    try {
      const [listaMens, listaPags] = await Promise.all([
        financeiroService.minhasMensalidades(),
        financeiroService.meusPagamentos(),
      ])
      setMensalidades(listaMens)
      setPagamentos(listaPags)
    } catch (err) {
      setErro(getApiErrorMessage(err, 'Não foi possível carregar suas mensalidades.'))
    } finally {
      setCarregando(false)
    }
  }, [])

  useEffect(() => {
    void carregarDados()
  }, [carregarDados])

  const situacao = useMemo(() => situacaoFinanceira(mensalidades), [mensalidades])
  const fechada = useMemo(() => ultimaFechada(mensalidades), [mensalidades])
  const aberta = useMemo(() => emAberto(mensalidades), [mensalidades])

  const mensalidadesFiltradas = useMemo(() => {
    const termo = buscaMens.trim().toLowerCase()
    const meses = periodoMens === 'todos' ? 'todos' : Number(periodoMens)

    return mensalidades.filter((m) => {
      if (statusMensFiltro !== 'TODOS' && m.status !== statusMensFiltro) return false
      if (!competenciaDentroDosMeses(m.competencia, meses)) return false
      if (termo) {
        const comp = formatarCompetencia(m.competencia).toLowerCase()
        if (!comp.includes(termo) && !String(m.id).includes(termo)) return false
      }
      return true
    })
  }, [mensalidades, statusMensFiltro, periodoMens, buscaMens])

  const pagamentosFiltrados = useMemo(() => {
    const termo = buscaPag.trim().toLowerCase()
    return pagamentos.filter((p) => {
      if (statusPagFiltro !== 'TODOS' && p.status !== statusPagFiltro) return false
      if (termo) {
        const forma = p.formaPagamento.toLowerCase()
        if (!forma.includes(termo) && !String(p.id).includes(termo)) return false
      }
      return true
    })
  }, [pagamentos, statusPagFiltro, buscaPag])

  function abrirModalPagamento(mensalidade: Mensalidade, forma: FormaPagamentoGateway | null = null) {
    setFormaInicialModal(forma)
    setMensalidadeParaPagar(mensalidade)
  }

  function retomarPagamento(pagamento: Pagamento) {
    if (!pagamento.mensalidadeId) return
    const mens = mensalidades.find((m) => m.id === pagamento.mensalidadeId)
    if (!mens) return
    const forma: FormaPagamentoGateway | null =
      pagamento.formaPagamento === 'PIX' || pagamento.formaPagamento === 'CARTAO'
        ? pagamento.formaPagamento
        : null
    abrirModalPagamento(mens, forma)
  }

  if (carregando) {
    return <div className="mens-loading">Carregando mensalidades e pagamentos...</div>
  }

  if (erro) {
    return (
      <div className="mens-panel">
        <div className="mens-alert mens-alert-erro" role="alert">
          {erro}
        </div>
        <div>
          <button type="button" className="mens-btn-secondary" onClick={() => void carregarDados()}>
            Tentar novamente
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className="mens-panel">
      {/* Cartões de resumo */}
      <section className="mens-cards-grid">
        <article className="mens-summary-card">
          <h3 className="mens-summary-title">Status atual:</h3>
          <span className={`mens-situacao-pill mens-situacao-${situacao.variant}`}>
            {situacao.label}
          </span>
          <div className="mens-summary-rows">
            <div className="mens-summary-row">
              <span>Mensalidades atrasadas</span>
              <strong>{situacao.atrasadas}</strong>
            </div>
            <div className="mens-summary-row">
              <span>Mensalidades pendentes</span>
              <strong>{situacao.pendentes}</strong>
            </div>
          </div>
        </article>

        <article className="mens-summary-card">
          <h3 className="mens-summary-title">Última mensalidade fechada</h3>
          {fechada ? (
            <>
              <div className="mens-summary-rows">
                <div className="mens-summary-row">
                  <span>Valor Total</span>
                  <strong>{formatCurrency(fechada.valor)}</strong>
                </div>
                <div className="mens-summary-row">
                  <span>Vencimento</span>
                  <strong>{formatarDataBr(fechada.vencimento)}</strong>
                </div>
              </div>
              {fechada.status === 'PENDENTE' && (
                <button
                  type="button"
                  className="mens-btn-primary"
                  onClick={() => abrirModalPagamento(fechada)}
                >
                  PAGAR
                </button>
              )}
              {fechada.status === 'ATRASADA' && (
                <p className="mens-summary-muted">Procure a recepção</p>
              )}
              {fechada.status === 'PAGA' && (
                <div style={{ alignSelf: 'center' }}>
                  <StatusMensalidadeBadge status="PAGA" />
                </div>
              )}
            </>
          ) : (
            <p className="mens-summary-muted">Nenhuma mensalidade fechada encontrada.</p>
          )}
        </article>

        <article className="mens-summary-card">
          <h3 className="mens-summary-title">Mensalidade em aberto</h3>
          {aberta ? (
            <>
              <div className="mens-summary-rows">
                {aberta.valorBase !== undefined && (
                  <div className="mens-summary-row">
                    <span>Valor base</span>
                    <strong>{formatCurrency(aberta.valorBase)}</strong>
                  </div>
                )}
              </div>
              {aberta.gastosAvulsos && aberta.gastosAvulsos.length > 0 && (
                <ul className="mens-summary-gastos">
                  {aberta.gastosAvulsos.map((g) => (
                    <li key={g.id}>
                      <span>{g.descricao}</span>
                      <span>{formatCurrency(g.valor)}</span>
                    </li>
                  ))}
                </ul>
              )}
              <div className="mens-total-pill">
                <span>Total até agora</span>
                <strong>{formatCurrency(aberta.valor)}</strong>
              </div>
              {aberta.dataFechamento && (
                <p className="mens-summary-muted">
                  Fechamento - {formatarDataBr(aberta.dataFechamento)}
                </p>
              )}
            </>
          ) : (
            <p className="mens-summary-muted">Nenhuma mensalidade em aberto no momento.</p>
          )}
        </article>
      </section>

      {/* Tabela: Histórico de mensalidades */}
      <section className="mens-section-card">
        <div className="mens-section-header">
          <h3 className="mens-section-title">Histórico de mensalidades</h3>
          <div className="mens-toolbar">
            <select
              className="mens-select"
              aria-label="Filtrar período de mensalidades"
              value={periodoMens}
              onChange={(e) => setPeriodoMens(e.target.value as FiltroPeriodo)}
            >
              <option value="todos">Todo o período</option>
              <option value="3">Últimos 3 meses</option>
              <option value="6">Últimos 6 meses</option>
              <option value="12">Últimos 12 meses</option>
            </select>
            <div className="mens-search-wrap">
              <Search size={14} className="mens-search-icon" />
              <input
                type="text"
                className="mens-search-input"
                placeholder="Buscar MM/AA ou ID..."
                value={buscaMens}
                onChange={(e) => setBuscaMens(e.target.value)}
              />
            </div>
          </div>
        </div>

        <FiltroStatusTabs<FiltroMensalidade>
          ativo={statusMensFiltro}
          onChange={setStatusMensFiltro}
          opcoes={[
            { valor: 'TODOS', label: 'Todos', quantidade: mensalidades.length },
            { valor: 'PAGA', label: 'PAGA' },
            { valor: 'PENDENTE', label: 'PENDENTE' },
            { valor: 'ATRASADA', label: 'ATRASADA' },
            { valor: 'CANCELADA', label: 'CANCELADA' },
          ]}
        />

        {mensalidadesFiltradas.length === 0 ? (
          <EstadoVazio
            titulo="Nenhuma mensalidade encontrada"
            descricao="Ajuste os filtros ou aguarde o fechamento da competência atual."
          />
        ) : (
          <div className="mens-table-wrap">
            <table className="mens-table">
              <thead>
                <tr>
                  <th>Competência</th>
                  <th>Vencimento</th>
                  <th>Status</th>
                  <th>Total</th>
                  <th>Ações</th>
                </tr>
              </thead>
              <tbody>
                {mensalidadesFiltradas.map((m) => (
                  <tr key={m.id}>
                    <td>
                      <strong>{formatarCompetencia(m.competencia)}</strong>
                    </td>
                    <td>{formatarDataBr(m.vencimento)}</td>
                    <td>
                      <StatusMensalidadeBadge status={m.status} />
                    </td>
                    <td>
                      <ValorMonetario
                        valor={m.valor}
                        riscado={m.status === 'CANCELADA'}
                      />
                    </td>
                    <td>
                      <div className="mens-row-actions">
                        {m.status === 'PENDENTE' && (
                          <button
                            type="button"
                            className="mens-btn-pay-sm"
                            onClick={() => abrirModalPagamento(m)}
                          >
                            Pagar
                          </button>
                        )}
                        <button
                          type="button"
                          className="mens-btn-secondary"
                          onClick={() => setMensalidadeDetalhes(m)}
                        >
                          Detalhes
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {/* Tabela: Histórico de pagamentos */}
      <section className="mens-section-card">
        <div className="mens-section-header">
          <h3 className="mens-section-title">Histórico de pagamentos</h3>
          <div className="mens-toolbar">
            <div className="mens-search-wrap">
              <Search size={14} className="mens-search-icon" />
              <input
                type="text"
                className="mens-search-input"
                placeholder="Buscar forma ou ID..."
                value={buscaPag}
                onChange={(e) => setBuscaPag(e.target.value)}
              />
            </div>
          </div>
        </div>

        <FiltroStatusTabs<FiltroPagamento>
          ativo={statusPagFiltro}
          onChange={setStatusPagFiltro}
          opcoes={[
            { valor: 'TODOS', label: 'Todos', quantidade: pagamentos.length },
            { valor: 'CONFIRMADO', label: 'CONFIRMADOS' },
            { valor: 'AGUARDANDO', label: 'AGUARDANDO' },
            { valor: 'EXPIRADO', label: 'EXPIRADOS' },
            { valor: 'RECUSADO', label: 'RECUSADOS' },
          ]}
        />

        {pagamentosFiltrados.length === 0 ? (
          <EstadoVazio
            titulo="Nenhum pagamento encontrado"
            descricao="Seus pagamentos realizados ou em andamento aparecerão aqui."
          />
        ) : (
          <div className="mens-table-wrap">
            <table className="mens-table">
              <thead>
                <tr>
                  <th>Forma</th>
                  <th>Data</th>
                  <th>Status do pagamento</th>
                  <th>Total</th>
                  <th>Ações</th>
                </tr>
              </thead>
              <tbody>
                {pagamentosFiltrados.map((p) => {
                  const dataExibida = p.dataCriacao ?? p.dataPagamento
                  const contagem =
                    p.status === 'AGUARDANDO' ? formatarSegundosMmSs(p.expiraEm) : undefined
                  const podeRetomar =
                    p.status === 'AGUARDANDO' &&
                    p.mensalidadeId !== null &&
                    mensalidades.some(
                      (m) => m.id === p.mensalidadeId && m.status === 'PENDENTE',
                    )
                  return (
                    <tr key={p.id}>
                      <td>
                        <strong>{p.formaPagamento}</strong>
                      </td>
                      <td>{dataExibida ? formatDateFull(dataExibida) : '—'}</td>
                      <td>
                        <StatusPagamentoBadge status={p.status} sufixo={contagem} />
                      </td>
                      <td>
                        <ValorMonetario
                          valor={p.valor}
                          riscado={p.status === 'RECUSADO' || p.status === 'EXPIRADO'}
                        />
                      </td>
                      <td>
                        <div className="mens-row-actions">
                          {podeRetomar && (
                            <button
                              type="button"
                              className="mens-btn-pay-sm"
                              onClick={() => retomarPagamento(p)}
                            >
                              Pagar
                            </button>
                          )}
                          <button
                            type="button"
                            className="mens-btn-secondary"
                            onClick={() => setPagamentoDetalhes(p)}
                          >
                            Detalhes
                          </button>
                        </div>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <PagamentoGatewayModal
        mensalidade={mensalidadeParaPagar}
        formaInicial={formaInicialModal}
        onClose={() => setMensalidadeParaPagar(null)}
        onPago={() => void carregarDados()}
      />

      <MensalidadeDetalhesModal
        mensalidade={mensalidadeDetalhes}
        onClose={() => setMensalidadeDetalhes(null)}
      />

      <PagamentoDetalhesModal
        pagamento={pagamentoDetalhes}
        onClose={() => setPagamentoDetalhes(null)}
      />
    </div>
  )
}
