import { useCallback, useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useParams } from 'react-router-dom'
import Aviso from '../components/Aviso'
import Badge, { varianteSituacao } from '../components/Badge'
import DependenteFormModal from '../components/DependenteFormModal'
import { associadosApi, dependentesApi, planosApi } from '../services/associadosApi'
import type { Associado, Plano, SituacaoCadastral, StatusVinculo, TipoPlano, VinculoDependencia } from '../types'
import {
  CATEGORIA_LABEL,
  MOTIVO_LABEL,
  SITUACAO_LABEL,
  STATUS_VINCULO_LABEL,
  TIPO_PLANO_LABEL,
  formatarCpf,
  formatarData,
  formatarMoeda,
  hojeISO,
  idadeEm,
  mensagemDeErro,
} from '../utils'

interface Transicao {
  alvo: SituacaoCadastral
  rotulo: string
  confirmacao?: string
  perigo?: boolean
}

// RF-AS06: transições de situação cadastral permitidas pela API
const TRANSICOES: Record<SituacaoCadastral, Transicao[]> = {
  ATIVO: [
    {
      alvo: 'SUSPENSO',
      rotulo: 'Suspender',
      confirmacao:
        'Suspender o titular impede o uso das instalações por ele e por seus dependentes. Os vínculos de dependência não são inativados. Continuar?',
    },
    {
      alvo: 'INATIVO',
      rotulo: 'Inativar',
      perigo: true,
      confirmacao:
        'Inativar o titular inativa automaticamente todos os vínculos de dependência ativos. O histórico é mantido. Continuar?',
    },
  ],
  SUSPENSO: [
    { alvo: 'ATIVO', rotulo: 'Reativar' },
    {
      alvo: 'INATIVO',
      rotulo: 'Inativar',
      perigo: true,
      confirmacao:
        'Inativar o titular inativa automaticamente todos os vínculos de dependência ativos. O histórico é mantido. Continuar?',
    },
  ],
  INATIVO: [
    {
      alvo: 'ATIVO',
      rotulo: 'Reativar',
      confirmacao:
        'Ao reativar, voltam somente os vínculos inativados pela inativação do titular, desde que o dependente tenha menos de 21 anos e o limite do plano permita. Continuar?',
    },
  ],
}

export default function AssociadoDetalhePage() {
  const { id } = useParams()
  const associadoId = Number(id)

  const [associado, setAssociado] = useState<Associado | null>(null)
  const [vinculos, setVinculos] = useState<VinculoDependencia[]>([])
  const [planos, setPlanos] = useState<Plano[]>([])
  const [filtroStatus, setFiltroStatus] = useState<StatusVinculo | ''>('')
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState<string | null>(null)
  const [aviso, setAviso] = useState<string | null>(null)
  const [ocupado, setOcupado] = useState(false)

  const [editandoDados, setEditandoDados] = useState(false)
  const [nome, setNome] = useState('')
  const [dataNascimento, setDataNascimento] = useState('')
  const [planoSelecionado, setPlanoSelecionado] = useState<TipoPlano | ''>('')

  const [modalDependente, setModalDependente] = useState<{ aberto: boolean; vinculo?: VinculoDependencia }>({
    aberto: false,
  })

  const carregar = useCallback(async () => {
    try {
      const [dadosAssociado, dadosVinculos, dadosPlanos] = await Promise.all([
        associadosApi.buscarPorId(associadoId),
        dependentesApi.listarPorTitular(associadoId),
        planosApi.listar(),
      ])
      setAssociado(dadosAssociado)
      setVinculos(dadosVinculos)
      setPlanos(dadosPlanos)
      setPlanoSelecionado(dadosAssociado.plano.tipo)
      setNome(dadosAssociado.pessoa.nome)
      setDataNascimento(dadosAssociado.pessoa.dataNascimento)
    } catch (e) {
      setErro(mensagemDeErro(e))
    }
  }, [associadoId])

  useEffect(() => {
    if (!Number.isInteger(associadoId)) {
      setErro('Associado inválido.')
      setCarregando(false)
      return
    }
    setCarregando(true)
    void carregar().finally(() => setCarregando(false))
  }, [associadoId, carregar])

  // Executa uma ação na API, recarrega os dados e mostra o resultado
  const executar = async (acao: () => Promise<unknown>, sucesso: string): Promise<boolean> => {
    setErro(null)
    setAviso(null)
    setOcupado(true)
    try {
      await acao()
      await carregar()
      setAviso(sucesso)
      return true
    } catch (e) {
      setErro(mensagemDeErro(e))
      return false
    } finally {
      setOcupado(false)
    }
  }

  const salvarDados = async (e: FormEvent) => {
    e.preventDefault()
    const ok = await executar(
      () => associadosApi.atualizarDadosPessoais(associadoId, { nome: nome.trim(), dataNascimento }),
      'Dados atualizados.',
    )
    if (ok) setEditandoDados(false)
  }

  const trocarPlano = () => {
    if (!planoSelecionado) return
    void executar(() => associadosApi.alterarPlano(associadoId, planoSelecionado), 'Plano alterado.')
  }

  const mudarSituacao = (t: Transicao) => {
    if (t.confirmacao && !window.confirm(t.confirmacao)) return
    void executar(() => associadosApi.alterarSituacao(associadoId, t.alvo), `Situação alterada para ${SITUACAO_LABEL[t.alvo]}.`)
  }

  const inativarVinculo = (v: VinculoDependencia) => {
    if (!window.confirm(`Inativar o vínculo de ${v.dependente.nome}? O registro histórico é mantido.`)) return
    void executar(() => dependentesApi.inativar(v.id), 'Vínculo de dependência inativado.')
  }

  if (carregando) {
    return <p className="as-vazio">Carregando…</p>
  }

  if (!associado) {
    return (
      <section>
        <Aviso tipo="erro" mensagem={erro ?? 'Associado não encontrado.'} />
        <Link to="/associados">← Voltar para a lista</Link>
      </section>
    )
  }

  const ativos = vinculos.filter((v) => v.status === 'ATIVO').length
  const maximo = associado.plano.maxDependentesAtivos
  const titularInativo = associado.situacaoCadastral === 'INATIVO'
  const limiteAtingido = ativos >= maximo
  const novoBloqueado = titularInativo || limiteAtingido
  const motivoBloqueio = titularInativo
    ? 'Titular inativo: reative-o para cadastrar dependentes.'
    : limiteAtingido
      ? `Limite do plano atingido (${maximo}).`
      : undefined

  const vinculosExibidos = vinculos.filter((v) => !filtroStatus || v.status === filtroStatus)

  return (
    <section>
      <Link to="/associados" className="as-voltar">
        ← Associados
      </Link>

      <div className="as-titulo-linha">
        <h1>
          {associado.pessoa.nome}{' '}
          <Badge texto={SITUACAO_LABEL[associado.situacaoCadastral]} variante={varianteSituacao(associado.situacaoCadastral)} />
        </h1>
      </div>

      <Aviso tipo="erro" mensagem={erro} onFechar={() => setErro(null)} />
      <Aviso tipo="sucesso" mensagem={aviso} onFechar={() => setAviso(null)} />

      <div className="as-grid">
        {/* Dados pessoais — RF-AS05 */}
        <div className="as-card">
          <div className="as-card-titulo">
            <h2>Dados pessoais</h2>
            {!editandoDados && (
              <button type="button" className="as-btn as-btn-link" onClick={() => setEditandoDados(true)}>
                Editar
              </button>
            )}
          </div>
          {editandoDados ? (
            <form onSubmit={salvarDados} className="as-form">
              <label>
                Nome completo
                <input value={nome} onChange={(e) => setNome(e.target.value)} required />
              </label>
              <label>
                Data de nascimento
                <input
                  type="date"
                  value={dataNascimento}
                  max={hojeISO()}
                  onChange={(e) => setDataNascimento(e.target.value)}
                  required
                />
              </label>
              <div className="as-form-actions">
                <button
                  type="button"
                  className="as-btn as-btn-secundario"
                  onClick={() => {
                    setEditandoDados(false)
                    setNome(associado.pessoa.nome)
                    setDataNascimento(associado.pessoa.dataNascimento)
                  }}
                >
                  Cancelar
                </button>
                <button type="submit" className="as-btn as-btn-primario" disabled={ocupado}>
                  Salvar
                </button>
              </div>
            </form>
          ) : (
            <dl className="as-dl">
              <dt>CPF</dt>
              <dd>{formatarCpf(associado.pessoa.cpf)}</dd>
              <dt>Nascimento</dt>
              <dd>
                {formatarData(associado.pessoa.dataNascimento)} ({idadeEm(associado.pessoa.dataNascimento)} anos)
              </dd>
              <dt>Admissão</dt>
              <dd>{formatarData(associado.dataAdmissao)}</dd>
            </dl>
          )}
        </div>

        {/* Plano — RF-AS02 */}
        <div className="as-card">
          <div className="as-card-titulo">
            <h2>Plano</h2>
          </div>
          <dl className="as-dl">
            <dt>Categoria</dt>
            <dd>{CATEGORIA_LABEL[associado.categoria]}</dd>
            <dt>Plano atual</dt>
            <dd>{TIPO_PLANO_LABEL[associado.plano.tipo]}</dd>
            <dt>Valor mensal</dt>
            <dd>{formatarMoeda(associado.plano.valorPeriodico)}</dd>
            <dt>Reservas/mês</dt>
            <dd>{associado.plano.limiteMensalReservas}</dd>
            <dt>Dependentes</dt>
            <dd>
              {ativos} ativo(s) de {maximo}
            </dd>
          </dl>
          <div className="as-inline">
            <select value={planoSelecionado} onChange={(e) => setPlanoSelecionado(e.target.value as TipoPlano)}>
              {planos.map((p) => (
                <option key={p.id} value={p.tipo}>
                  {TIPO_PLANO_LABEL[p.tipo]} — {formatarMoeda(p.valorPeriodico)}
                </option>
              ))}
            </select>
            <button
              type="button"
              className="as-btn as-btn-secundario"
              onClick={trocarPlano}
              disabled={ocupado || planoSelecionado === associado.plano.tipo}
            >
              Alterar plano
            </button>
          </div>
          <p className="as-hint">A categoria acompanha o plano escolhido.</p>
        </div>

        {/* Situação cadastral — RF-AS06 */}
        <div className="as-card">
          <div className="as-card-titulo">
            <h2>Situação cadastral</h2>
          </div>
          <p>
            Atual: <strong>{SITUACAO_LABEL[associado.situacaoCadastral]}</strong>
          </p>
          <div className="as-acoes">
            {TRANSICOES[associado.situacaoCadastral].map((t) => (
              <button
                key={t.alvo}
                type="button"
                className={`as-btn ${t.perigo ? 'as-btn-perigo' : 'as-btn-secundario'}`}
                onClick={() => mudarSituacao(t)}
                disabled={ocupado}
              >
                {t.rotulo}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* Dependentes — RF-AS03/AS04 */}
      <div className="as-card as-card-largo">
        <div className="as-card-titulo">
          <h2>
            Dependentes{' '}
            <span className="as-contador">
              ({ativos}/{maximo} ativos)
            </span>
          </h2>
          <div className="as-acoes">
            <select value={filtroStatus} onChange={(e) => setFiltroStatus(e.target.value as StatusVinculo | '')}>
              <option value="">Todos</option>
              <option value="ATIVO">Ativos</option>
              <option value="INATIVO">Inativos</option>
            </select>
            <button
              type="button"
              className="as-btn as-btn-primario"
              onClick={() => setModalDependente({ aberto: true })}
              disabled={novoBloqueado}
              title={motivoBloqueio}
            >
              + Novo dependente
            </button>
          </div>
        </div>
        {motivoBloqueio && <p className="as-hint">{motivoBloqueio}</p>}

        <div className="as-tabela-wrap">
          <table className="as-tabela">
            <thead>
              <tr>
                <th>Nome</th>
                <th>CPF</th>
                <th>Nascimento</th>
                <th>Parentesco</th>
                <th>Início</th>
                <th>Fim</th>
                <th>Status</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {vinculosExibidos.length === 0 && (
                <tr>
                  <td colSpan={8} className="as-vazio">
                    Nenhum dependente.
                  </td>
                </tr>
              )}
              {vinculosExibidos.map((v) => (
                <tr key={v.id}>
                  <td>{v.dependente.nome}</td>
                  <td>{formatarCpf(v.dependente.cpf)}</td>
                  <td>
                    {formatarData(v.dependente.dataNascimento)} ({idadeEm(v.dependente.dataNascimento)})
                  </td>
                  <td>{v.parentesco}</td>
                  <td>{formatarData(v.dataInicio)}</td>
                  <td>{formatarData(v.dataFim)}</td>
                  <td>
                    <Badge
                      texto={STATUS_VINCULO_LABEL[v.status]}
                      variante={v.status === 'ATIVO' ? 'verde' : 'cinza'}
                    />
                    {v.motivoInativacao && <div className="as-hint">{MOTIVO_LABEL[v.motivoInativacao]}</div>}
                  </td>
                  <td className="as-acoes-celula">
                    <button
                      type="button"
                      className="as-btn as-btn-link"
                      onClick={() => setModalDependente({ aberto: true, vinculo: v })}
                    >
                      Editar
                    </button>
                    {v.status === 'ATIVO' && (
                      <button
                        type="button"
                        className="as-btn as-btn-link as-btn-link-perigo"
                        onClick={() => inativarVinculo(v)}
                        disabled={ocupado}
                      >
                        Inativar
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {modalDependente.aberto && (
        <DependenteFormModal
          titularId={associadoId}
          vinculo={modalDependente.vinculo}
          onFechar={() => setModalDependente({ aberto: false })}
          onSalvo={() => {
            const edicao = modalDependente.vinculo !== undefined
            setModalDependente({ aberto: false })
            void carregar().then(() => setAviso(edicao ? 'Dependente atualizado.' : 'Dependente cadastrado.'))
          }}
        />
      )}
    </section>
  )
}
