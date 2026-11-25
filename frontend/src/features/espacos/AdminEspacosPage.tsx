import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react'
import toast from 'react-hot-toast'
import { AlertCircle, Ban, Building2, CalendarDays, CheckCircle2, ChevronLeft, ChevronRight, Clock, Dumbbell, Pencil, Plus, Search, Sparkles, UserPlus, Users, Wrench } from 'lucide-react'
import { Modal } from '../../components/Modal/Modal'
import { useAuth } from '../../context/AuthContext'
import { getApiErrorMessage } from '../../services/api'
import {
  espacosService,
  type Bloco,
  type BlocoDisponibilidade,
  type Espaco,
  type EspacoPayload,
  type EstadoEspaco,
  type TipoBloco,
} from '../../services/espacosService'
import { ESTADO_LABEL, SITUACAO_CLASSE, SITUACAO_LABEL, formatarData, hojeISO, iconeDoEspaco, maisDuasHoras, somarDias } from './espacosUtils'
import './espacos.css'

const VAZIO: EspacoPayload = { nome: '', publico: false, usoExclusivoTreino: false, limiteConvidados: 0, capacidadeMaxima: null }
// Espaços de exemplo para testar o fluxo de reservas pela própria interface (sem mexer no banco).
// Cada um recebe blocos de 2h das 08h às 22h; na Quadra de Tênis 1 o das 08h é de treino, para ver um bloco não reservável.
const EXEMPLOS: EspacoPayload[] = [
  { nome: 'Quadra de Tênis 1', publico: false, usoExclusivoTreino: false, limiteConvidados: 2, capacidadeMaxima: 4 },
  { nome: 'Quadra de Tênis 2', publico: false, usoExclusivoTreino: false, limiteConvidados: 2, capacidadeMaxima: 4 },
  { nome: 'Quadra de Beach Tennis', publico: false, usoExclusivoTreino: false, limiteConvidados: 2, capacidadeMaxima: 4 },
  { nome: 'Quadra Poliesportiva', publico: false, usoExclusivoTreino: false, limiteConvidados: 10, capacidadeMaxima: 20 },
  { nome: 'Campo Society', publico: false, usoExclusivoTreino: false, limiteConvidados: 8, capacidadeMaxima: 14 },
  { nome: 'Churrasqueira', publico: false, usoExclusivoTreino: false, limiteConvidados: 20, capacidadeMaxima: 30 },
  { nome: 'Salão de Festas', publico: false, usoExclusivoTreino: false, limiteConvidados: 50, capacidadeMaxima: 80 },
]
const INICIOS_EXEMPLO = ['08:00', '10:00', '12:00', '14:00', '16:00', '18:00', '20:00']
const CLASSE_ESTADO: Record<EstadoEspaco, string> = { ATIVO: 'esp-badge--ok', EM_MANUTENCAO: 'esp-badge--warn', INATIVO: 'esp-badge--off' }

// UC-GE: cadastro de espaços (RECEPCAO/ADMIN); blocos de horário só o ADMIN configura.
export function AdminEspacosPage() {
  const { usuario } = useAuth()
  const ehAdmin = usuario?.perfil === 'ADMIN'

  const [espacos, setEspacos] = useState<Espaco[]>([])
  const [erro, setErro] = useState('')
  const [carregando, setCarregando] = useState(true)
  const [busca, setBusca] = useState('')
  const [filtro, setFiltro] = useState<'TODOS' | EstadoEspaco>('TODOS')

  // formulário
  const [formAberto, setFormAberto] = useState(false)
  const [editando, setEditando] = useState<Espaco | null>(null)
  const [form, setForm] = useState<EspacoPayload>(VAZIO)
  const [erroForm, setErroForm] = useState('')
  const [salvando, setSalvando] = useState(false)
  const [criandoExemplos, setCriandoExemplos] = useState(false)

  // agenda e blocos
  const [agendaDe, setAgendaDe] = useState<Espaco | null>(null)
  const [blocos, setBlocos] = useState<Bloco[]>([])
  const [dataAgenda, setDataAgenda] = useState(hojeISO())
  const [agenda, setAgenda] = useState<BlocoDisponibilidade[]>([])
  const [erroAgenda, setErroAgenda] = useState('')
  const [novoInicio, setNovoInicio] = useState('08:00')
  const [novoTipo, setNovoTipo] = useState<TipoBloco>('LIVRE')

  const carregar = useCallback(async () => {
    try {
      setEspacos(await espacosService.listarEspacos())
    } catch (e) {
      setErro(getApiErrorMessage(e, 'Não foi possível carregar os espaços.'))
    } finally {
      setCarregando(false)
    }
  }, [])

  useEffect(() => {
    void carregar()
  }, [carregar])

  const carregarAgenda = useCallback(async (espaco: Espaco, data: string) => {
    setErroAgenda('')
    try {
      const [b, d] = await Promise.all([
        espacosService.listarBlocos(espaco.id),
        espacosService.disponibilidade(espaco.id, data),
      ])
      setBlocos(b)
      setAgenda(d.blocos)
    } catch (e) {
      setErroAgenda(getApiErrorMessage(e, 'Não foi possível carregar a agenda.'))
    }
  }, [])

  useEffect(() => {
    if (agendaDe) void carregarAgenda(agendaDe, dataAgenda)
  }, [agendaDe, dataAgenda, carregarAgenda])

  // ADMIN: cria os espaços de exemplo que ainda não existem (por nome) e seus blocos, usando a própria API.
  async function criarExemplos() {
    setCriandoExemplos(true)
    try {
      const existentes = new Set((await espacosService.listarEspacos()).map((e) => e.nome.trim().toLowerCase()))
      let criados = 0
      for (const ex of EXEMPLOS) {
        if (existentes.has(ex.nome.toLowerCase())) continue
        const espaco = await espacosService.cadastrarEspaco(ex)
        for (const inicio of INICIOS_EXEMPLO) {
          const tipo: TipoBloco = ex.nome === 'Quadra de Tênis 1' && inicio === '08:00' ? 'TREINO' : 'LIVRE'
          await espacosService.adicionarBloco(espaco.id, { horaInicio: inicio, horaFim: maisDuasHoras(inicio), tipo })
        }
        criados += 1
      }
      toast.success(criados > 0 ? `${criados} espaço(s) de exemplo criado(s).` : 'Os espaços de exemplo já existem.')
    } catch (e) {
      toast.error(getApiErrorMessage(e, 'Não foi possível criar os espaços de exemplo.'))
    } finally {
      setCriandoExemplos(false)
      await carregar()
    }
  }

  function novo() {
    setEditando(null)
    setForm(VAZIO)
    setErroForm('')
    setFormAberto(true)
  }

  function editar(e: Espaco) {
    setEditando(e)
    setForm({ nome: e.nome, publico: e.publico, usoExclusivoTreino: e.usoExclusivoTreino, limiteConvidados: e.limiteConvidados, capacidadeMaxima: e.capacidadeMaxima })
    setErroForm('')
    setFormAberto(true)
  }

  async function salvar(ev: FormEvent) {
    ev.preventDefault()
    setSalvando(true)
    setErroForm('')
    const dto: EspacoPayload = { ...form, nome: form.nome.trim(), capacidadeMaxima: form.publico ? null : form.capacidadeMaxima }
    try {
      if (editando) await espacosService.atualizarEspaco(editando.id, dto)
      else await espacosService.cadastrarEspaco(dto)
      toast.success('Espaço salvo.')
      setFormAberto(false)
      await carregar()
    } catch (e) {
      setErroForm(getApiErrorMessage(e, 'Não foi possível salvar o espaço.'))
    } finally {
      setSalvando(false)
    }
  }

  async function mudarEstado(e: Espaco, estado: EstadoEspaco) {
    try {
      await espacosService.alterarEstado(e.id, estado)
      toast.success('Estado atualizado. Reservas já feitas são mantidas.')
      await carregar()
    } catch (err) {
      toast.error(getApiErrorMessage(err, 'Não foi possível alterar o estado.'))
    }
  }

  async function adicionarBloco() {
    if (!agendaDe) return
    const fim = maisDuasHoras(novoInicio)
    if (!fim) {
      setErroAgenda('Horário inválido: o bloco de 2h precisa terminar até 23:59.')
      return
    }
    try {
      await espacosService.adicionarBloco(agendaDe.id, { horaInicio: novoInicio, horaFim: fim, tipo: novoTipo })
      toast.success('Bloco adicionado.')
      await carregarAgenda(agendaDe, dataAgenda)
    } catch (e) {
      setErroAgenda(getApiErrorMessage(e, 'Não foi possível adicionar o bloco.'))
    }
  }

  const totais = useMemo(() => ({
    TODOS: espacos.length,
    ATIVO: espacos.filter((e) => e.estado === 'ATIVO').length,
    EM_MANUTENCAO: espacos.filter((e) => e.estado === 'EM_MANUTENCAO').length,
    INATIVO: espacos.filter((e) => e.estado === 'INATIVO').length,
  }), [espacos])

  const visiveis = useMemo(() => {
    const termo = busca.trim().toLowerCase()
    return espacos.filter((e) => (filtro === 'TODOS' || e.estado === filtro) && (termo === '' || e.nome.toLowerCase().includes(termo)))
  }, [espacos, busca, filtro])

  return (
    <div className="esp-page">
      <header className="esp-hero">
        <div className="esp-hero-main">
          <span className="esp-eyebrow"><Building2 size={13} /> Administração</span>
          <h1 className="esp-title">Espaços do clube</h1>
          <p className="esp-subtitle">Cadastre espaços, controle o estado e consulte a agenda.</p>
        </div>
        <div className="esp-hero-actions">
          {ehAdmin && (
            <button type="button" className="esp-btn esp-btn--outline-light" disabled={criandoExemplos} onClick={() => void criarExemplos()}>
              <Sparkles size={16} /> {criandoExemplos ? 'Criando…' : 'Criar exemplos'}
            </button>
          )}
          <button type="button" className="esp-btn esp-btn--light" onClick={novo}>
            <Plus size={16} /> Novo espaço
          </button>
        </div>
      </header>

      {erro && <p className="esp-alert esp-alert--erro" role="alert"><AlertCircle size={16} />{erro}</p>}

      <div className="esp-stats">
        <div className="esp-stat">
          <span className="esp-stat-icone"><Building2 size={20} /></span>
          <div><strong>{totais.TODOS}</strong><span>Espaços</span></div>
        </div>
        <div className="esp-stat">
          <span className="esp-stat-icone esp-stat-icone--ok"><CheckCircle2 size={20} /></span>
          <div><strong>{totais.ATIVO}</strong><span>Ativos</span></div>
        </div>
        <div className="esp-stat">
          <span className="esp-stat-icone esp-stat-icone--warn"><Wrench size={20} /></span>
          <div><strong>{totais.EM_MANUTENCAO}</strong><span>Em manutenção</span></div>
        </div>
        <div className="esp-stat">
          <span className="esp-stat-icone esp-stat-icone--off"><Ban size={20} /></span>
          <div><strong>{totais.INATIVO}</strong><span>Inativos</span></div>
        </div>
      </div>

      <div className="esp-toolbar">
        <label className="esp-search">
          <Search size={16} aria-hidden="true" />
          <input type="search" placeholder="Buscar espaço pelo nome" aria-label="Buscar espaço pelo nome" value={busca} onChange={(e) => setBusca(e.target.value)} />
        </label>
        <div className="esp-pills" role="group" aria-label="Filtrar por estado">
          {(['TODOS', 'ATIVO', 'EM_MANUTENCAO', 'INATIVO'] as const).map((f) => (
            <button key={f} type="button" className={`esp-pill ${filtro === f ? 'esp-pill--sel' : ''}`} aria-pressed={filtro === f} onClick={() => setFiltro(f)}>
              {f === 'TODOS' ? 'Todos' : ESTADO_LABEL[f]} <small>{totais[f]}</small>
            </button>
          ))}
        </div>
      </div>

      {carregando ? (
        <div className="esp-espacos" aria-busy="true" aria-label="Carregando espaços">
          {Array.from({ length: 3 }, (_, i) => <div key={i} className="esp-skeleton" />)}
        </div>
      ) : visiveis.length === 0 ? (
        <div className="esp-card">
          <div className="esp-vazio-cta">
            <span className="esp-vazio-icone"><Building2 size={26} /></span>
            <strong>{espacos.length === 0 ? 'Nenhum espaço cadastrado' : 'Nenhum espaço encontrado'}</strong>
            <p>{espacos.length === 0 ? 'Cadastre o primeiro espaço para liberar reservas aos associados.' : 'Ajuste a busca ou o filtro de estado.'}</p>
            {espacos.length === 0 && !erro && (
              <button type="button" className="esp-btn esp-btn--primary" onClick={novo}><Plus size={16} /> Novo espaço</button>
            )}
          </div>
        </div>
      ) : (
        <ul className="esp-espacos">
          {visiveis.map((e) => {
            const Icone = iconeDoEspaco(e.nome)
            return (
              <li key={e.id} className={`esp-espaco ${e.estado === 'INATIVO' ? 'esp-espaco--inativo' : ''}`}>
                <div className="esp-espaco-topo">
                  <span className="esp-espaco-icone" aria-hidden="true"><Icone size={22} /></span>
                  <div className="esp-espaco-nome">
                    <h3>{e.nome}</h3>
                    <span className={`esp-badge ${CLASSE_ESTADO[e.estado]}`}>{ESTADO_LABEL[e.estado]}</span>
                  </div>
                </div>
                <div className="esp-espaco-tags">
                  <span className="esp-tag"><Users size={12} /> {e.publico ? 'Público' : `Capacidade ${e.capacidadeMaxima ?? '—'}`}</span>
                  <span className="esp-tag"><UserPlus size={12} /> até {e.limiteConvidados} convidado(s)</span>
                  {e.usoExclusivoTreino && <span className="esp-tag esp-tag--treino"><Dumbbell size={12} /> Uso exclusivo de treino</span>}
                </div>
                <div className="esp-espaco-rodape">
                  <select
                    className="esp-select"
                    aria-label={`Estado de ${e.nome}`}
                    value={e.estado}
                    onChange={(ev) => void mudarEstado(e, ev.target.value as EstadoEspaco)}
                  >
                    {(Object.keys(ESTADO_LABEL) as EstadoEspaco[]).map((st) => (
                      <option key={st} value={st}>{ESTADO_LABEL[st]}</option>
                    ))}
                  </select>
                  <div className="esp-espaco-botoes">
                    <button type="button" className="esp-btn esp-btn--ghost esp-btn--sm" onClick={() => editar(e)}><Pencil size={14} /> Editar</button>
                    <button type="button" className="esp-btn esp-btn--sm" onClick={() => setAgendaDe(e)}><CalendarDays size={14} /> Agenda</button>
                  </div>
                </div>
              </li>
            )
          })}
        </ul>
      )}

      <Modal
        isOpen={formAberto}
        onClose={() => setFormAberto(false)}
        title={editando ? 'Editar espaço' : 'Novo espaço'}
        maxWidth={480}
      >
        <form className="esp-modal-body" onSubmit={salvar}>
          <label className="esp-field">
            Nome
            <input value={form.nome} onChange={(e) => setForm({ ...form, nome: e.target.value })} maxLength={100} required placeholder="Ex.: Quadra de Tênis 1" />
          </label>
          <label className="esp-field esp-check">
            <input type="checkbox" checked={form.publico} onChange={(e) => setForm({ ...form, publico: e.target.checked })} />
            <span>Espaço público<small>Sem limite de capacidade</small></span>
          </label>
          <label className="esp-field esp-check">
            <input type="checkbox" checked={form.usoExclusivoTreino} onChange={(e) => setForm({ ...form, usoExclusivoTreino: e.target.checked })} />
            <span>Uso exclusivo de treino<small>Não pode ser reservado pelos associados</small></span>
          </label>
          <div className="esp-form-grid">
            <label className="esp-field">
              Limite de convidados
              <input type="number" min={0} value={form.limiteConvidados} onChange={(e) => setForm({ ...form, limiteConvidados: Number(e.target.value) || 0 })} />
            </label>
            {!form.publico && (
              <label className="esp-field">
                Capacidade máxima (AVCB)
                <input
                  type="number"
                  min={1}
                  value={form.capacidadeMaxima ?? ''}
                  onChange={(e) => setForm({ ...form, capacidadeMaxima: e.target.value === '' ? null : Number(e.target.value) })}
                  required
                />
              </label>
            )}
          </div>
          {erroForm && <p className="esp-alert esp-alert--erro" role="alert"><AlertCircle size={16} />{erroForm}</p>}
          <div className="esp-modal-actions">
            <button type="button" className="esp-btn esp-btn--ghost" onClick={() => setFormAberto(false)}>Fechar</button>
            <button type="submit" className="esp-btn esp-btn--primary" disabled={salvando}>{salvando ? 'Salvando…' : 'Salvar'}</button>
          </div>
        </form>
      </Modal>

      <Modal
        isOpen={agendaDe !== null}
        onClose={() => setAgendaDe(null)}
        title={agendaDe ? `Agenda — ${agendaDe.nome}` : 'Agenda'}
        subtitle={`Dia ${formatarData(dataAgenda)}`}
        maxWidth={560}
      >
        <div className="esp-modal-body">
          <div className="esp-agenda-bar">
            <div className="esp-agenda-nav">
              <button type="button" className="esp-cal-seta" aria-label="Dia anterior" onClick={() => setDataAgenda(somarDias(dataAgenda, -1))}><ChevronLeft size={18} /></button>
              <button type="button" className="esp-cal-seta" aria-label="Próximo dia" onClick={() => setDataAgenda(somarDias(dataAgenda, 1))}><ChevronRight size={18} /></button>
            </div>
            <label className="esp-field">
              Data
              <input type="date" value={dataAgenda} onChange={(e) => e.target.value && setDataAgenda(e.target.value)} />
            </label>
          </div>
          {erroAgenda && <p className="esp-alert esp-alert--erro" role="alert"><AlertCircle size={16} />{erroAgenda}</p>}
          {agenda.length === 0 ? (
            <p className="esp-alert esp-alert--info">Nenhum bloco configurado.</p>
          ) : (
            <ul className="esp-slotlist">
              {agenda.map((b) => (
                <li key={b.blocoId} className={`esp-slotrow esp-slotrow--${SITUACAO_CLASSE[b.situacao]}`}>
                  <strong><Clock size={15} /> {b.horaInicio} – {b.horaFim}</strong>
                  <span className={`esp-badge ${b.situacao === 'LIVRE' ? 'esp-badge--ok' : b.situacao === 'OCUPADO' ? 'esp-badge--warn' : ''}`}>
                    {SITUACAO_LABEL[b.situacao]}
                  </span>
                </li>
              ))}
            </ul>
          )}
          <p className="esp-muted">{blocos.length} bloco(s) configurado(s) neste espaço.</p>
          {ehAdmin && (
            <div className="esp-novobloco">
              <h4>Adicionar bloco de 2h</h4>
              <div className="esp-form-grid">
                <label className="esp-field">
                  Início do novo bloco
                  <input type="time" value={novoInicio} onChange={(e) => setNovoInicio(e.target.value)} />
                </label>
                <label className="esp-field">
                  Tipo
                  <select value={novoTipo} onChange={(e) => setNovoTipo(e.target.value as TipoBloco)}>
                    <option value="LIVRE">Livre</option>
                    <option value="TREINO">Treino</option>
                    <option value="MANUTENCAO">Manutenção</option>
                  </select>
                </label>
              </div>
              <button type="button" className="esp-btn esp-btn--primary" onClick={() => void adicionarBloco()}><Plus size={16} /> Adicionar bloco</button>
            </div>
          )}
        </div>
      </Modal>
    </div>
  )
}
