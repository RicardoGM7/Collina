import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import toast from 'react-hot-toast'
import { ChevronLeft, Search, User } from 'lucide-react'
import { useAuth } from '../../context/AuthContext'
import { getApiErrorMessage } from '../../services/api'
import { associadoService } from '../../services/associadoService'
import { financeiroService } from '../../services/financeiroService'
import type { Associado, VinculoDependencia } from '../../types/associado'
import type { Mensalidade, Pagamento, StatusMensalidade } from '../../types/financeiro'
import { HOME_POR_PERFIL } from '../../types/user'
import { formatCurrency } from '../../utils/formatters'
import {
  formatarCpf,
  formatarDataBr,
  statusHttp,
  validarCpf,
} from '../associados/associadoUtils'
import { CancelarMensalidadeModal } from './components/CancelarMensalidadeModal'
import { EstadoVazio } from './components/EstadoVazio'
import { FiltroStatusTabs } from './components/FiltroStatusTabs'
import {
  InserirGastoAvulsoModal,
  type PessoaGastoOption,
} from './components/InserirGastoAvulsoModal'
import { MensalidadeDetalhesModal } from './components/MensalidadeDetalhesModal'
import { RegistrarPagamentoModal } from './components/RegistrarPagamentoModal'
import { StatusMensalidadeBadge } from './components/StatusMensalidadeBadge'
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
import './MensalidadesAssociadoPage.css'

type FiltroMensalidade = 'TODOS' | Exclude<StatusMensalidade, 'EM_ABERTO'>
type FiltroPeriodo = 'todos' | '3' | '6' | '12'

export function MensalidadesAssociadoPage() {
  const { usuario } = useAuth()
  const [searchParams] = useSearchParams()

  const perfil = usuario?.perfil
  const home = usuario ? HOME_POR_PERFIL[usuario.perfil] : '/'
  const podeConfirmarManual = perfil === 'ADMIN' || perfil === 'RECEPCAO'
  const podeVerPagamentos = perfil === 'ADMIN' || perfil === 'FINANCEIRO'

  // Busca por CPF e titular selecionado
  const [cpfInput, setCpfInput] = useState('')
  const [buscandoCpf, setBuscandoCpf] = useState(false)
  const [associadoId, setAssociadoId] = useState<number | null>(null)
  const [nomeTitular, setNomeTitular] = useState('')
  const [associado, setAssociado] = useState<Associado | null>(null)
  const [dependentes, setDependentes] = useState<VinculoDependencia[]>([])

  // Dados financeiros do titular
  const [mensalidades, setMensalidades] = useState<Mensalidade[]>([])
  const [carregando, setCarregando] = useState(false)
  const [erro, setErro] = useState('')

  // Filtros
  const [statusFiltro, setStatusFiltro] = useState<FiltroMensalidade>('TODOS')
  const [periodoFiltro, setPeriodoFiltro] = useState<FiltroPeriodo>('todos')
  const [buscaTexto, setBuscaTexto] = useState('')

  // Modais
  const [mensalidadePagamento, setMensalidadePagamento] = useState<Mensalidade | null>(null)
  const [mensalidadeCancelar, setMensalidadeCancelar] = useState<Mensalidade | null>(null)
  const [modalGastoAberto, setModalGastoAberto] = useState(false)
  const [mensalidadeDetalhes, setMensalidadeDetalhes] = useState<Mensalidade | null>(null)
  const [pagamentosDetalhes, setPagamentosDetalhes] = useState<Pagamento[] | undefined>(undefined)
  const [carregandoPagDetalhes, setCarregandoPagDetalhes] = useState(false)

  const carregarMensalidades = useCallback(async (idTitular: number) => {
    setCarregando(true)
    setErro('')
    try {
      const lista = await financeiroService.listarMensalidades(idTitular)
      // Ordena por competência decrescente e, dentro da mesma competência,
      // coloca a CANCELADA antes da substituta (menor id primeiro) para exibir logo abaixo
      const ordenada = [...lista].sort((a, b) => {
        const compDiff = b.competencia.localeCompare(a.competencia)
        if (compDiff !== 0) return compDiff
        if (a.status === 'CANCELADA' && b.status !== 'CANCELADA') return -1
        if (b.status === 'CANCELADA' && a.status !== 'CANCELADA') return 1
        return a.id - b.id
      })
      setMensalidades(ordenada)
    } catch (err) {
      setErro(getApiErrorMessage(err, 'Não foi possível carregar as mensalidades do associado.'))
    } finally {
      setCarregando(false)
    }
  }, [])

  const carregarDependentes = useCallback(async (idTitular: number) => {
    try {
      const deps = await associadoService.listarDependentes(idTitular)
      setDependentes(deps)
    } catch {
      setDependentes([])
    }
  }, [])

  const buscarPorCpf = useCallback(
    async (cpf: string) => {
      if (!validarCpf(cpf)) {
        toast.error('Informe um CPF válido com 11 dígitos.')
        return
      }
      setBuscandoCpf(true)
      setErro('')
      try {
        const encontrado = await associadoService.buscarPorCpf(cpf)
        setAssociado(encontrado)
        setAssociadoId(encontrado.id)
        setNomeTitular(encontrado.pessoa.nome)
        await Promise.all([
          carregarMensalidades(encontrado.id),
          carregarDependentes(encontrado.id),
        ])
      } catch (err) {
        if (statusHttp(err) === 404) {
          setErro('Nenhum titular encontrado com este CPF.')
        } else {
          setErro(getApiErrorMessage(err, 'Não foi possível buscar o associado pelo CPF.'))
        }
        setAssociado(null)
        setAssociadoId(null)
        setNomeTitular('')
        setMensalidades([])
        setDependentes([])
      } finally {
        setBuscandoCpf(false)
      }
    },
    [carregarDependentes, carregarMensalidades],
  )

  // Leitura inicial dos parâmetros ?cpf= ou ?associadoId=&nome=
  useEffect(() => {
    const cpfParam = searchParams.get('cpf')
    const idParam = searchParams.get('associadoId')
    const nomeParam = searchParams.get('nome')

    if (cpfParam) {
      const formatado = formatarCpf(cpfParam)
      setCpfInput(formatado)
      void buscarPorCpf(formatado)
      return
    }

    if (idParam) {
      const parsedId = Number(idParam)
      if (Number.isFinite(parsedId) && parsedId > 0) {
        setAssociado(null)
        setAssociadoId(parsedId)
        setNomeTitular(nomeParam || `Associado #${parsedId}`)
        void carregarMensalidades(parsedId)
        void carregarDependentes(parsedId)
      }
    }
  }, [searchParams, buscarPorCpf, carregarMensalidades, carregarDependentes])

  function handleSubmitCpf(e: FormEvent) {
    e.preventDefault()
    void buscarPorCpf(cpfInput)
  }

  function abrirDetalhes(m: Mensalidade) {
    setMensalidadeDetalhes(m)
    if (!podeVerPagamentos) {
      setPagamentosDetalhes(undefined)
      return
    }
    setCarregandoPagDetalhes(true)
    setPagamentosDetalhes([])
    financeiroService
      .pagamentosDaMensalidade(m.id)
      .then((lista) => setPagamentosDetalhes(lista))
      .catch(() => setPagamentosDetalhes([]))
      .finally(() => setCarregandoPagDetalhes(false))
  }

  const situacao = useMemo(() => situacaoFinanceira(mensalidades), [mensalidades])
  const fechada = useMemo(() => ultimaFechada(mensalidades), [mensalidades])
  const aberta = useMemo(() => emAberto(mensalidades), [mensalidades])

  const pessoasParaGasto: PessoaGastoOption[] = useMemo(() => {
    const opcoes: PessoaGastoOption[] = []
    if (associado) {
      opcoes.push({
        pessoaId: associado.pessoa.id,
        rotulo: `Titular: ${associado.pessoa.nome}`,
      })
    }
    for (const dep of dependentes) {
      if (dep.status === 'ATIVO') {
        opcoes.push({
          pessoaId: dep.dependente.id,
          rotulo: `${dep.dependente.nome} (${dep.parentesco})`,
        })
      }
    }
    return opcoes
  }, [associado, dependentes])

  const mensalidadesFiltradas = useMemo(() => {
    const termo = buscaTexto.trim().toLowerCase()
    const meses = periodoFiltro === 'todos' ? 'todos' : Number(periodoFiltro)

    return mensalidades.filter((m) => {
      if (statusFiltro !== 'TODOS' && m.status !== statusFiltro) return false
      if (!competenciaDentroDosMeses(m.competencia, meses)) return false
      if (termo) {
        const comp = formatarCompetencia(m.competencia).toLowerCase()
        if (!comp.includes(termo) && !String(m.id).includes(termo)) return false
      }
      return true
    })
  }, [mensalidades, statusFiltro, periodoFiltro, buscaTexto])

  const recarregarAtual = () => {
    if (associadoId) {
      void carregarMensalidades(associadoId)
    }
  }

  return (
    <div className="mens-page">
      {/* Barra superior e breadcrumb */}
      <header className="mens-topbar">
        <div className="mens-topbar-left">
          <Link to={home} className="mens-btn-back">
            <ChevronLeft size={14} />
            <span>Voltar ao Início</span>
          </Link>

          <nav className="mens-breadcrumb" aria-label="Navegação">
            {perfil === 'RECEPCAO' ? (
              <Link to="/recepcao">Recepção</Link>
            ) : (
              <Link to="/financeiro">Financeiro</Link>
            )}
            <span>›</span>
            <span>Mensalidades</span>
            <span>›</span>
            <span className="mens-breadcrumb-active">Histórico</span>
          </nav>
        </div>
      </header>

      {/* Busca por CPF */}
      <section className="mens-cpf-bar">
        <form className="mens-cpf-form" onSubmit={handleSubmitCpf}>
          <div className="mens-cpf-input-wrap">
            <Search size={14} className="mens-search-icon" />
            <input
              type="text"
              className="mens-cpf-input"
              placeholder="Insira o CPF do Associado"
              value={cpfInput}
              onChange={(e) => setCpfInput(formatarCpf(e.target.value))}
              disabled={buscandoCpf}
            />
          </div>
          <button type="submit" className="mens-btn-navy" disabled={buscandoCpf}>
            {buscandoCpf ? 'Buscando...' : 'Buscar'}
          </button>
        </form>

        {associadoId && (
          <div className="mens-titular-chip">
            <User size={15} />
            <span>{nomeTitular}</span>
          </div>
        )}
      </section>

      {erro && (
        <div className="mens-alert mens-alert-erro" role="alert">
          {erro}
        </div>
      )}

      {!associadoId ? (
        <EstadoVazio
          titulo="Nenhum associado selecionado"
          descricao="Busque um titular pelo CPF acima para visualizar as mensalidades e realizar lançamentos."
        />
      ) : carregando ? (
        <div className="mens-loading">Carregando histórico de mensalidades...</div>
      ) : (
        <>
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
                  {(fechada.status === 'PENDENTE' || fechada.status === 'ATRASADA') &&
                    podeConfirmarManual && (
                      <button
                        type="button"
                        className="mens-btn-primary"
                        onClick={() => setMensalidadePagamento(fechada)}
                      >
                        MARCAR COMO PAGA
                      </button>
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
                    <div className="mens-summary-row">
                      <span>Total até agora</span>
                      <strong>{formatCurrency(aberta.valor)}</strong>
                    </div>
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
                  <button
                    type="button"
                    className="mens-btn-navy"
                    onClick={() => setModalGastoAberto(true)}
                    disabled={pessoasParaGasto.length === 0}
                    title={
                      pessoasParaGasto.length === 0
                        ? 'Busque o titular pelo CPF para carregar a lista de pessoas.'
                        : undefined
                    }
                  >
                    INSERIR GASTO AVULSO
                  </button>
                </>
              ) : (
                <p className="mens-summary-muted">Nenhuma mensalidade em aberto no momento.</p>
              )}
            </article>
          </section>

          {/* Tabela de histórico */}
          <section className="mens-section-card">
            <div className="mens-section-header">
              <h3 className="mens-section-title">
                Histórico de mensalidades de {nomeTitular}
              </h3>
              <div className="mens-toolbar">
                <select
                  className="mens-select"
                  aria-label="Filtrar período"
                  value={periodoFiltro}
                  onChange={(e) => setPeriodoFiltro(e.target.value as FiltroPeriodo)}
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
                    value={buscaTexto}
                    onChange={(e) => setBuscaTexto(e.target.value)}
                  />
                </div>
              </div>
            </div>

            <FiltroStatusTabs<FiltroMensalidade>
              ativo={statusFiltro}
              onChange={setStatusFiltro}
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
                descricao="Não há mensalidades para os filtros aplicados."
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
                    {mensalidadesFiltradas.map((m) => {
                      const podePagarOuCancelar =
                        m.status === 'PENDENTE' || m.status === 'ATRASADA'
                      return (
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
                              {podePagarOuCancelar && podeConfirmarManual && (
                                <button
                                  type="button"
                                  className="mens-btn-pay-sm"
                                  onClick={() => setMensalidadePagamento(m)}
                                >
                                  Registrar pagamento
                                </button>
                              )}
                              {podePagarOuCancelar && (
                                <button
                                  type="button"
                                  className="mens-btn-cancel-sm"
                                  onClick={() => setMensalidadeCancelar(m)}
                                >
                                  CANCELAR
                                </button>
                              )}
                              <button
                                type="button"
                                className="mens-btn-secondary"
                                onClick={() => abrirDetalhes(m)}
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
        </>
      )}

      <RegistrarPagamentoModal
        mensalidade={mensalidadePagamento}
        onClose={() => setMensalidadePagamento(null)}
        onSuccess={recarregarAtual}
      />

      <CancelarMensalidadeModal
        mensalidade={mensalidadeCancelar}
        onClose={() => setMensalidadeCancelar(null)}
        onSuccess={recarregarAtual}
      />

      <InserirGastoAvulsoModal
        isOpen={modalGastoAberto}
        pessoas={pessoasParaGasto}
        onClose={() => setModalGastoAberto(false)}
        onSuccess={recarregarAtual}
      />

      <MensalidadeDetalhesModal
        mensalidade={mensalidadeDetalhes}
        pagamentos={pagamentosDetalhes}
        carregandoPagamentos={carregandoPagDetalhes}
        onClose={() => setMensalidadeDetalhes(null)}
      />
    </div>
  )
}
