import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import toast from 'react-hot-toast'
import { ChevronLeft, Play } from 'lucide-react'
import { Modal } from '../../components/Modal/Modal'
import { useAuth } from '../../context/AuthContext'
import { getApiErrorMessage } from '../../services/api'
import { associadoService } from '../../services/associadoService'
import { financeiroService } from '../../services/financeiroService'
import type { ResumoFechamento } from '../../types/financeiro'
import { HOME_POR_PERFIL } from '../../types/user'
import { formatDateFull } from '../../utils/formatters'
import { EstadoVazio } from './components/EstadoVazio'
import { formatarCompetencia } from './mensalidadesUtils'
import './components/mensalidadesShared.css'
import './MensalidadesAssociadoPage.css'
import './MinhasMensalidadesPanel.css'
import './InadimplentesPage.css'
import './ProcessamentoMensalidadesPage.css'

export function ProcessamentoMensalidadesPage() {
  const { usuario } = useAuth()
  const home = usuario ? HOME_POR_PERFIL[usuario.perfil] : '/'
  const ehAdmin = usuario?.perfil === 'ADMIN'

  const [resumo, setResumo] = useState<ResumoFechamento | null>(null)
  const [nomesAssociados, setNomesAssociados] = useState<Record<number, string>>({})
  const [carregando, setCarregando] = useState(true)
  const [executando, setExecutando] = useState(false)
  const [modalConfirmar, setModalConfirmar] = useState(false)
  const [erro, setErro] = useState('')

  const carregarUltimo = useCallback(async () => {
    setCarregando(true)
    setErro('')
    try {
      const ultimo = await financeiroService.ultimoFechamento()
      setResumo(ultimo)

      if (ehAdmin && ultimo && ultimo.titularesComFalha.length > 0) {
        try {
          const lista = await associadoService.listar()
          const mapa: Record<number, string> = {}
          for (const assoc of lista) {
            mapa[assoc.id] = assoc.pessoa.nome
          }
          setNomesAssociados(mapa)
        } catch {
          // Caso falhe a busca de nomes, exibe apenas os IDs
        }
      }
    } catch (err) {
      setErro(getApiErrorMessage(err, 'Não foi possível carregar o último processamento.'))
    } finally {
      setCarregando(false)
    }
  }, [ehAdmin])

  useEffect(() => {
    void carregarUltimo()
  }, [carregarUltimo])

  async function executarFechamento() {
    setExecutando(true)
    setErro('')
    try {
      const resultado = await financeiroService.executarFechamento()
      setResumo(resultado)
      setModalConfirmar(false)
      toast.success(
        `Fechamento da competência ${formatarCompetencia(resultado.competencia)} concluído!`,
      )

      if (ehAdmin && resultado.titularesComFalha.length > 0) {
        try {
          const lista = await associadoService.listar()
          const mapa: Record<number, string> = {}
          for (const assoc of lista) {
            mapa[assoc.id] = assoc.pessoa.nome
          }
          setNomesAssociados(mapa)
        } catch {
          // Mantém mapa existente
        }
      }
    } catch (err) {
      setErro(getApiErrorMessage(err, 'Não foi possível executar o fechamento mensal.'))
    } finally {
      setExecutando(false)
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
            <span className="mens-breadcrumb-active">Processamento</span>
          </nav>
        </div>

        <button
          type="button"
          className="mens-btn-navy"
          onClick={() => setModalConfirmar(true)}
          disabled={executando}
        >
          <Play size={14} />
          <span>{executando ? 'Executando...' : 'Executar fechamento'}</span>
        </button>
      </header>

      {erro && (
        <div className="mens-alert mens-alert-erro" role="alert">
          {erro}
        </div>
      )}

      <section className="mens-report-card">
        <div className="mens-report-header">
          <div>
            <h2 className="mens-report-title">Relatório de falhas de processamento</h2>
            <p className="mens-proc-subtitle">
              Data do último processamento:{' '}
              <strong>
                {resumo?.dataExecucao ? formatDateFull(resumo.dataExecucao) : '—'}
              </strong>
            </p>
          </div>
        </div>

        {carregando ? (
          <div className="mens-loading">Carregando dados do último processamento...</div>
        ) : !resumo ? (
          <EstadoVazio
            titulo="Nenhum fechamento executado ainda"
            descricao="Clique em Executar fechamento para processar as mensalidades da competência corrente."
          />
        ) : (
          <>
            <div className="mens-proc-kpis">
              <div className="mens-proc-kpi">
                <span>Competência</span>
                <strong>{formatarCompetencia(resumo.competencia)}</strong>
              </div>
              <div className="mens-proc-kpi">
                <span>Processados</span>
                <strong>{resumo.processados}</strong>
              </div>
              <div className="mens-proc-kpi">
                <span>Ignorados</span>
                <strong>{resumo.ignorados}</strong>
              </div>
              <div className="mens-proc-kpi">
                <span>Titulares com falha</span>
                <strong>{resumo.titularesComFalha.length}</strong>
              </div>
            </div>

            {resumo.titularesComFalha.length === 0 ? (
              <EstadoVazio
                titulo="Nenhuma falha de processamento"
                descricao="Todos os titulares elegíveis foram processados sem erros na última execução."
              />
            ) : (
              <div className="mens-table-wrap">
                <table className="mens-table">
                  <thead>
                    <tr>
                      <th>ID do associado</th>
                      {ehAdmin && <th>Nome do associado</th>}
                    </tr>
                  </thead>
                  <tbody>
                    {resumo.titularesComFalha.map((id) => (
                      <tr key={id}>
                        <td>#{id}</td>
                        {ehAdmin && <td>{nomesAssociados[id] ?? '—'}</td>}
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}

            <footer className="mens-report-footer">
              <div>
                Total de falhas: <strong>{resumo.titularesComFalha.length}</strong>
              </div>
            </footer>
          </>
        )}
      </section>

      <Modal
        isOpen={modalConfirmar}
        onClose={() => !executando && setModalConfirmar(false)}
        title="Executar fechamento mensal"
        maxWidth={460}
      >
        <div className="mens-modal-body">
          <div className="mens-alert mens-alert-info">
            O fechamento atua na competência corrente e é idempotente.
          </div>
          <div className="mens-modal-actions">
            <button
              type="button"
              className="mens-btn-secondary"
              onClick={() => setModalConfirmar(false)}
              disabled={executando}
            >
              Cancelar
            </button>
            <button
              type="button"
              className="mens-btn-primary"
              onClick={() => void executarFechamento()}
              disabled={executando}
            >
              {executando ? 'Executando...' : 'Confirmar execução'}
            </button>
          </div>
        </div>
      </Modal>
    </div>
  )
}
