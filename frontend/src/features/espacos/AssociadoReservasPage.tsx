import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import toast from 'react-hot-toast'
import { AlertCircle, CalendarDays, Check, CheckCircle2, Clock, Info, MapPin, Minus, Plus, UserPlus, Users } from 'lucide-react'
import { getApiErrorMessage } from '../../services/api'
import { associadoService } from '../../services/associadoService'
import {
  espacosService,
  type BlocoDisponibilidade,
  type Espaco,
  type Reserva,
} from '../../services/espacosService'
import type { VinculoDependencia } from '../../types/associado'
import { useMeuCadastro } from '../associados/MeuCadastroContext'
import { CalendarioReservas, type MarcaDia } from './CalendarioReservas'
import { CancelarReservaModal } from './CancelarReservaModal'
import {
  IDADE_LIMITE_DEPENDENTE,
  celulasDoMes,
  MAX_DIAS_ANTECEDENCIA,
  SITUACAO_CLASSE,
  SITUACAO_LABEL,
  STATUS_RESERVA_LABEL,
  formatarDataComDia,
  hojeISO,
  iconeDoEspaco,
  idadeEm,
  partesDaData,
  podeCancelar,
  rotuloConvidados,
  somarDias,
} from './espacosUtils'
import './espacos.css'

type Aba = 'nova' | 'minhas'

// UC-GE01 / UC-GE03: o associado titular reserva blocos de 2h e cancela até o dia anterior.
export function AssociadoReservasPage() {
  const { associado } = useMeuCadastro()
  const hoje = hojeISO()
  const maxData = somarDias(hoje, MAX_DIAS_ANTECEDENCIA)

  const [aba, setAba] = useState<Aba>('nova')
  const [espacos, setEspacos] = useState<Espaco[]>([])
  const [dependentes, setDependentes] = useState<VinculoDependencia[]>([])
  const [minhas, setMinhas] = useState<Reserva[]>([])
  const [erroCarga, setErroCarga] = useState('')

  const [espacoId, setEspacoId] = useState('')
  const [data, setData] = useState(hoje)
  const [blocos, setBlocos] = useState<BlocoDisponibilidade[]>([])
  const [carregandoBlocos, setCarregandoBlocos] = useState(false)
  const [mensagem, setMensagem] = useState<string | null>(null)
  const [selecionados, setSelecionados] = useState<number[]>([])
  const [convidados, setConvidados] = useState(0)
  const [dependenteId, setDependenteId] = useState('')
  const [enviando, setEnviando] = useState(false)
  const [erro, setErro] = useState('')
  const [confirmadas, setConfirmadas] = useState<Reserva[]>([])
  const [paraCancelar, setParaCancelar] = useState<Reserva | null>(null)
  const [mesVisivel, setMesVisivel] = useState(hoje.slice(0, 7))
  const [totalLivres, setTotalLivres] = useState(0)
  const [livresPorDia, setLivresPorDia] = useState<Record<string, number>>({})
  const [versao, setVersao] = useState(0)
  const jaBuscados = useRef<Set<string>>(new Set())

  const espaco = espacos.find((e) => String(e.id) === espacoId) ?? null

  const carregarMinhas = useCallback(async () => {
    try {
      setMinhas(await espacosService.minhasReservas())
    } catch (e) {
      setErroCarga(getApiErrorMessage(e, 'Não foi possível carregar suas reservas.'))
    }
  }, [])

  useEffect(() => {
    let ativo = true
    async function carregar() {
      try {
        const [lista, deps] = await Promise.all([
          espacosService.listarEspacos(),
          associadoService.meusDependentes('ATIVO'),
        ])
        if (!ativo) return
        setEspacos(lista)
        setDependentes(deps)
      } catch (e) {
        if (ativo) setErroCarga(getApiErrorMessage(e, 'Não foi possível carregar os espaços.'))
      }
    }
    void carregar()
    void carregarMinhas()
    return () => {
      ativo = false
    }
  }, [carregarMinhas])

  const carregarDisponibilidade = useCallback(async () => {
    setSelecionados([])
    setErro('')
    if (!espacoId || !data) {
      setBlocos([])
      setMensagem(null)
      return
    }
    setCarregandoBlocos(true)
    try {
      const r = await espacosService.disponibilidade(Number(espacoId), data)
      setBlocos(r.blocos)
      setMensagem(r.mensagem)
    } catch (e) {
      setBlocos([])
      setMensagem(null)
      setErro(getApiErrorMessage(e, 'Não foi possível consultar a disponibilidade.'))
    } finally {
      setCarregandoBlocos(false)
    }
  }, [espacoId, data])

  useEffect(() => {
    void carregarDisponibilidade()
  }, [carregarDisponibilidade])

  // Total de blocos reserváveis do espaço escolhido (base para saber quantos horários outras pessoas já ocuparam)
  useEffect(() => {
    jaBuscados.current = new Set()
    setLivresPorDia({})
    setTotalLivres(0)
    if (!espacoId) return
    let ativo = true
    espacosService
      .listarBlocos(Number(espacoId))
      .then((lista) => { if (ativo) setTotalLivres(lista.filter((b) => b.tipo === 'LIVRE').length) })
      .catch(() => undefined)
    return () => { ativo = false }
  }, [espacoId, versao])

  // Horários livres de cada dia do mês exibido (só dias dentro da janela de reserva), com poucas requisições em paralelo
  useEffect(() => {
    if (!espacoId) return
    const dias = celulasDoMes(mesVisivel).filter(
      (iso): iso is string => iso !== null && iso >= hoje && iso <= maxData && !jaBuscados.current.has(iso),
    )
    if (dias.length === 0) return
    let ativo = true
    const fila = [...dias]
    async function trabalhador() {
      while (ativo && fila.length > 0) {
        const iso = fila.shift() as string
        try {
          const r = await espacosService.disponibilidade(Number(espacoId), iso)
          if (ativo) {
            jaBuscados.current.add(iso)
            setLivresPorDia((atual) => ({ ...atual, [iso]: r.blocos.filter((b) => b.situacao === 'LIVRE').length }))
          }
        } catch {
          // dia sem informação: o calendário simplesmente não marca
        }
      }
    }
    void Promise.all([trabalhador(), trabalhador(), trabalhador(), trabalhador()])
    return () => { ativo = false }
  }, [espacoId, mesVisivel, versao, hoje, maxData])

  // RF-AS04: elegível se tiver menos de 21 anos na DATA DE USO
  const dependentesElegiveis = useMemo(
    () => dependentes.filter((v) => idadeEm(v.dependente.dataNascimento, data) < IDADE_LIMITE_DEPENDENTE),
    [dependentes, data],
  )
  const dependenteValido = dependentesElegiveis.some((v) => String(v.id) === dependenteId)

  const reservasAtivas = minhas.filter((r) => r.status === 'CONFIRMADA')
  const mesAtual = hoje.slice(0, 7)
  const usadasNoMes = reservasAtivas.filter((r) => r.dataUso.startsWith(mesAtual)).length
  const limite = associado?.plano.limiteMensalReservas

  const ordenar = (a: Reserva, b: Reserva) => `${a.dataUso} ${a.horaInicio}`.localeCompare(`${b.dataUso} ${b.horaInicio}`)
  const proximas = minhas.filter((r) => r.status === 'CONFIRMADA' && r.dataUso >= hoje).sort(ordenar)
  const historico = minhas.filter((r) => !(r.status === 'CONFIRMADA' && r.dataUso >= hoje)).sort((a, b) => ordenar(b, a))
  const blocosEscolhidos = blocos.filter((b) => selecionados.includes(b.blocoId)).sort((a, b) => a.horaInicio.localeCompare(b.horaInicio))


  const marcas = useMemo(() => {
    const m: Record<string, MarcaDia> = {}
    for (const r of minhas) {
      if (r.status !== 'CONFIRMADA') continue
      m[r.dataUso] = { ...m[r.dataUso], minha: true }
    }
    if (espacoId && totalLivres > 0) {
      for (const [iso, livres] of Object.entries(livresPorDia)) {
        const minhasNoEspaco = minhas.filter((r) => r.status === 'CONFIRMADA' && r.dataUso === iso && String(r.espacoId) === espacoId).length
        const porOutros = Math.max(0, totalLivres - livres - minhasNoEspaco)
        if (porOutros > 0) m[iso] = { ...m[iso], outros: livres === 0 ? 'lotado' : 'parcial' }
      }
    }
    return m
  }, [minhas, espacoId, totalLivres, livresPorDia])

  function alternar(id: number) {
    setSelecionados((atual) => (atual.includes(id) ? atual.filter((x) => x !== id) : [...atual, id]))
  }

  async function reservar() {
    if (!espaco || selecionados.length === 0) return
    setEnviando(true)
    setErro('')
    try {
      const criadas = await espacosService.solicitarReserva({
        espacoId: espaco.id,
        data,
        blocoIds: selecionados,
        qtdConvidados: convidados,
        dependenteId: dependenteValido ? Number(dependenteId) : null,
      })
      setConfirmadas(criadas)
      toast.success('Reserva confirmada!')
      await Promise.all([carregarDisponibilidade(), carregarMinhas()])
      setVersao((v) => v + 1)
    } catch (e) {
      setErro(getApiErrorMessage(e, 'Não foi possível concluir a reserva.'))
      void carregarDisponibilidade()
    } finally {
      setEnviando(false)
    }
  }

  const temCota = limite !== undefined && limite !== null && limite > 0
  const cotaCheia = temCota && usadasNoMes >= (limite as number)
  const horariosEscolhidos = blocosEscolhidos.map((b) => `${b.horaInicio}–${b.horaFim}`).join(', ')
  const rotuloBotao = enviando
    ? 'Reservando…'
    : !espaco
      ? 'Escolha um espaço'
      : selecionados.length === 0
        ? 'Escolha um horário'
        : `Confirmar reserva${selecionados.length > 1 ? ` (${selecionados.length} blocos)` : ''}`
  const botaoDesabilitado = enviando || !espaco || selecionados.length === 0

  function cartaoReserva(r: Reserva, passada: boolean) {
    const d = partesDaData(r.dataUso)
    const cancelavel = r.status === 'CONFIRMADA' && !passada
    const classeStatus = r.status === 'CONFIRMADA' ? 'esp-res' : 'esp-res esp-res--cancelada'
    return (
      <li key={r.id} className={`${classeStatus} ${passada ? 'esp-res--passada' : ''}`}>
        <div className="esp-res-data" aria-hidden="true">
          <span>{d.semana}</span>
          <strong>{d.dia}</strong>
          <span>{d.mes}</span>
        </div>
        <div className="esp-res-info">
          <strong>{r.espacoNome}</strong>
          <div className="esp-res-linha">
            <span className="esp-meta"><Clock size={14} /> {r.horaInicio} às {r.horaFim}</span>
            <span className="esp-meta"><Users size={14} /> {rotuloConvidados(r.qtdConvidados)}</span>
            {r.dependenteNome && <span className="esp-meta"><UserPlus size={14} /> {r.dependenteNome}</span>}
          </div>
        </div>
        <div className="esp-res-lado">
          <span className={`esp-badge ${r.status === 'CONFIRMADA' ? 'esp-badge--ok' : 'esp-badge--off'}`}>
            {STATUS_RESERVA_LABEL[r.status]}
          </span>
          {cancelavel && podeCancelar(r.dataUso, hoje) && (
            <button type="button" className="esp-btn esp-btn--ghost esp-btn--sm" onClick={() => setParaCancelar(r)}>
              Cancelar
            </button>
          )}
          {cancelavel && !podeCancelar(r.dataUso, hoje) && (
            <span className="esp-res-aviso">Só dá para cancelar até o dia anterior</span>
          )}
        </div>
      </li>
    )
  }

  return (
    <div className="esp-page">
      <header className="esp-hero">
        <div className="esp-hero-main">
          <span className="esp-eyebrow"><CalendarDays size={13} /> Espaços do clube</span>
          <h1 className="esp-title">Reservar espaço</h1>
          <p className="esp-subtitle">Blocos de 2 horas, com até {MAX_DIAS_ANTECEDENCIA} dias de antecedência.</p>
        </div>

        {temCota && (
          <div className="esp-quota">
            <div className="esp-quota-top">
              <span className="esp-quota-num">{usadasNoMes}<small> / {limite}</small></span>
              <span className="esp-quota-label">reservas neste mês</span>
            </div>
            <div className="esp-quota-bar" role="img" aria-label={`${usadasNoMes} de ${limite} reservas usadas`}>
              {(limite as number) <= 12 ? (
                Array.from({ length: limite as number }, (_, i) => (
                  <span
                    key={i}
                    className={`esp-quota-seg ${i < usadasNoMes ? (cotaCheia ? 'esp-quota-seg--cheia' : 'esp-quota-seg--usada') : ''}`}
                  />
                ))
              ) : (
                <span className="esp-quota-seg" style={{ background: `linear-gradient(90deg, #2BB673 ${Math.min(100, (usadasNoMes / (limite as number)) * 100)}%, rgba(255,255,255,0.18) 0)` }} />
              )}
            </div>
            <p className="esp-quota-msg">
              {cotaCheia ? 'Você atingiu a cota do plano este mês.' : `Ainda restam ${(limite as number) - usadasNoMes} no seu plano.`}
            </p>
          </div>
        )}
      </header>

      {erroCarga && (
        <p className="esp-alert esp-alert--erro" role="alert"><AlertCircle size={16} />{erroCarga}</p>
      )}

      <div className="esp-tabs" role="tablist">
        <button type="button" role="tab" aria-selected={aba === 'nova'} className={`esp-tab ${aba === 'nova' ? 'esp-tab--ativa' : ''}`} onClick={() => setAba('nova')}>
          <Plus size={16} /> Nova reserva
        </button>
        <button type="button" role="tab" aria-selected={aba === 'minhas'} className={`esp-tab ${aba === 'minhas' ? 'esp-tab--ativa' : ''}`} onClick={() => setAba('minhas')}>
          Minhas reservas
          {proximas.length > 0 && <span className="esp-tab-count">{proximas.length}</span>}
        </button>
      </div>

      {aba === 'nova' && (
        <div className="esp-nova">
          <div className="esp-nova-main">
            <section className="esp-section" aria-labelledby="esp-sec-espaco">
              <div className="esp-section-head">
                <span className={`esp-step ${espaco ? 'esp-step--ok' : ''}`} aria-hidden="true">{espaco ? <Check size={16} /> : 1}</span>
                <div>
                  <h2 id="esp-sec-espaco">Escolha o espaço</h2>
                  <p>Toque no espaço que você quer reservar.</p>
                </div>
              </div>
              {espacos.length === 0 ? (
                <p className="esp-alert esp-alert--info"><Info size={16} />Nenhum espaço disponível para reserva no momento.</p>
              ) : (
                <div className="esp-spaces">
                  {espacos.map((e) => {
                    const indisponivel = e.estado !== 'ATIVO'
                    const selecionado = String(e.id) === espacoId
                    const Icone = iconeDoEspaco(e.nome)
                    return (
                      <button
                        key={e.id}
                        type="button"
                        className={`esp-space ${selecionado ? 'esp-space--sel' : ''}`}
                        aria-pressed={selecionado}
                        disabled={indisponivel}
                        onClick={() => { setEspacoId(String(e.id)); setConfirmadas([]) }}
                      >
                        {selecionado && <span className="esp-space-check" aria-hidden="true"><Check size={14} /></span>}
                        <span className="esp-space-topo">
                          <span className="esp-space-icone" aria-hidden="true"><Icone size={22} /></span>
                          <span className="esp-space-nome">{e.nome}</span>
                        </span>
                        <span className="esp-space-tags">
                          {indisponivel && <span className="esp-tag esp-tag--treino">Indisponível</span>}
                          {e.capacidadeMaxima !== null && <span className="esp-tag"><Users size={12} /> Até {e.capacidadeMaxima} pessoas</span>}
                          <span className="esp-tag"><UserPlus size={12} /> {e.limiteConvidados === 0 ? 'Sem convidados' : `${e.limiteConvidados} convidados`}</span>
                          {e.usoExclusivoTreino && <span className="esp-tag esp-tag--treino">Treino</span>}
                        </span>
                      </button>
                    )
                  })}
                </div>
              )}
            </section>

            <section className="esp-section" aria-labelledby="esp-sec-data">
              <div className="esp-section-head">
                <span className={`esp-step ${selecionados.length > 0 ? 'esp-step--ok' : ''}`} aria-hidden="true">{selecionados.length > 0 ? <Check size={16} /> : 2}</span>
                <div>
                  <h2 id="esp-sec-data">Escolha o dia e o horário</h2>
                  <p>
                    Reservas abertas até {formatarDataComDia(maxData)} ({MAX_DIAS_ANTECEDENCIA} dias de antecedência).
                    Os indicadores de horários ocupados valem para o espaço escolhido.
                  </p>
                </div>
              </div>
              <div className="esp-diahora">
                <CalendarioReservas
                  mes={mesVisivel}
                  onMesChange={setMesVisivel}
                  selecionado={data}
                  onSelecionar={(iso) => { setData(iso); setConfirmadas([]) }}
                  hoje={hoje}
                  maxData={maxData}
                  marcas={marcas}
                />
                <div className="esp-diahora-horas">
                  <h3 className="esp-subtitulo">Horários em {formatarDataComDia(data)}</h3>
                  {!espaco ? (
                    <p className="esp-alert esp-alert--info"><Info size={16} />Escolha um espaço para ver os horários livres.</p>
                  ) : carregandoBlocos ? (
                    <div className="esp-slots" aria-busy="true" aria-label="Carregando horários">
                      {Array.from({ length: 6 }, (_, i) => <span key={i} className="esp-slots-skeleton" />)}
                    </div>
                  ) : blocos.length === 0 ? (
                    <p className="esp-alert esp-alert--info"><Info size={16} />{mensagem ?? 'Nenhum horário livre neste dia. Tente outro dia.'}</p>
                  ) : (
                    <>
                      <p className="esp-muted">Cada bloco tem 2 horas. Você pode marcar mais de um.</p>
                      <div className="esp-slots">
                        {blocos.map((b) => {
                          const marcado = selecionados.includes(b.blocoId)
                          return (
                            <button
                              key={b.blocoId}
                              type="button"
                              className={`esp-slot esp-slot--${SITUACAO_CLASSE[b.situacao]} ${marcado ? 'esp-slot--sel' : ''}`}
                              aria-pressed={marcado}
                              onClick={() => alternar(b.blocoId)}
                              disabled={b.situacao !== 'LIVRE'}
                            >
                              <span className="esp-slot-hora">{b.horaInicio} – {b.horaFim}</span>
                              <span className="esp-slot-estado">{marcado ? 'Selecionado' : SITUACAO_LABEL[b.situacao]}</span>
                              {marcado && <Check className="esp-slot-check" size={16} aria-hidden="true" />}
                            </button>
                          )
                        })}
                      </div>
                    </>
                  )}
                </div>
              </div>
            </section>
          </div>

          <aside className="esp-resumo" aria-label="Resumo da reserva">
            <h2>Sua reserva</h2>
            <dl className="esp-resumo-linhas">
              <div>
                <dt><MapPin size={14} /> Espaço</dt>
                <dd className={espaco ? '' : 'esp-vazio'}>{espaco ? espaco.nome : 'Ainda não escolhido'}</dd>
              </div>
              <div>
                <dt><CalendarDays size={14} /> Dia</dt>
                <dd>{formatarDataComDia(data)}</dd>
              </div>
              <div>
                <dt><Clock size={14} /> Horários</dt>
                <dd className={blocosEscolhidos.length ? '' : 'esp-vazio'}>
                  {blocosEscolhidos.length ? horariosEscolhidos : 'Ainda não escolhidos'}
                </dd>
              </div>
            </dl>

            {espaco && (
              <>
                <div className="esp-field">
                  Convidados (máx. {espaco.limiteConvidados})
                  <div className="esp-stepper">
                    <button type="button" aria-label="Menos um convidado" disabled={convidados <= 0} onClick={() => setConvidados((n) => Math.max(0, n - 1))}><Minus size={16} /></button>
                    <output aria-live="polite">{convidados}</output>
                    <button type="button" aria-label="Mais um convidado" disabled={convidados >= espaco.limiteConvidados} onClick={() => setConvidados((n) => Math.min(espaco.limiteConvidados, n + 1))}><Plus size={16} /></button>
                  </div>
                </div>
                {dependentesElegiveis.length > 0 && (
                  <label className="esp-field">
                    Dependente beneficiário (opcional)
                    <select value={dependenteValido ? dependenteId : ''} onChange={(e) => setDependenteId(e.target.value)}>
                      <option value="">Nenhum</option>
                      {dependentesElegiveis.map((v) => (
                        <option key={v.id} value={v.id}>{v.dependente.nome}</option>
                      ))}
                    </select>
                  </label>
                )}
                {espaco.capacidadeMaxima !== null && !espaco.publico && (
                  <p className="esp-muted">
                    Capacidade: {espaco.capacidadeMaxima} pessoas. Você, o dependente e os convidados contam.
                  </p>
                )}
              </>
            )}

            {erro && <p className="esp-alert esp-alert--erro" role="alert"><AlertCircle size={16} />{erro}</p>}

            <button type="button" className="esp-btn esp-btn--primary esp-resumo-btn" disabled={botaoDesabilitado} onClick={reservar}>
              {rotuloBotao}
            </button>

            {confirmadas.length > 0 && (
              <div className="esp-alert esp-alert--ok" role="status">
                <CheckCircle2 size={16} />
                <span>
                  <strong>Reserva confirmada:</strong> {confirmadas[0].espacoNome}, {formatarDataComDia(confirmadas[0].dataUso)}, {confirmadas.map((r) => `${r.horaInicio}–${r.horaFim}`).join(', ')}.{' '}
                  <button type="button" className="esp-link" onClick={() => setAba('minhas')}>Ver minhas reservas</button>
                </span>
              </div>
            )}
          </aside>

          {/* Celular/tablet: a confirmação acompanha a rolagem */}
          <div className="esp-mobilebar">
            <div className="esp-mobilebar-info">
              <strong>{selecionados.length > 0 ? horariosEscolhidos : espaco ? 'Escolha um horário' : 'Escolha um espaço'}</strong>
              <span>{espaco ? `${espaco.nome} · ${formatarDataComDia(data)}` : 'Siga os passos acima'}</span>
            </div>
            <button type="button" className="esp-btn esp-btn--primary" disabled={botaoDesabilitado} onClick={reservar}>
              {enviando ? 'Reservando…' : 'Confirmar'}
            </button>
          </div>
        </div>
      )}

      {aba === 'minhas' && (
        <div className="esp-section">
          {minhas.length === 0 ? (
            <div className="esp-vazio-cta">
              <span className="esp-vazio-icone"><CalendarDays size={26} /></span>
              <strong>Nenhuma reserva por aqui ainda</strong>
              <p>Escolha um espaço, um dia e um horário livre. Leva menos de um minuto.</p>
              <button type="button" className="esp-btn esp-btn--primary" onClick={() => setAba('nova')}>Fazer uma reserva</button>
            </div>
          ) : (
            <>
              <div className="esp-grupo">
                <h2 className="esp-grupo-titulo">Próximas <small>{proximas.length}</small></h2>
                {proximas.length === 0 ? (
                  <p className="esp-muted">Nenhuma reserva futura. <button type="button" className="esp-link" onClick={() => setAba('nova')}>Reservar agora</button></p>
                ) : (
                  <ul className="esp-list">{proximas.map((r) => cartaoReserva(r, false))}</ul>
                )}
              </div>
              {historico.length > 0 && (
                <div className="esp-grupo">
                  <h2 className="esp-grupo-titulo">Anteriores e canceladas <small>{historico.length}</small></h2>
                  <ul className="esp-list">{historico.map((r) => cartaoReserva(r, true))}</ul>
                </div>
              )}
            </>
          )}
        </div>
      )}

      <CancelarReservaModal
        reserva={paraCancelar}
        onClose={() => setParaCancelar(null)}
        onCancelada={() => { void carregarMinhas(); void carregarDisponibilidade(); setVersao((v) => v + 1) }}
      />
    </div>
  )
}
