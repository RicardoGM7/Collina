import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  AlertCircle,
  Building2,
  CalendarDays,
  ChevronLeft,
  ChevronRight,
  Clock,
  UserPlus,
  Users,
} from 'lucide-react'
import { useAuth } from '../../context/AuthContext'
import { getApiErrorMessage } from '../../services/api'
import { associadoService } from '../../services/associadoService'
import { espacosService, type Reserva } from '../../services/espacosService'
import { PLANO_NOME, type Associado } from '../../types/associado'
import { HOME_POR_PERFIL } from '../../types/user'
import { AdminEspacosPanel } from './AdminEspacosPanel'
import { CancelarReservaModal } from './CancelarReservaModal'
import { STATUS_RESERVA_LABEL, hojeISO, partesDaData, podeCancelar, rotuloConvidados } from './espacosUtils'
import './espacos.css'

type AbaAdminReservas = 'reservas' | 'espacos'

interface AdminReservasPageProps {
  abaInicial?: AbaAdminReservas
}

// Recepção/administração: consulta e cancela reservas em nome do titular e gerencia espaços do clube.
export function AdminReservasPage({ abaInicial = 'reservas' }: AdminReservasPageProps) {
  const { usuario } = useAuth()
  const rotaVoltar = usuario ? HOME_POR_PERFIL[usuario.perfil] : '/admin'

  const hoje = hojeISO()
  const [aba, setAba] = useState<AbaAdminReservas>(abaInicial)
  const [totalEspacos, setTotalEspacos] = useState<number | null>(null)
  const [associados, setAssociados] = useState<Associado[]>([])
  const [associadoId, setAssociadoId] = useState('')
  const [reservas, setReservas] = useState<Reserva[]>([])
  const [erro, setErro] = useState('')
  const [paraCancelar, setParaCancelar] = useState<Reserva | null>(null)
  const [filtro, setFiltro] = useState<'proximas' | 'anteriores' | 'canceladas' | 'todas'>('proximas')

  useEffect(() => {
    setAba(abaInicial)
  }, [abaInicial])

  useEffect(() => {
    associadoService
      .listar()
      .then(setAssociados)
      .catch((e) => setErro(getApiErrorMessage(e, 'Não foi possível carregar os associados.')))
  }, [])

  const carregar = useCallback(async () => {
    if (!associadoId) {
      setReservas([])
      return
    }
    setErro('')
    try {
      setReservas(await espacosService.reservasDoAssociado(Number(associadoId)))
    } catch (e) {
      setReservas([])
      setErro(getApiErrorMessage(e, 'Não foi possível carregar as reservas.'))
    }
  }, [associadoId])

  useEffect(() => {
    void carregar()
  }, [carregar])

  const titular = associados.find((a) => String(a.id) === associadoId) ?? null
  const ehProxima = (r: Reserva) => r.status === 'CONFIRMADA' && r.dataUso >= hoje
  const ordenar = (a: Reserva, b: Reserva) => `${b.dataUso} ${b.horaInicio}`.localeCompare(`${a.dataUso} ${a.horaInicio}`)

  const contagens = {
    proximas: reservas.filter(ehProxima).length,
    anteriores: reservas.filter((r) => r.status === 'CONFIRMADA' && r.dataUso < hoje).length,
    canceladas: reservas.filter((r) => r.status === 'CANCELADA').length,
    todas: reservas.length,
  }
  const visiveis = reservas
    .filter((r) => {
      if (filtro === 'proximas') return ehProxima(r)
      if (filtro === 'anteriores') return r.status === 'CONFIRMADA' && r.dataUso < hoje
      if (filtro === 'canceladas') return r.status === 'CANCELADA'
      return true
    })
    // próximas: da mais perto para a mais longe; demais: da mais recente para a mais antiga
    .sort((a, b) => (filtro === 'proximas' ? -ordenar(a, b) : ordenar(a, b)))

  const FILTROS = [
    { id: 'proximas', rotulo: 'Próximas' },
    { id: 'anteriores', rotulo: 'Anteriores' },
    { id: 'canceladas', rotulo: 'Canceladas' },
    { id: 'todas', rotulo: 'Todas' },
  ] as const

  const iniciais = (nome: string) => nome.split(' ').filter(Boolean).slice(0, 2).map((p) => p[0]).join('').toUpperCase()

  return (
    <div className="esp-shell">
      <div className="esp-topbar">
        <Link to={rotaVoltar} className="esp-btn-back">
          <ChevronLeft size={14} strokeWidth={2.5} />
          <span>Voltar ao Início</span>
        </Link>

        <nav className="esp-breadcrumb" aria-label="Breadcrumb">
          <span>Visão geral</span>
          <ChevronRight size={14} />
          <span className="esp-breadcrumb-active">
            {aba === 'reservas' ? 'Reservas' : 'Gerir espaços'}
          </span>
        </nav>
      </div>

      <div className="esp-page">
        <header className="esp-hero">
          {aba === 'reservas' ? (
            <div className="esp-hero-main">
              <span className="esp-eyebrow">
                <CalendarDays size={13} /> Atendimento
              </span>
              <h1 className="esp-title">Reservas de espaços</h1>
              <p className="esp-subtitle">Consulte e cancele reservas em nome do associado titular.</p>
            </div>
          ) : (
            <div className="esp-hero-main">
              <span className="esp-eyebrow">
                <Building2 size={13} /> Administração
              </span>
              <h1 className="esp-title">Espaços do clube</h1>
              <p className="esp-subtitle">
                Cadastre, edite ou remova espaços, controle o estado e configure a agenda.
              </p>
            </div>
          )}
        </header>

        <div className="esp-tabs" role="tablist">
          <button
            type="button"
            role="tab"
            aria-selected={aba === 'reservas'}
            className={`esp-tab ${aba === 'reservas' ? 'esp-tab--ativa' : ''}`}
            onClick={() => setAba('reservas')}
          >
            <CalendarDays size={16} /> Reservas
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={aba === 'espacos'}
            className={`esp-tab ${aba === 'espacos' ? 'esp-tab--ativa' : ''}`}
            onClick={() => setAba('espacos')}
          >
            <Building2 size={16} /> Gerir espaços
            {totalEspacos !== null && <span className="esp-tab-count">{totalEspacos}</span>}
          </button>
        </div>

        {aba === 'reservas' ? (
          <>
            <div className="esp-card">
              <div className="esp-filtro">
                <label className="esp-field">
                  Associado titular
                  <select value={associadoId} onChange={(e) => setAssociadoId(e.target.value)}>
                    <option value="">Selecione…</option>
                    {associados.map((a) => (
                      <option key={a.id} value={a.id}>
                        {a.pessoa.nome}
                      </option>
                    ))}
                  </select>
                </label>
              </div>

              {erro && (
                <p className="esp-alert esp-alert--erro" role="alert">
                  <AlertCircle size={16} />
                  {erro}
                </p>
              )}

              {titular && (
                <div className="esp-titular">
                  <span className="esp-avatar" aria-hidden="true">
                    {iniciais(titular.pessoa.nome)}
                  </span>
                  <div>
                    <strong>{titular.pessoa.nome}</strong>
                    <span>
                      {PLANO_NOME[titular.plano.tipo]} · até {titular.plano.limiteMensalReservas} reservas por mês
                    </span>
                  </div>
                </div>
              )}
            </div>

            {!associadoId ? (
              <div className="esp-card">
                <div className="esp-vazio-cta">
                  <span className="esp-vazio-icone">
                    <Users size={26} />
                  </span>
                  <strong>Escolha um associado</strong>
                  <p>Selecione o titular acima para ver o histórico de reservas e cancelar em nome dele.</p>
                </div>
              </div>
            ) : (
              <div className="esp-card">
                <div className="esp-pills" role="group" aria-label="Filtrar reservas">
                  {FILTROS.map((f) => (
                    <button
                      key={f.id}
                      type="button"
                      className={`esp-pill ${filtro === f.id ? 'esp-pill--sel' : ''}`}
                      aria-pressed={filtro === f.id}
                      onClick={() => setFiltro(f.id)}
                    >
                      {f.rotulo} <small>{contagens[f.id]}</small>
                    </button>
                  ))}
                </div>

                {visiveis.length === 0 && !erro ? (
                  <div className="esp-vazio-cta">
                    <span className="esp-vazio-icone">
                      <CalendarDays size={26} />
                    </span>
                    <strong>{reservas.length === 0 ? 'Nenhuma reserva encontrada' : 'Nada neste filtro'}</strong>
                    <p>
                      {reservas.length === 0
                        ? 'Este associado ainda não fez reservas.'
                        : 'Troque o filtro para ver outras reservas.'}
                    </p>
                  </div>
                ) : (
                  <ul className="esp-list">
                    {visiveis.map((r) => {
                      const d = partesDaData(r.dataUso)
                      const passada = r.dataUso < hoje || r.status === 'CANCELADA'
                      return (
                        <li
                          key={r.id}
                          className={`esp-res ${r.status === 'CANCELADA' ? 'esp-res--cancelada' : ''} ${passada ? 'esp-res--passada' : ''}`}
                        >
                          <div className="esp-res-data" aria-hidden="true">
                            <span>{d.semana}</span>
                            <strong>{d.dia}</strong>
                            <span>{d.mes}</span>
                          </div>
                          <div className="esp-res-info">
                            <strong>{r.espacoNome}</strong>
                            <div className="esp-res-linha">
                              <span className="esp-meta">
                                <Clock size={14} /> {r.horaInicio} às {r.horaFim}
                              </span>
                              <span className="esp-meta">
                                <Users size={14} /> {rotuloConvidados(r.qtdConvidados)}
                              </span>
                              {r.dependenteNome && (
                                <span className="esp-meta">
                                  <UserPlus size={14} /> {r.dependenteNome}
                                </span>
                              )}
                            </div>
                          </div>
                          <div className="esp-res-lado">
                            <span className={`esp-badge ${r.status === 'CONFIRMADA' ? 'esp-badge--ok' : 'esp-badge--off'}`}>
                              {STATUS_RESERVA_LABEL[r.status]}
                            </span>
                            {r.status === 'CONFIRMADA' && podeCancelar(r.dataUso, hoje) && (
                              <button
                                type="button"
                                className="esp-btn esp-btn--ghost esp-btn--sm"
                                onClick={() => setParaCancelar(r)}
                              >
                                Cancelar
                              </button>
                            )}
                            {r.status === 'CONFIRMADA' && !passada && !podeCancelar(r.dataUso, hoje) && (
                              <span className="esp-res-aviso">Só dá para cancelar até o dia anterior</span>
                            )}
                          </div>
                        </li>
                      )
                    })}
                  </ul>
                )}
              </div>
            )}

            <CancelarReservaModal
              reserva={paraCancelar}
              onClose={() => setParaCancelar(null)}
              onCancelada={() => void carregar()}
            />
          </>
        ) : (
          <AdminEspacosPanel onTotalChange={setTotalEspacos} />
        )}
      </div>
    </div>
  )
}
