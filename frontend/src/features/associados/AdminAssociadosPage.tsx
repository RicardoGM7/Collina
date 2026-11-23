import {
  AlertCircle,
  AlertTriangle,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  Edit2,
  Eye,
  EyeOff,
  Plus,
  RefreshCw,
  Search,
  Shield,
  UserCheck,
  UserCog,
  UserPlus,
  Users,
  UserX,
} from 'lucide-react'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { Modal } from '../../components/Modal/Modal'
import { useAuth } from '../../context/AuthContext'
import { associadoService } from '../../services/associadoService'
import { UsuariosAdminPanel } from '../auth/UsuariosAdminPanel'
import {
  CATEGORIA_DO_PLANO,
  CATEGORIA_LABEL,
  SITUACAO_LABEL,
  nomePlano,
  type Associado,
  type CategoriaAssociado,
  type Plano,
  type SituacaoCadastral,
  type TipoPlano,
  type VinculoDependencia,
} from '../../types/associado'
import {
  calcularIdade,
  formatarCpf,
  formatarDataBr,
  formatarMoeda,
  mensagemErroApi,
  validarCpf,
} from './associadoUtils'
import './AdminAssociadosPage.css'

export function AdminAssociadosPage() {
  const { usuario } = useAuth()
  const ehAdmin = usuario?.perfil === 'ADMIN'

  const [abaAtiva, setAbaAtiva] = useState<
    'lista' | 'cadastro' | 'planos' | 'usuarios'
  >('lista')

  // Dados reais vindos do back-end
  const [associados, setAssociados] = useState<Associado[]>([])
  const [planos, setPlanos] = useState<Plano[]>([])
  const [carregando, setCarregando] = useState(true)
  const [erroGeral, setErroGeral] = useState('')
  const [feedbackGeral, setFeedbackGeral] = useState('')

  // Filtros da listagem
  const [filtroSituacao, setFiltroSituacao] = useState<string>('TODOS')
  const [filtroCategoria, setFiltroCategoria] = useState<string>('TODAS')
  const [busca, setBusca] = useState('')

  // Formulário de Cadastro de Titular (+ conta de acesso MVP)
  const [formNome, setFormNome] = useState('')
  const [formCpf, setFormCpf] = useState('')
  const [formDataNasc, setFormDataNasc] = useState('')
  const [formEmail, setFormEmail] = useState('')
  const [formSenha, setFormSenha] = useState('')
  const [mostrarSenha, setMostrarSenha] = useState(false)
  const [formCategoria, setFormCategoria] =
    useState<CategoriaAssociado>('CONTRIBUINTE')
  const [formTipoPlano, setFormTipoPlano] = useState<TipoPlano>('BASICO')
  const [submittingCadastro, setSubmittingCadastro] = useState(false)
  const [mensagemSucesso, setMensagemSucesso] = useState('')
  const [mensagemErro, setMensagemErro] = useState('')

  // Modal Alterar Situação
  const [associadoParaSituacao, setAssociadoParaSituacao] =
    useState<Associado | null>(null)
  const [novaSituacaoSelecionada, setNovaSituacaoSelecionada] =
    useState<SituacaoCadastral>('ATIVO')
  const [erroModalSituacao, setErroModalSituacao] = useState('')
  const [salvandoSituacao, setSalvandoSituacao] = useState(false)

  // Modal Gestão de Dependentes
  const [titularSelecionado, setTitularSelecionado] =
    useState<Associado | null>(null)
  const [dependentesTitular, setDependentesTitular] = useState<
    VinculoDependencia[]
  >([])
  const [carregandoDeps, setCarregandoDeps] = useState(false)
  const [depNome, setDepNome] = useState('')
  const [depCpf, setDepCpf] = useState('')
  const [depDataNasc, setDepDataNasc] = useState('')
  const [depParentesco, setDepParentesco] = useState('Filho(a)')
  const [salvandoDep, setSalvandoDep] = useState(false)
  const [depErro, setDepErro] = useState('')
  const [depSucesso, setDepSucesso] = useState('')

  // Modal Editar Parâmetros do Plano (somente ADMIN)
  const [planoEditando, setPlanoEditando] = useState<Plano | null>(null)
  const [planoValorEdit, setPlanoValorEdit] = useState('')
  const [planoReservasEdit, setPlanoReservasEdit] = useState('')
  const [planoDepsEdit, setPlanoDepsEdit] = useState('')
  const [salvandoPlanoParam, setSalvandoPlanoParam] = useState(false)
  const [erroPlanoParam, setErroPlanoParam] = useState('')

  // Rotina de inativação por idade
  const [executandoRotinaIdade, setExecutandoRotinaIdade] = useState(false)

  const carregarDados = useCallback(async () => {
    setCarregando(true)
    setErroGeral('')
    try {
      const [listaAssoc, listaPlanos] = await Promise.all([
        associadoService.listar(),
        associadoService.listarPlanos(),
      ])
      setAssociados(listaAssoc)
      setPlanos(listaPlanos)
    } catch (e) {
      setErroGeral(
        mensagemErroApi(e, 'Não foi possível carregar os dados do back-end.'),
      )
    } finally {
      setCarregando(false)
    }
  }, [])

  useEffect(() => {
    void carregarDados()
  }, [carregarDados])

  // Planos compatíveis com a categoria escolhida no formulário
  const planosDaCategoria = useMemo(
    () => planos.filter((p) => CATEGORIA_DO_PLANO[p.tipo] === formCategoria),
    [planos, formCategoria],
  )

  const planoSelecionadoForm = useMemo(
    () => planos.find((p) => p.tipo === formTipoPlano) ?? null,
    [planos, formTipoPlano],
  )

  const associadosFiltrados = useMemo(() => {
    const buscaLower = busca.trim().toLowerCase()
    const buscaDigitos = busca.replace(/\D/g, '')
    return associados.filter((a) => {
      const matchBusca =
        !buscaLower ||
        a.pessoa.nome.toLowerCase().includes(buscaLower) ||
        (buscaDigitos.length > 0 && a.pessoa.cpf.includes(buscaDigitos))
      const matchSit =
        filtroSituacao === 'TODOS' || a.situacaoCadastral === filtroSituacao
      const matchCat =
        filtroCategoria === 'TODAS' || a.categoria === filtroCategoria
      return matchBusca && matchSit && matchCat
    })
  }, [associados, busca, filtroSituacao, filtroCategoria])

  // Estatísticas reais
  const totalTitulares = associados.length
  const totalAtivos = associados.filter(
    (a) => a.situacaoCadastral === 'ATIVO',
  ).length
  const totalSuspensos = associados.filter(
    (a) => a.situacaoCadastral === 'SUSPENSO',
  ).length
  const totalInativos = associados.filter(
    (a) => a.situacaoCadastral === 'INATIVO',
  ).length

  async function abrirModalDependentes(assoc: Associado) {
    setTitularSelecionado(assoc)
    setDepErro('')
    setDepSucesso('')
    setDepNome('')
    setDepCpf('')
    setDepDataNasc('')
    setDepParentesco('Filho(a)')
    setCarregandoDeps(true)
    try {
      const lista = await associadoService.listarDependentes(assoc.id)
      setDependentesTitular(lista)
    } catch (e) {
      setDependentesTitular([])
      setDepErro(
        mensagemErroApi(e, 'Não foi possível carregar os dependentes.'),
      )
    } finally {
      setCarregandoDeps(false)
    }
  }

  async function handleCadastrarTitular(e: React.FormEvent) {
    e.preventDefault()
    setMensagemErro('')
    setMensagemSucesso('')

    if (!formNome.trim()) {
      setMensagemErro('O nome do associado é obrigatório.')
      return
    }
    const cleanCpf = formCpf.replace(/\D/g, '')
    if (cleanCpf.length !== 11 || !validarCpf(cleanCpf)) {
      setMensagemErro('Informe um CPF válido com 11 dígitos.')
      return
    }
    if (!formDataNasc) {
      setMensagemErro('A data de nascimento é obrigatória.')
      return
    }
    if (!formEmail.trim()) {
      setMensagemErro('O e-mail de acesso é obrigatório.')
      return
    }
    if (formSenha.length < 6) {
      setMensagemErro('A senha inicial deve ter pelo menos 6 caracteres.')
      return
    }

    setSubmittingCadastro(true)
    try {
      const novo = await associadoService.cadastrar({
        nome: formNome.trim(),
        cpf: cleanCpf,
        dataNascimento: formDataNasc,
        categoria: formCategoria,
        tipoPlano: formTipoPlano,
        email: formEmail.trim(),
        senha: formSenha,
      })

      setAssociados((prev) => [novo, ...prev])
      setMensagemSucesso(
        'Associado titular e conta de acesso cadastrados com sucesso!',
      )
      setFormNome('')
      setFormCpf('')
      setFormDataNasc('')
      setFormEmail('')
      setFormSenha('')
      setMostrarSenha(false)
      setTimeout(() => {
        setMensagemSucesso('')
        setAbaAtiva('lista')
      }, 1400)
    } catch (err) {
      setMensagemErro(mensagemErroApi(err, 'Erro ao cadastrar associado.'))
    } finally {
      setSubmittingCadastro(false)
    }
  }

  async function handleSalvarNovaSituacao() {
    if (!associadoParaSituacao) return
    setErroModalSituacao('')
    setSalvandoSituacao(true)
    try {
      const atualizado = await associadoService.alterarSituacao(
        associadoParaSituacao.id,
        novaSituacaoSelecionada,
      )
      setAssociados((prev) =>
        prev.map((a) => (a.id === atualizado.id ? atualizado : a)),
      )
      setAssociadoParaSituacao(null)
    } catch (err) {
      setErroModalSituacao(mensagemErroApi(err, 'Erro ao alterar a situação.'))
    } finally {
      setSalvandoSituacao(false)
    }
  }

  async function handleCadastrarDependente(e: React.FormEvent) {
    e.preventDefault()
    if (!titularSelecionado) return
    setDepErro('')
    setDepSucesso('')

    if (!depNome.trim()) {
      setDepErro('O nome do dependente é obrigatório.')
      return
    }
    const cleanCpf = depCpf.replace(/\D/g, '')
    if (cleanCpf.length !== 11 || !validarCpf(cleanCpf)) {
      setDepErro('CPF do dependente inválido.')
      return
    }
    if (!depDataNasc) {
      setDepErro('Data de nascimento é obrigatória.')
      return
    }

    setSalvandoDep(true)
    try {
      const vinculo = await associadoService.cadastrarDependente(
        titularSelecionado.id,
        {
          nome: depNome.trim(),
          cpf: cleanCpf,
          dataNascimento: depDataNasc,
          parentesco: depParentesco,
        },
      )
      setDependentesTitular((prev) => [...prev, vinculo])
      setDepSucesso('Dependente cadastrado com sucesso!')
      setDepNome('')
      setDepCpf('')
      setDepDataNasc('')
      setTimeout(() => setDepSucesso(''), 2500)
    } catch (err) {
      setDepErro(mensagemErroApi(err, 'Erro ao cadastrar dependente.'))
    } finally {
      setSalvandoDep(false)
    }
  }

  async function handleInativarDependente(vinculoId: number) {
    setDepErro('')
    try {
      const atualizado = await associadoService.inativarDependente(vinculoId)
      setDependentesTitular((prev) =>
        prev.map((d) => (d.id === atualizado.id ? atualizado : d)),
      )
    } catch (err) {
      setDepErro(mensagemErroApi(err, 'Erro ao inativar vínculo.'))
    }
  }

  async function handleExecutarRotinaIdade() {
    setExecutandoRotinaIdade(true)
    setErroGeral('')
    setFeedbackGeral('')
    try {
      const inativados = await associadoService.inativarDependentesPorIdade()
      setFeedbackGeral(
        `Rotina de idade limite (21 anos) concluída: ${inativados} vínculo(s) inativado(s) preservando o histórico.`,
      )
      setTimeout(() => setFeedbackGeral(''), 5000)
    } catch (err) {
      setErroGeral(
        mensagemErroApi(err, 'Erro ao executar a rotina de idade limite.'),
      )
    } finally {
      setExecutandoRotinaIdade(false)
    }
  }

  function abrirEdicaoPlano(plano: Plano) {
    setPlanoEditando(plano)
    setPlanoValorEdit(String(plano.valorPeriodico))
    setPlanoReservasEdit(String(plano.limiteMensalReservas))
    setPlanoDepsEdit(String(plano.maxDependentesAtivos))
    setErroPlanoParam('')
  }

  async function handleSalvarParametrosPlano(e: React.FormEvent) {
    e.preventDefault()
    if (!planoEditando) return
    setErroPlanoParam('')
    setSalvandoPlanoParam(true)
    try {
      const atualizado = await associadoService.atualizarPlano(
        planoEditando.id,
        {
          valorPeriodico: Number(planoValorEdit),
          limiteMensalReservas: Number(planoReservasEdit),
          maxDependentesAtivos: Number(planoDepsEdit),
        },
      )
      setPlanos((prev) =>
        prev.map((p) => (p.id === atualizado.id ? atualizado : p)),
      )
      setAssociados((prev) =>
        prev.map((a) =>
          a.plano.id === atualizado.id ? { ...a, plano: atualizado } : a,
        ),
      )
      setPlanoEditando(null)
    } catch (err) {
      setErroPlanoParam(
        mensagemErroApi(err, 'Erro ao atualizar parâmetros do plano.'),
      )
    } finally {
      setSalvandoPlanoParam(false)
    }
  }

  const rotaVoltar = ehAdmin ? '/admin' : '/recepcao'

  return (
    <div className="admin-associados-page">
      {/* Top Header & Breadcrumb */}
      <div className="admin-page-top">
        <div className="admin-breadcrumb">
          <Link to={rotaVoltar} className="admin-back-btn">
            <ChevronLeft size={14} color="#2BB673" strokeWidth={2.8} />
            <span>Voltar ao Início</span>
          </Link>
          <div className="admin-breadcrumb-text">
            <span>Visão geral</span>
            <ChevronRight size={14} color="#94A3B8" />
            <span className="crumb-curr">Cadastros</span>
          </div>
        </div>

        <div className="admin-header-actions">
          <button
            type="button"
            className="btn-rotina-idade"
            onClick={() => void handleExecutarRotinaIdade()}
            disabled={executandoRotinaIdade}
            title="Executa verificação de dependentes que completaram 21 anos"
          >
            <RefreshCw
              size={14}
              className={executandoRotinaIdade ? 'spin-icon' : ''}
            />
            <span>Rotina de Idade (21 anos)</span>
          </button>

          <button
            type="button"
            className="btn-novo-associado"
            onClick={() => setAbaAtiva('cadastro')}
          >
            <UserPlus size={16} />
            <span>Novo Titular</span>
          </button>
        </div>
      </div>

      <div className="admin-title-row">
        <div>
          <h1 className="admin-page-title">Gestão de Associados</h1>
          <p className="admin-page-subtitle">
            Administração de sócios titulares, dependentes e planos do clube
          </p>
        </div>
      </div>

      {feedbackGeral && (
        <div className="profile-alert success">
          <CheckCircle2 size={16} />
          <span>{feedbackGeral}</span>
        </div>
      )}

      {erroGeral && (
        <div className="profile-alert error">
          <AlertCircle size={16} />
          <span>{erroGeral}</span>
        </div>
      )}

      {/* Summary Stat Cards (dados reais) */}
      <div className="admin-stats-row">
        <div className="stat-card">
          <div className="stat-card-icon green">
            <Users size={20} />
          </div>
          <div className="stat-card-info">
            <span className="stat-card-val">{totalTitulares}</span>
            <span className="stat-card-label">Titulares Cadastrados</span>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-card-icon blue">
            <UserCheck size={20} />
          </div>
          <div className="stat-card-info">
            <span className="stat-card-val">{totalAtivos}</span>
            <span className="stat-card-label">Titulares Ativos</span>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-card-icon purple">
            <AlertTriangle size={20} />
          </div>
          <div className="stat-card-info">
            <span className="stat-card-val">{totalSuspensos}</span>
            <span className="stat-card-label">Suspensos</span>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-card-icon red">
            <UserX size={20} />
          </div>
          <div className="stat-card-info">
            <span className="stat-card-val">{totalInativos}</span>
            <span className="stat-card-label">Inativos</span>
          </div>
        </div>
      </div>

      {/* Tab Navigation */}
      <div className="admin-tabs-nav">
        <button
          type="button"
          className={`admin-nav-tab ${abaAtiva === 'lista' ? 'active' : ''}`}
          onClick={() => setAbaAtiva('lista')}
        >
          <Users size={15} />
          <span>Lista de Associados ({associados.length})</span>
        </button>

        <button
          type="button"
          className={`admin-nav-tab ${abaAtiva === 'cadastro' ? 'active' : ''}`}
          onClick={() => setAbaAtiva('cadastro')}
        >
          <UserPlus size={15} />
          <span>Cadastrar Titular</span>
        </button>

        <button
          type="button"
          className={`admin-nav-tab ${abaAtiva === 'planos' ? 'active' : ''}`}
          onClick={() => setAbaAtiva('planos')}
        >
          <Shield size={15} />
          <span>Planos do Clube ({planos.length})</span>
        </button>

        {ehAdmin && (
          <button
            type="button"
            className={`admin-nav-tab ${abaAtiva === 'usuarios' ? 'active' : ''}`}
            onClick={() => setAbaAtiva('usuarios')}
          >
            <UserCog size={15} />
            <span>Usuários e Perfis</span>
          </button>
        )}
      </div>

      {/* ABA 1: LISTA DE ASSOCIADOS */}
      {abaAtiva === 'lista' && (
        <div className="admin-content-card">
          <div className="admin-filters-bar">
            <div className="admin-search-input-wrap">
              <Search size={16} className="search-icon" />
              <input
                type="text"
                placeholder="Buscar por nome ou CPF..."
                value={busca}
                onChange={(e) => setBusca(e.target.value)}
                className="admin-search-input"
              />
            </div>

            <div className="admin-filters-group">
              <div className="filter-select-wrap">
                <label>Situação:</label>
                <select
                  value={filtroSituacao}
                  onChange={(e) => setFiltroSituacao(e.target.value)}
                  className="filter-select"
                >
                  <option value="TODOS">Todas</option>
                  <option value="ATIVO">Ativo</option>
                  <option value="SUSPENSO">Suspenso</option>
                  <option value="INATIVO">Inativo</option>
                </select>
              </div>

              <div className="filter-select-wrap">
                <label>Categoria:</label>
                <select
                  value={filtroCategoria}
                  onChange={(e) => setFiltroCategoria(e.target.value)}
                  className="filter-select"
                >
                  <option value="TODAS">Todas</option>
                  <option value="CONTRIBUINTE">Contribuinte</option>
                  <option value="PROPRIETARIO">Proprietário</option>
                </select>
              </div>
            </div>
          </div>

          <div className="admin-table-wrap">
            <table className="admin-table">
              <thead>
                <tr>
                  <th>Nome do Titular</th>
                  <th>CPF</th>
                  <th>Categoria & Plano</th>
                  <th>Admissão</th>
                  <th>Situação Cadastral</th>
                  <th>Dependentes</th>
                  <th>Ações</th>
                </tr>
              </thead>
              <tbody>
                {carregando ? (
                  <tr>
                    <td colSpan={7} className="table-empty">
                      Carregando associados do servidor...
                    </td>
                  </tr>
                ) : associadosFiltrados.length > 0 ? (
                  associadosFiltrados.map((assoc) => (
                    <tr key={assoc.id}>
                      <td className="assoc-name-cell">
                        <strong>{assoc.pessoa.nome}</strong>
                        <span className="assoc-email-sub">
                          {assoc.usuarioId
                            ? 'Conta de acesso vinculada'
                            : 'Sem conta de acesso vinculada'}
                        </span>
                      </td>

                      <td className="assoc-cpf-cell">
                        {formatarCpf(assoc.pessoa.cpf)}
                      </td>

                      <td>
                        <span className="assoc-plan-name">
                          {nomePlano(assoc.plano)}
                        </span>
                        <span className="assoc-cat-sub">
                          {CATEGORIA_LABEL[assoc.categoria]} •{' '}
                          {formatarMoeda(assoc.plano.valorPeriodico)}/mês
                        </span>
                      </td>

                      <td>{formatarDataBr(assoc.dataAdmissao)}</td>

                      <td>
                        <span
                          className={`status-pill ${
                            assoc.situacaoCadastral === 'ATIVO'
                              ? 'status-ativo'
                              : assoc.situacaoCadastral === 'SUSPENSO'
                                ? 'status-suspenso'
                                : 'status-inativo'
                          }`}
                        >
                          {SITUACAO_LABEL[assoc.situacaoCadastral]}
                        </span>
                      </td>

                      <td>
                        <button
                          type="button"
                          className="btn-ver-deps"
                          onClick={() => void abrirModalDependentes(assoc)}
                        >
                          <Users size={13} />
                          <span>Gerenciar</span>
                        </button>
                      </td>

                      <td style={{ textAlign: 'right' }}>
                        <div className="action-buttons-cell">
                          <button
                            type="button"
                            className="btn-action-small"
                            onClick={() => {
                              setAssociadoParaSituacao(assoc)
                              setNovaSituacaoSelecionada(
                                assoc.situacaoCadastral,
                              )
                              setErroModalSituacao('')
                            }}
                            title="Alterar Situação"
                          >
                            Situação
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan={7} className="table-empty">
                      Nenhum associado encontrado.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* ABA 2: FORMULÁRIO DE CADASTRO DE TITULAR */}
      {abaAtiva === 'cadastro' && (
        <div className="admin-content-card">
          <div className="form-card-header">
            <h2 className="form-card-title">Novo Associado Titular</h2>
            <p className="form-card-desc">
              Cadastre um novo associado titular nas categorias Contribuinte ou
              Proprietário.
            </p>
          </div>

          {mensagemSucesso && (
            <div className="profile-alert success">
              <CheckCircle2 size={16} />
              <span>{mensagemSucesso}</span>
            </div>
          )}

          {mensagemErro && (
            <div className="profile-alert error">
              <AlertCircle size={16} />
              <span>{mensagemErro}</span>
            </div>
          )}

          <form
            className="cadastro-form"
            onSubmit={(e) => void handleCadastrarTitular(e)}
          >
            <div className="cadastro-grid">
              <div className="profile-field">
                <label className="profile-label">Nome Completo *</label>
                <input
                  type="text"
                  required
                  placeholder="Nome completo do titular"
                  className="profile-input"
                  value={formNome}
                  onChange={(e) => setFormNome(e.target.value)}
                />
              </div>

              <div className="profile-field">
                <label className="profile-label">CPF (11 dígitos) *</label>
                <input
                  type="text"
                  required
                  placeholder="000.000.000-00"
                  maxLength={14}
                  className="profile-input"
                  value={formatarCpf(formCpf)}
                  onChange={(e) => setFormCpf(e.target.value)}
                />
              </div>

              <div className="profile-field">
                <label className="profile-label">Data de Nascimento *</label>
                <input
                  type="date"
                  required
                  className="profile-input"
                  value={formDataNasc}
                  onChange={(e) => setFormDataNasc(e.target.value)}
                />
              </div>

              <div className="profile-field">
                <label className="profile-label">E-mail de Acesso *</label>
                <input
                  type="email"
                  required
                  placeholder="associado@email.com"
                  className="profile-input"
                  value={formEmail}
                  onChange={(e) => setFormEmail(e.target.value)}
                />
              </div>

              <div className="profile-field">
                <label className="profile-label">Senha Inicial *</label>
                <div className="password-input-wrap">
                  <input
                    type={mostrarSenha ? 'text' : 'password'}
                    required
                    minLength={6}
                    placeholder="Mínimo 6 caracteres"
                    className="profile-input password-input-field"
                    value={formSenha}
                    onChange={(e) => setFormSenha(e.target.value)}
                  />
                  <button
                    type="button"
                    className="password-toggle-btn"
                    onClick={() => setMostrarSenha((prev) => !prev)}
                    aria-label={mostrarSenha ? 'Ocultar senha' : 'Mostrar senha'}
                  >
                    {mostrarSenha ? <EyeOff size={16} /> : <Eye size={16} />}
                  </button>
                </div>
              </div>

              <div className="profile-field">
                <label className="profile-label">
                  Categoria de Associado *
                </label>
                <select
                  className="profile-input"
                  value={formCategoria}
                  onChange={(e) => {
                    const cat = e.target.value as CategoriaAssociado
                    setFormCategoria(cat)
                    const primeiroCompativel = planos.find(
                      (p) => CATEGORIA_DO_PLANO[p.tipo] === cat,
                    )
                    if (primeiroCompativel) {
                      setFormTipoPlano(primeiroCompativel.tipo)
                    } else {
                      setFormTipoPlano(
                        cat === 'PROPRIETARIO' ? 'PROPRIETARIO' : 'BASICO',
                      )
                    }
                  }}
                >
                  <option value="CONTRIBUINTE">Contribuinte</option>
                  <option value="PROPRIETARIO">Proprietário</option>
                </select>
              </div>

              <div className="profile-field">
                <label className="profile-label">
                  Plano de Adesão *
                </label>
                <select
                  className="profile-input"
                  value={formTipoPlano}
                  onChange={(e) =>
                    setFormTipoPlano(e.target.value as TipoPlano)
                  }
                  disabled={planosDaCategoria.length === 0}
                >
                  {planosDaCategoria.map((p) => (
                    <option key={p.id} value={p.tipo}>
                      {nomePlano(p)} ({formatarMoeda(p.valorPeriodico)}/mês •{' '}
                      {p.limiteMensalReservas} reservas • até{' '}
                      {p.maxDependentesAtivos} dep.)
                    </option>
                  ))}
                </select>
              </div>
            </div>

            {/* Plano Preview Card (valores reais do back-end) */}
            {planoSelecionadoForm && (
              <div className="plan-preview-box">
                <div className="plan-preview-header">
                  <div>
                    <span className="plan-preview-label">
                      Resumo do Plano Selecionado
                    </span>
                    <h3 className="plan-preview-title">
                      {nomePlano(planoSelecionadoForm)}
                    </h3>
                  </div>
                  <div className="plan-preview-price">
                    {formatarMoeda(planoSelecionadoForm.valorPeriodico)}
                    <span className="plan-preview-period">/mês</span>
                  </div>
                </div>

                <div className="plan-preview-params">
                  <span className="param-pill">
                    Limite de Reservas:{' '}
                    <strong>
                      {planoSelecionadoForm.limiteMensalReservas}/mês
                    </strong>
                  </span>
                  <span className="param-pill">
                    Capacidade de Dependentes:{' '}
                    <strong>
                      até {planoSelecionadoForm.maxDependentesAtivos} ativo(s)
                    </strong>
                  </span>
                </div>
              </div>
            )}

            <div className="cadastro-form-actions">
              <button
                type="button"
                className="profile-btn-cancel"
                onClick={() => setAbaAtiva('lista')}
              >
                Voltar
              </button>
              <button
                type="submit"
                className="btn-create-order"
                style={{ width: 'auto', padding: '10px 24px' }}
                disabled={submittingCadastro || planosDaCategoria.length === 0}
              >
                <UserPlus size={16} />
                <span>
                  {submittingCadastro ? 'Cadastrando...' : 'Cadastrar Titular'}
                </span>
              </button>
            </div>
          </form>
        </div>
      )}

      {/* ABA 3: PLANOS DO CLUBE (dados reais de GET /api/planos) */}
      {abaAtiva === 'planos' && (
        <div className="admin-content-card">
          <div className="form-card-header">
            <h2 className="form-card-title">Parâmetros dos Planos</h2>
            <p className="form-card-desc">
              Valores e limites carregados diretamente do back-end
            </p>
          </div>

          {carregando ? (
            <p className="no-deps-text">Carregando planos...</p>
          ) : (
            <div className="planos-grid">
              {planos.map((plano) => {
                const badgeNivel =
                  plano.tipo === 'PROPRIETARIO'
                    ? 'Proprietário'
                    : plano.tipo === 'BASICO'
                      ? 'Contribuinte · Básico'
                      : plano.tipo === 'PLUS'
                        ? 'Contribuinte · Plus'
                        : 'Contribuinte · Premium'

                return (
                <div
                  key={plano.id}
                  className={`plano-card-item plano-card--${plano.tipo.toLowerCase()}`}
                >
                  <div className="plano-card-top">
                    <span className="plano-tipo-badge">{badgeNivel}</span>
                    <span className="plano-valor">
                      {formatarMoeda(plano.valorPeriodico)}
                      <small>/mês</small>
                    </span>
                  </div>

                  <h3 className="plano-nome">{nomePlano(plano)}</h3>

                  <div className="plano-limits-list">
                    <div className="plano-limit-row">
                      <span>Limite Mensal de Reservas:</span>
                      <strong>{plano.limiteMensalReservas}</strong>
                    </div>
                    <div className="plano-limit-row">
                      <span>Dependentes Ativos Permitidos:</span>
                      <strong>até {plano.maxDependentesAtivos}</strong>
                    </div>
                  </div>

                  {ehAdmin && (
                    <div className="plano-card-footer">
                      <button
                        type="button"
                        className="btn-action-small plano-btn-edit"
                        onClick={() => abrirEdicaoPlano(plano)}
                      >
                        <Edit2 size={12} />
                        <span>Editar parâmetros</span>
                      </button>
                    </div>
                  )}
                </div>
                )
              })}
            </div>
          )}
        </div>
      )}

      {/* ABA 4: USUÁRIOS E PERFIS (somente ADMIN) */}
      {ehAdmin && abaAtiva === 'usuarios' && <UsuariosAdminPanel />}

      {/* MODAL: GERENCIAMENTO DE DEPENDENTES POR TITULAR */}
      {titularSelecionado && (
        <Modal
          isOpen={!!titularSelecionado}
          onClose={() => setTitularSelecionado(null)}
          title={`Dependentes de ${titularSelecionado.pessoa.nome}`}
          subtitle={`${nomePlano(titularSelecionado.plano)} (Máx: ${titularSelecionado.plano.maxDependentesAtivos} ativo(s))`}
          maxWidth={640}
        >
          <div className="deps-modal-body">
            {depSucesso && (
              <div className="profile-alert success">
                <CheckCircle2 size={16} />
                <span>{depSucesso}</span>
              </div>
            )}
            {depErro && (
              <div className="profile-alert error">
                <AlertCircle size={16} />
                <span>{depErro}</span>
              </div>
            )}

            <div className="deps-list-section">
              <h4 className="deps-subtitle">Dependentes Cadastrados</h4>
              {carregandoDeps ? (
                <p className="no-deps-text">Carregando dependentes...</p>
              ) : (
                <div className="deps-cards-list">
                  {dependentesTitular.length > 0 ? (
                    dependentesTitular.map((vinc) => {
                      const idade = calcularIdade(
                        vinc.dependente.dataNascimento,
                      )
                      const isAtivo = vinc.status === 'ATIVO'

                      return (
                        <div
                          key={vinc.id}
                          className={`dep-manage-row ${isAtivo ? '' : 'inactive'}`}
                        >
                          <div className="dep-manage-info">
                            <div className="dep-manage-name-row">
                              <strong>{vinc.dependente.nome}</strong>
                              <span
                                className={`status-pill ${isAtivo ? 'status-ativo' : 'status-inativo'}`}
                              >
                                {isAtivo
                                  ? 'Ativo'
                                  : vinc.motivoInativacao === 'IDADE_LIMITE'
                                    ? 'Inativo (21 anos)'
                                    : 'Inativo'}
                              </span>
                            </div>
                            <span className="dep-manage-sub">
                              {vinc.parentesco} • CPF:{' '}
                              {formatarCpf(vinc.dependente.cpf)} • {idade} anos
                              ({formatarDataBr(vinc.dependente.dataNascimento)})
                            </span>
                          </div>

                          {isAtivo && (
                            <button
                              type="button"
                              className="btn-inativar-dep"
                              onClick={() =>
                                void handleInativarDependente(vinc.id)
                              }
                            >
                              Inativar
                            </button>
                          )}
                        </div>
                      )
                    })
                  ) : (
                    <p className="no-deps-text">
                      Nenhum dependente vinculado a este titular.
                    </p>
                  )}
                </div>
              )}
            </div>

            <div className="add-dep-box">
              <h4 className="deps-subtitle">+ Cadastrar Novo Dependente</h4>
              <form
                onSubmit={(e) => void handleCadastrarDependente(e)}
                className="dep-form"
              >
                <div className="dep-form-grid">
                  <div className="profile-field">
                    <label className="profile-label">Nome Completo *</label>
                    <input
                      type="text"
                      required
                      placeholder="Nome do dependente"
                      className="profile-input"
                      value={depNome}
                      onChange={(e) => setDepNome(e.target.value)}
                    />
                  </div>

                  <div className="profile-field">
                    <label className="profile-label">CPF *</label>
                    <input
                      type="text"
                      required
                      placeholder="000.000.000-00"
                      maxLength={14}
                      className="profile-input"
                      value={formatarCpf(depCpf)}
                      onChange={(e) => setDepCpf(e.target.value)}
                    />
                  </div>

                  <div className="profile-field">
                    <label className="profile-label">
                      Data de Nascimento *
                    </label>
                    <input
                      type="date"
                      required
                      className="profile-input"
                      value={depDataNasc}
                      onChange={(e) => setDepDataNasc(e.target.value)}
                    />
                    <span className="field-hint">
                      Deve ter menos de 21 anos.
                    </span>
                  </div>

                  <div className="profile-field">
                    <label className="profile-label">Parentesco *</label>
                    <select
                      className="profile-input"
                      value={depParentesco}
                      onChange={(e) => setDepParentesco(e.target.value)}
                    >
                      <option value="Filho(a)">Filho(a)</option>
                      <option value="Cônjuge">Cônjuge</option>
                      <option value="Enteado(a)">Enteado(a)</option>
                      <option value="Outro">Outro Vínculo Legal</option>
                    </select>
                  </div>
                </div>

                <div className="dep-form-actions">
                  <button
                    type="submit"
                    className="btn-salvar-dep"
                    disabled={salvandoDep}
                  >
                    <Plus size={15} />
                    <span>
                      {salvandoDep ? 'Salvando...' : 'Adicionar Dependente'}
                    </span>
                  </button>
                </div>
              </form>
            </div>
          </div>
        </Modal>
      )}

      {/* MODAL: ALTERAR SITUAÇÃO CADASTRAL */}
      {associadoParaSituacao && (
        <Modal
          isOpen={!!associadoParaSituacao}
          onClose={() => setAssociadoParaSituacao(null)}
          title={`Alterar Situação de ${associadoParaSituacao.pessoa.nome}`}
          subtitle="Controle de acesso, suspensão e inativação"
          maxWidth={480}
        >
          <div className="modal-edit-body">
            {erroModalSituacao && (
              <div className="profile-alert error">
                <AlertCircle size={16} />
                <span>{erroModalSituacao}</span>
              </div>
            )}

            <p className="modal-edit-text">
              Situação atual:{' '}
              <strong>
                {SITUACAO_LABEL[associadoParaSituacao.situacaoCadastral]}
              </strong>
            </p>

            <div className="profile-field">
              <label className="profile-label">Nova Situação Cadastral</label>
              <select
                className="profile-input"
                value={novaSituacaoSelecionada}
                onChange={(e) =>
                  setNovaSituacaoSelecionada(
                    e.target.value as SituacaoCadastral,
                  )
                }
              >
                <option value="ATIVO">Ativo</option>
                <option value="SUSPENSO">
                  Suspenso (bloqueia uso das instalações)
                </option>
                <option value="INATIVO">
                  Inativo (inativa automaticamente os dependentes)
                </option>
              </select>
            </div>

            <div className="modal-edit-actions">
              <button
                type="button"
                className="profile-btn-cancel"
                onClick={() => setAssociadoParaSituacao(null)}
              >
                Cancelar
              </button>
              <button
                type="button"
                className="profile-btn-save"
                disabled={salvandoSituacao}
                onClick={() => void handleSalvarNovaSituacao()}
              >
                {salvandoSituacao ? 'Salvando...' : 'Salvar Situação'}
              </button>
            </div>
          </div>
        </Modal>
      )}

      {/* MODAL: EDITAR PARÂMETROS DO PLANO (ADMIN) */}
      {planoEditando && (
        <Modal
          isOpen={!!planoEditando}
          onClose={() => setPlanoEditando(null)}
          title={`Editar ${nomePlano(planoEditando)}`}
          subtitle="Atualize os parâmetros vigentes do plano no back-end"
          maxWidth={460}
        >
          <form
            className="modal-edit-body"
            onSubmit={(e) => void handleSalvarParametrosPlano(e)}
          >
            {erroPlanoParam && (
              <div className="profile-alert error">
                <AlertCircle size={16} />
                <span>{erroPlanoParam}</span>
              </div>
            )}

            <div className="profile-field">
              <label className="profile-label">Valor Mensal (R$)</label>
              <input
                type="number"
                step="0.01"
                min="0"
                required
                className="profile-input"
                value={planoValorEdit}
                onChange={(e) => setPlanoValorEdit(e.target.value)}
              />
            </div>

            <div className="profile-field">
              <label className="profile-label">Limite Mensal de Reservas</label>
              <input
                type="number"
                min="0"
                required
                className="profile-input"
                value={planoReservasEdit}
                onChange={(e) => setPlanoReservasEdit(e.target.value)}
              />
            </div>

            <div className="profile-field">
              <label className="profile-label">
                Máximo de Dependentes Ativos
              </label>
              <input
                type="number"
                min="0"
                required
                className="profile-input"
                value={planoDepsEdit}
                onChange={(e) => setPlanoDepsEdit(e.target.value)}
              />
            </div>

            <div className="modal-edit-actions">
              <button
                type="button"
                className="profile-btn-cancel"
                onClick={() => setPlanoEditando(null)}
              >
                Cancelar
              </button>
              <button
                type="submit"
                className="profile-btn-save"
                disabled={salvandoPlanoParam}
              >
                {salvandoPlanoParam ? 'Salvando...' : 'Salvar Parâmetros'}
              </button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  )
}
