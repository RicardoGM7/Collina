import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ArrowUpDown, ChevronLeft, Search } from 'lucide-react'
import { useAuth } from '../../context/AuthContext'
import { getApiErrorMessage } from '../../services/api'
import { financeiroService } from '../../services/financeiroService'
import type { Inadimplente } from '../../types/financeiro'
import { HOME_POR_PERFIL } from '../../types/user'
import { formatCurrency } from '../../utils/formatters'
import { formatarDataBr } from '../associados/associadoUtils'
import { EstadoVazio } from './components/EstadoVazio'
import { agruparTotais, formatarCompetencia } from './mensalidadesUtils'
import './components/mensalidadesShared.css'
import './MensalidadesAssociadoPage.css'
import './MinhasMensalidadesPanel.css'
import './InadimplentesPage.css'

type ColunaOrdenacao =
  | 'associadoId'
  | 'nomeAssociado'
  | 'valor'
  | 'competencia'
  | 'dataVencimento'

export function InadimplentesPage() {
  const { usuario } = useAuth()
  const navigate = useNavigate()
  const home = usuario ? HOME_POR_PERFIL[usuario.perfil] : '/'

  const [itens, setItens] = useState<Inadimplente[]>([])
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState('')

  const [busca, setBusca] = useState('')
  const [competenciaFiltro, setCompetenciaFiltro] = useState('')
  const [ordenarPor, setOrdenarPor] = useState<ColunaOrdenacao>('dataVencimento')
  const [ordemAsc, setOrdemAsc] = useState(true)

  const carregar = useCallback(async () => {
    setCarregando(true)
    setErro('')
    try {
      const lista = await financeiroService.inadimplentes()
      setItens(lista)
    } catch (err) {
      setErro(getApiErrorMessage(err, 'Não foi possível carregar o relatório de inadimplentes.'))
    } finally {
      setCarregando(false)
    }
  }, [])

  useEffect(() => {
    void carregar()
  }, [carregar])

  const competenciasDisponiveis = useMemo(() => {
    const unicas = Array.from(new Set(itens.map((i) => i.competencia))).sort((a, b) =>
      b.localeCompare(a),
    )
    return unicas
  }, [itens])

  const filtradosEOrdenados = useMemo(() => {
    const termo = busca.trim().toLowerCase()
    const filtrados = itens.filter((item) => {
      if (competenciaFiltro && item.competencia !== competenciaFiltro) return false
      if (termo) {
        const matchNome = item.nomeAssociado.toLowerCase().includes(termo)
        const matchId = String(item.associadoId).includes(termo)
        if (!matchNome && !matchId) return false
      }
      return true
    })

    const fator = ordemAsc ? 1 : -1
    return [...filtrados].sort((a, b) => {
      switch (ordenarPor) {
        case 'associadoId':
          return (a.associadoId - b.associadoId) * fator
        case 'nomeAssociado':
          return a.nomeAssociado.localeCompare(b.nomeAssociado, 'pt-BR') * fator
        case 'valor':
          return (a.valor - b.valor) * fator
        case 'competencia':
          return a.competencia.localeCompare(b.competencia) * fator
        case 'dataVencimento':
          return a.dataVencimento.localeCompare(b.dataVencimento) * fator
      }
    })
  }, [itens, busca, competenciaFiltro, ordenarPor, ordemAsc])

  const totalInadimplencia = useMemo(
    () => agruparTotais(filtradosEOrdenados.map((i) => i.valor)),
    [filtradosEOrdenados],
  )

  function alternarOrdenacao(coluna: ColunaOrdenacao) {
    if (ordenarPor === coluna) {
      setOrdemAsc((prev) => !prev)
    } else {
      setOrdenarPor(coluna)
      setOrdemAsc(true)
    }
  }

  return (
    <div className="mens-page">
      <header className="mens-topbar">
        <div className="mens-topbar-left">
          <Link to={home} className="mens-btn-back">
            <ChevronLeft size={14} />
            <span>Voltar ao Início</span>
          </Link>

          <nav className="mens-breadcrumb" aria-label="Navegação">
            <Link to="/financeiro">Financeiro</Link>
            <span>›</span>
            <span>Mensalidades</span>
            <span>›</span>
            <span className="mens-breadcrumb-active">Inadimplentes</span>
          </nav>
        </div>
      </header>

      <section className="mens-report-card">
        <div className="mens-report-header">
          <h2 className="mens-report-title">Relatório de inadimplentes</h2>
          <div className="mens-toolbar">
            <div className="mens-search-wrap">
              <Search size={14} className="mens-search-icon" />
              <input
                type="text"
                className="mens-search-input"
                placeholder="Buscar por nome ou ID"
                value={busca}
                onChange={(e) => setBusca(e.target.value)}
              />
            </div>
            <select
              className="mens-select"
              aria-label="Filtrar por competência"
              value={competenciaFiltro}
              onChange={(e) => setCompetenciaFiltro(e.target.value)}
            >
              <option value="">Todas as competências</option>
              {competenciasDisponiveis.map((comp) => (
                <option key={comp} value={comp}>
                  {formatarCompetencia(comp)}
                </option>
              ))}
            </select>
          </div>
        </div>

        {erro && (
          <div className="mens-alert mens-alert-erro" role="alert">
            {erro}
          </div>
        )}

        {carregando ? (
          <div className="mens-loading">Carregando relatório de inadimplentes...</div>
        ) : filtradosEOrdenados.length === 0 ? (
          <EstadoVazio
            titulo="Nenhum inadimplente encontrado"
            descricao="Não há mensalidades atrasadas correspondentes aos filtros atuais."
          />
        ) : (
          <div className="mens-table-wrap">
            <table className="mens-table">
              <thead>
                <tr>
                  <th>
                    <button
                      type="button"
                      className="mens-sort-btn"
                      onClick={() => alternarOrdenacao('associadoId')}
                    >
                      <span>ID Associado</span>
                      <ArrowUpDown size={12} />
                    </button>
                  </th>
                  <th>
                    <button
                      type="button"
                      className="mens-sort-btn"
                      onClick={() => alternarOrdenacao('nomeAssociado')}
                    >
                      <span>Nome do associado</span>
                      <ArrowUpDown size={12} />
                    </button>
                  </th>
                  <th>
                    <button
                      type="button"
                      className="mens-sort-btn"
                      onClick={() => alternarOrdenacao('valor')}
                    >
                      <span>Valor</span>
                      <ArrowUpDown size={12} />
                    </button>
                  </th>
                  <th>
                    <button
                      type="button"
                      className="mens-sort-btn"
                      onClick={() => alternarOrdenacao('competencia')}
                    >
                      <span>Competência</span>
                      <ArrowUpDown size={12} />
                    </button>
                  </th>
                  <th>
                    <button
                      type="button"
                      className="mens-sort-btn"
                      onClick={() => alternarOrdenacao('dataVencimento')}
                    >
                      <span>Data do vencimento</span>
                      <ArrowUpDown size={12} />
                    </button>
                  </th>
                  <th>Ações</th>
                </tr>
              </thead>
              <tbody>
                {filtradosEOrdenados.map((item, index) => (
                  <tr key={`${item.associadoId}-${item.competencia}-${index}`}>
                    <td>#{item.associadoId}</td>
                    <td>
                      <strong>{item.nomeAssociado}</strong>
                    </td>
                    <td>
                      <strong>{formatCurrency(item.valor)}</strong>
                    </td>
                    <td>{formatarCompetencia(item.competencia)}</td>
                    <td>{formatarDataBr(item.dataVencimento)}</td>
                    <td>
                      <button
                        type="button"
                        className="mens-btn-secondary"
                        onClick={() =>
                          navigate(
                            `/mensalidades?associadoId=${item.associadoId}&nome=${encodeURIComponent(item.nomeAssociado)}`,
                          )
                        }
                      >
                        Ver histórico
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        <footer className="mens-report-footer">
          <div>
            Total de inadimplentes: <strong>{filtradosEOrdenados.length}</strong>
          </div>
          <div>
            Valor total de inadimplência: <strong>{formatCurrency(totalInadimplencia)}</strong>
          </div>
        </footer>
      </section>
    </div>
  )
}
