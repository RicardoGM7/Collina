import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import toast from 'react-hot-toast'
import {
  ArrowUpDown,
  ChevronLeft,
  ChevronRight,
  Clock,
  FileText,
  Minus,
  Pencil,
  Plus,
  Search,
  TrendingDown,
  TrendingUp,
  Wallet,
  X,
} from 'lucide-react'
import { useAuth } from '../../context/AuthContext'
import { getApiErrorMessage } from '../../services/api'
import { financeiroService } from '../../services/financeiroService'
import type {
  MovimentacaoResponse,
  SaldoResponse,
  SituacaoMovimentacao,
  SolicitacaoResponse,
  TipoMovimentacao,
} from '../../types/financeiro'
import { HOME_POR_PERFIL } from '../../types/user'
import { formatCurrency, formatDateShort, getPeriodDateRange } from '../../utils/formatters'
import {
  CancelarMovimentacaoModal,
  EfetivarSolicitacaoModal,
  MovimentacaoModal,
  RelatorioFechamentoModal,
  SolicitacaoGastoModal,
} from './FinanceiroModals'
import { MensalidadesResumoSection } from './components/MensalidadesResumoSection'
import {
  lastMonths,
  monthBounds,
  monthKey,
  ORIGEM_LABEL,
  PRIORIDADE_LABEL,
  PRIORIDADE_ORDEM,
  TIPO_LABEL,
} from './financeiroUtils'
import './FinanceiroPage.css'

const LINHAS_POR_PAGINA = 8
const ALTURA_MAXIMA_BARRA = 140

function somar(lista: MovimentacaoResponse[], tipo: TipoMovimentacao): number {
  return lista.filter((m) => m.tipo === tipo).reduce((total, m) => total + m.valor, 0)
}

export function FinanceiroPage() {
  const { usuario } = useAuth()
  const home = usuario ? HOME_POR_PERFIL[usuario.perfil] : '/'

  // Dados do painel
  const [saldo, setSaldo] = useState<SaldoResponse | null>(null)
  const [pendentes, setPendentes] = useState<SolicitacaoResponse[]>([])
  const [resumo, setResumo] = useState<MovimentacaoResponse[]>([]) // movimentações ativas dos últimos 6 meses
  const [carregando, setCarregando] = useState(true)
  const [erroCarga, setErroCarga] = useState(false)

  // Tabela de movimentações (com filtros)
  const [tabela, setTabela] = useState<MovimentacaoResponse[]>([])
  const [tipoFiltro, setTipoFiltro] = useState<'' | TipoMovimentacao>('')
  const [situacaoFiltro, setSituacaoFiltro] = useState<'' | SituacaoMovimentacao>('')
  const [periodo, setPeriodo] = useState('30d')
  const [pagina, setPagina] = useState(1)

  // Solicitações de gasto
  const [busca, setBusca] = useState('')
  const [ordenarPorValor, setOrdenarPorValor] = useState(false)

  // Modais
  const [tipoNovo, setTipoNovo] = useState<TipoMovimentacao | null>(null)
  const [editando, setEditando] = useState<MovimentacaoResponse | null>(null)
  const [cancelando, setCancelando] = useState<MovimentacaoResponse | null>(null)
  const [novaSolicitacao, setNovaSolicitacao] = useState(false)
  const [efetivando, setEfetivando] = useState<SolicitacaoResponse | null>(null)
  const [relatorioAberto, setRelatorioAberto] = useState(false)

  const meses = useMemo(() => lastMonths(6), [])

  const carregarPainel = useCallback(async () => {
    const hoje = new Date()
    const inicio = monthBounds(new Date(hoje.getFullYear(), hoje.getMonth() - 5, 1)).inicio
    const fim = monthBounds(hoje).fim
    try {
      const [saldoAtual, solicitacoes, historico] = await Promise.all([
        financeiroService.consultarSaldo(),
        financeiroService.listarSolicitacoesPorPrioridade(),
        financeiroService.listarHistorico({ situacao: 'ATIVO', dataInicio: inicio, dataFim: fim }),
      ])
      setSaldo(saldoAtual)
      setPendentes(solicitacoes)
      setResumo(historico)
      setErroCarga(false)
    } catch (err) {
      setErroCarga(true)
      toast.error(getApiErrorMessage(err, 'Não foi possível carregar o painel financeiro.'))
    } finally {
      setCarregando(false)
    }
  }, [])

  const carregarTabela = useCallback(async () => {
    try {
      const dados = await financeiroService.listarHistorico({
        tipo: tipoFiltro || undefined,
        situacao: situacaoFiltro || undefined,
        ...getPeriodDateRange(periodo),
      })
      setTabela(dados)
    } catch (err) {
      toast.error(getApiErrorMessage(err, 'Não foi possível carregar as movimentações.'))
    }
  }, [tipoFiltro, situacaoFiltro, periodo])

  useEffect(() => {
    void carregarPainel()
  }, [carregarPainel])

  useEffect(() => {
    void carregarTabela()
  }, [carregarTabela])

  // Depois de qualquer alteração, recarrega o painel e a tabela
  const recarregar = useCallback(() => {
    void carregarPainel()
    void carregarTabela()
  }, [carregarPainel, carregarTabela])

  // KPIs e gráficos do mês corrente
  const mesAtual = monthKey(new Date())
  const doMes = useMemo(() => resumo.filter((m) => m.dataHora.slice(0, 7) === mesAtual), [resumo, mesAtual])
  const entradasMes = somar(doMes, 'ENTRADA')
  const saidasMes = somar(doMes, 'SAIDA')
  const totalFluxo = entradasMes + saidasMes
  const percentualEntradas = totalFluxo > 0 ? (entradasMes / totalFluxo) * 100 : 0
  const totalPendentes = pendentes.reduce((total, s) => total + s.valor, 0)

  const serie = useMemo(
    () =>
      meses.map((mes) => {
        const doPeriodo = resumo.filter((m) => m.dataHora.slice(0, 7) === mes.key)
        return { ...mes, entradas: somar(doPeriodo, 'ENTRADA'), saidas: somar(doPeriodo, 'SAIDA') }
      }),
    [meses, resumo],
  )
  const maximoSerie = Math.max(1, ...serie.flatMap((s) => [s.entradas, s.saidas]))
  const alturaBarra = (valor: number) =>
    valor > 0 ? Math.max(4, Math.round((valor / maximoSerie) * ALTURA_MAXIMA_BARRA)) : 0

  const ultimas = resumo.slice(0, 5)

  // Solicitações: busca e ordenação
  const solicitacoesExibidas = useMemo(() => {
    const termo = busca.trim().toLowerCase()
    const filtradas = pendentes.filter((s) => s.descricao.toLowerCase().includes(termo))
    return [...filtradas].sort((a, b) =>
      ordenarPorValor ? b.valor - a.valor : PRIORIDADE_ORDEM[a.prioridade] - PRIORIDADE_ORDEM[b.prioridade],
    )
  }, [pendentes, busca, ordenarPorValor])

  // Paginação da tabela
  const totalPaginas = Math.max(1, Math.ceil(tabela.length / LINHAS_POR_PAGINA))
  const paginaAtual = Math.min(pagina, totalPaginas)
  const linhas = tabela.slice((paginaAtual - 1) * LINHAS_POR_PAGINA, paginaAtual * LINHAS_POR_PAGINA)

  const saldoValor = saldo?.valor ?? 0

  return (
    <div className="fin-page">
      <div className="fin-breadcrumb-bar">
        <Link to={home} className="fin-btn-back">
          <ChevronLeft size={14} color="#2BB673" strokeWidth={2.8} />
          <span>Voltar ao Início</span>
        </Link>
        <div className="fin-breadcrumb">
          <span>Visão geral</span>
          <ChevronRight size={14} color="#94A3B8" />
          <span className="fin-breadcrumb-ativo">Financeiro</span>
        </div>
      </div>

      {erroCarga && !carregando && (
        <div className="fin-aviso" role="alert">
          Não foi possível carregar os dados financeiros.
          <button type="button" className="fin-link" onClick={recarregar}>Tentar de novo</button>
        </div>
      )}

      {/* KPIs */}
      <div className="fin-kpi-grid">
        <div className="fin-kpi fin-kpi-branco">
          <div className="fin-kpi-info">
            <span className="fin-kpi-label">Saldo do clube</span>
            <span className="fin-kpi-valor fin-verde">{carregando ? '—' : formatCurrency(saldoValor)}</span>
            {saldo && <span className="fin-kpi-nota">Atualizado {formatDateShort(saldo.dataAtualizacao)}</span>}
          </div>
          <div className="fin-kpi-icone fin-icone-verde"><Wallet size={20} /></div>
        </div>
        <div className="fin-kpi fin-kpi-verde">
          <div className="fin-kpi-info">
            <span className="fin-kpi-label">Entradas do mês</span>
            <span className="fin-kpi-valor">{formatCurrency(entradasMes)}</span>
            <span className="fin-kpi-nota">{doMes.filter((m) => m.tipo === 'ENTRADA').length} lançamentos</span>
          </div>
          <div className="fin-kpi-icone fin-icone-verde"><TrendingUp size={20} /></div>
        </div>
        <div className="fin-kpi fin-kpi-azul">
          <div className="fin-kpi-info">
            <span className="fin-kpi-label">Saídas do mês</span>
            <span className="fin-kpi-valor">{formatCurrency(saidasMes)}</span>
            <span className="fin-kpi-nota">{doMes.filter((m) => m.tipo === 'SAIDA').length} lançamentos</span>
          </div>
          <div className="fin-kpi-icone fin-icone-azul"><TrendingDown size={20} /></div>
        </div>
        <div className="fin-kpi fin-kpi-ambar">
          <div className="fin-kpi-info">
            <span className="fin-kpi-label">Solicitações pendentes</span>
            <span className="fin-kpi-valor">{pendentes.length}</span>
            <span className="fin-kpi-nota">{formatCurrency(totalPendentes)} no total</span>
          </div>
          <div className="fin-kpi-icone fin-icone-ambar"><Clock size={20} /></div>
        </div>
      </div>

      {/* Pizza e últimas movimentações */}
      <div className="fin-duo-grid">
        <section className="fin-card">
          <h2 className="fin-titulo">Entradas × saídas do mês</h2>
          <div className="fin-pizza-area">
            <div
              className="fin-pizza"
              role="img"
              aria-label={`Entradas ${formatCurrency(entradasMes)} e saídas ${formatCurrency(saidasMes)} no mês`}
              style={{
                background:
                  totalFluxo > 0
                    ? `conic-gradient(#2BB673 0 ${percentualEntradas}%, #0B1F33 0)`
                    : '#E2E8F0',
              }}
            />
            <div className="fin-pizza-legenda">
              <div>
                <span className="fin-chip fin-chip-entrada">Entradas</span>
                <strong>{formatCurrency(entradasMes)}</strong>
              </div>
              <div>
                <span className="fin-chip fin-chip-saida">Saídas</span>
                <strong>{formatCurrency(saidasMes)}</strong>
              </div>
              {totalFluxo === 0 && <span className="fin-kpi-nota">Sem movimentações neste mês.</span>}
            </div>
          </div>
        </section>

        <section className="fin-card">
          <h2 className="fin-titulo">Últimas movimentações</h2>
          {ultimas.length === 0 ? (
            <p className="fin-vazio">Nenhuma movimentação recente.</p>
          ) : (
            <ul className="fin-ultimas">
              {ultimas.map((m) => (
                <li key={m.id}>
                  <span className={`fin-data ${m.tipo === 'ENTRADA' ? 'fin-data-entrada' : 'fin-data-saida'}`}>
                    {formatDateShort(m.dataHora).slice(0, 5)}
                  </span>
                  <span className="fin-pilula">{formatCurrency(m.valor)}</span>
                  <span className="fin-ultimas-desc">{m.descricao || ORIGEM_LABEL[m.origem]}</span>
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>

      {/* Barras por mês */}
      <section className="fin-card">
        <div className="fin-card-topo">
          <h2 className="fin-titulo">Receitas e despesas por mês</h2>
          <span className="fin-periodo">{serie[0].label} - {serie[serie.length - 1].label}</span>
          <div className="fin-legenda">
            <span className="fin-chip fin-chip-entrada">Receita</span>
            <span className="fin-chip fin-chip-saida">Despesa</span>
          </div>
        </div>
        <div className="fin-barras">
          {serie.map((s) => (
            <div key={s.key} className="fin-barras-mes" title={`${s.label}: receitas ${formatCurrency(s.entradas)} · despesas ${formatCurrency(s.saidas)}`}>
              <div className="fin-barras-par">
                <span className="fin-barra fin-barra-entrada" style={{ height: alturaBarra(s.entradas) }} />
                <span className="fin-barra fin-barra-saida" style={{ height: alturaBarra(s.saidas) }} />
              </div>
              <span className="fin-barras-rotulo">{s.label}</span>
            </div>
          ))}
        </div>
      </section>

      {/* Movimentações */}
      <section className="fin-card">
        <div className="fin-card-topo">
          <h2 className="fin-titulo">Movimentações</h2>
          <div className="fin-filtros">
            <select className="fin-select" aria-label="Filtrar por tipo" value={tipoFiltro}
              onChange={(e) => { setTipoFiltro(e.target.value as '' | TipoMovimentacao); setPagina(1) }}>
              <option value="">Todos os tipos</option>
              <option value="ENTRADA">Entradas</option>
              <option value="SAIDA">Saídas</option>
            </select>
            <select className="fin-select" aria-label="Filtrar por situação" value={situacaoFiltro}
              onChange={(e) => { setSituacaoFiltro(e.target.value as '' | SituacaoMovimentacao); setPagina(1) }}>
              <option value="">Todas as situações</option>
              <option value="ATIVO">Ativas</option>
              <option value="CANCELADO">Canceladas</option>
            </select>
            <select className="fin-select" aria-label="Filtrar por período" value={periodo}
              onChange={(e) => { setPeriodo(e.target.value); setPagina(1) }}>
              <option value="7d">Últimos 7 dias</option>
              <option value="30d">Últimos 30 dias</option>
              <option value="Todos">Todo o período</option>
            </select>
          </div>
        </div>

        <div className="fin-acoes">
          <button type="button" className="fin-btn fin-btn-receita" onClick={() => setTipoNovo('ENTRADA')}>
            <Plus size={14} /> Cadastrar receita
          </button>
          <button type="button" className="fin-btn fin-btn-despesa" onClick={() => setTipoNovo('SAIDA')}>
            <Minus size={14} /> Cadastrar despesa
          </button>
          <button type="button" className="fin-btn fin-btn-neutro" onClick={() => setRelatorioAberto(true)}>
            <FileText size={14} /> Relatório de fechamento
          </button>
        </div>

        <div className="fin-tabela-wrap">
          <table className="fin-tabela">
            <thead>
              <tr>
                <th>Data</th><th>Descrição</th><th>Origem</th><th>Tipo</th><th>Valor</th><th>Situação</th><th className="fin-th-acoes">Ações</th>
              </tr>
            </thead>
            <tbody>
              {linhas.length === 0 && (
                <tr><td colSpan={7} className="fin-vazio">Nenhuma movimentação encontrada.</td></tr>
              )}
              {linhas.map((m) => {
                const cancelada = m.situacao === 'CANCELADO'
                const gerenciavel = !cancelada && m.origem !== 'PAGAMENTO'
                return (
                  <tr key={m.id} className={cancelada ? 'fin-linha-cancelada' : ''}>
                    <td>{formatDateShort(m.dataHora)}</td>
                    <td>
                      {m.descricao || '—'}
                      {cancelada && m.motivoCancelamento && (
                        <span className="fin-motivo">Motivo: {m.motivoCancelamento}</span>
                      )}
                    </td>
                    <td>{ORIGEM_LABEL[m.origem]}</td>
                    <td>
                      <span className={`fin-chip ${m.tipo === 'ENTRADA' ? 'fin-chip-entrada' : 'fin-chip-saida'}`}>
                        {TIPO_LABEL[m.tipo]}
                      </span>
                    </td>
                    <td className={m.tipo === 'ENTRADA' ? 'fin-valor-entrada' : 'fin-valor-saida'}>
                      {m.tipo === 'ENTRADA' ? '+' : '−'} {formatCurrency(m.valor)}
                    </td>
                    <td>{cancelada ? 'Cancelada' : 'Ativa'}</td>
                    <td className="fin-td-acoes">
                      {gerenciavel ? (
                        <>
                          <button type="button" className="fin-icone-btn" aria-label={`Editar movimentação ${m.id}`} onClick={() => setEditando(m)}>
                            <Pencil size={14} />
                          </button>
                          <button type="button" className="fin-icone-btn fin-icone-perigo" aria-label={`Cancelar movimentação ${m.id}`} onClick={() => setCancelando(m)}>
                            <X size={14} />
                          </button>
                        </>
                      ) : (
                        <span className="fin-kpi-nota">{cancelada ? '—' : 'Gerada por pagamento'}</span>
                      )}
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>

        <div className="fin-paginacao">
          <span className="fin-kpi-nota">{tabela.length} movimentações</span>
          <div className="fin-paginacao-botoes">
            <button type="button" className="fin-pagina-btn" aria-label="Página anterior" disabled={paginaAtual <= 1} onClick={() => setPagina(paginaAtual - 1)}>
              <ChevronLeft size={14} />
            </button>
            <span className="fin-pagina-atual">{paginaAtual} / {totalPaginas}</span>
            <button type="button" className="fin-pagina-btn" aria-label="Próxima página" disabled={paginaAtual >= totalPaginas} onClick={() => setPagina(paginaAtual + 1)}>
              <ChevronRight size={14} />
            </button>
          </div>
        </div>
      </section>

      {/* Solicitações de gasto */}
      <section className="fin-card">
        <div className="fin-card-topo">
          <h2 className="fin-titulo">Solicitações de gasto pendentes</h2>
          <div className="fin-filtros">
            <div className="fin-busca">
              <Search size={14} className="fin-busca-icone" />
              <input className="fin-busca-input" aria-label="Buscar solicitação" placeholder="Buscar solicitação" value={busca} onChange={(e) => setBusca(e.target.value)} />
            </div>
            <button type="button" className="fin-btn fin-btn-neutro" onClick={() => setOrdenarPorValor((v) => !v)} aria-label="Alternar ordenação">
              <ArrowUpDown size={14} /> {ordenarPorValor ? 'Por valor' : 'Por prioridade'}
            </button>
            <button type="button" className="fin-btn fin-btn-receita" onClick={() => setNovaSolicitacao(true)}>
              <Plus size={14} /> Nova solicitação
            </button>
          </div>
        </div>

        {solicitacoesExibidas.length === 0 ? (
          <p className="fin-vazio">Nenhuma solicitação pendente.</p>
        ) : (
          <ul className="fin-solicitacoes">
            {solicitacoesExibidas.map((s) => {
              const semSaldo = s.valor > saldoValor
              return (
                <li key={s.id}>
                  <span className="fin-pilula fin-sol-desc">{s.descricao}</span>
                  <span className="fin-pilula">{formatCurrency(s.valor)}</span>
                  <span className={`fin-prioridade fin-prio-${s.prioridade.toLowerCase()}`}>{PRIORIDADE_LABEL[s.prioridade]}</span>
                  <span className="fin-pilula">{formatDateShort(s.dataSolicitacao).slice(0, 5)}</span>
                  {semSaldo && <span className="fin-chip fin-chip-alerta">Saldo insuficiente</span>}
                  <button type="button" className="fin-btn fin-btn-receita" disabled={semSaldo} onClick={() => setEfetivando(s)}>
                    Efetivar
                  </button>
                </li>
              )
            })}
          </ul>
        )}
      </section>

      <MensalidadesResumoSection />

      <MovimentacaoModal tipoNovo={tipoNovo} editando={editando} onClose={() => { setTipoNovo(null); setEditando(null) }} onSaved={recarregar} />
      <CancelarMovimentacaoModal movimentacao={cancelando} onClose={() => setCancelando(null)} onSaved={recarregar} />
      <SolicitacaoGastoModal isOpen={novaSolicitacao} onClose={() => setNovaSolicitacao(false)} onSaved={recarregar} />
      <EfetivarSolicitacaoModal solicitacao={efetivando} saldoAtual={saldoValor} onClose={() => setEfetivando(null)} onSaved={recarregar} />
      <RelatorioFechamentoModal isOpen={relatorioAberto} onClose={() => setRelatorioAberto(false)} />
    </div>
  )
}
