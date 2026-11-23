import {
  AlertCircle,
  CheckCircle2,
  ChevronLeft,
  Clock,
  CreditCard,
  Edit2,
  Info,
  Save,
  ShieldCheck,
  User,
  Users,
} from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { Modal } from '../../components/Modal/Modal'
import { useAuth } from '../../context/AuthContext'
import { associadoService } from '../../services/associadoService'
import {
  CATEGORIA_LABEL,
  SITUACAO_LABEL,
  nomePlano,
  type VinculoDependencia,
} from '../../types/associado'
import { useMeuCadastro } from './MeuCadastroContext'
import {
  calcularIdade,
  formatarCpf,
  formatarDataBr,
  formatarMoeda,
  mensagemErroApi,
} from './associadoUtils'
import './AssociadoMensalidadesPage.css'

type AbaAtiva = 'dados' | 'dependentes' | 'pagamentos'

const ABAS_VALIDAS: readonly AbaAtiva[] = ['dados', 'dependentes', 'pagamentos']

const BREADCRUMB_LABEL: Record<AbaAtiva, string> = {
  dados: 'Dados Pessoais',
  dependentes: 'Dependentes',
  pagamentos: 'Mensalidades',
}

function resolverAba(valor: string | null): AbaAtiva {
  if (valor && (ABAS_VALIDAS as readonly string[]).includes(valor)) {
    return valor as AbaAtiva
  }
  return 'pagamentos'
}

export function AssociadoMensalidadesPage() {
  const { usuario } = useAuth()
  const { associado, carregando, naoVinculado, erro, atualizarLocal } =
    useMeuCadastro()
  const [searchParams, setSearchParams] = useSearchParams()

  const activeTab = resolverAba(searchParams.get('aba'))

  const [editando, setEditando] = useState(false)
  const [nomeEdit, setNomeEdit] = useState('')
  const [dataNascEdit, setDataNascEdit] = useState('')
  const [salvando, setSalvando] = useState(false)
  const [mensagemSucesso, setMensagemSucesso] = useState('')
  const [mensagemErro, setMensagemErro] = useState('')

  // Dependentes reais buscados do banco (GET /api/associados/me/dependentes)
  const [dependentes, setDependentes] = useState<VinculoDependencia[]>([])
  const [carregandoDeps, setCarregandoDeps] = useState(false)
  const [erroDeps, setErroDeps] = useState('')
  const [modalPagamentoAberto, setModalPagamentoAberto] = useState(false)

  useEffect(() => {
    if (associado) {
      setNomeEdit(associado.pessoa.nome)
      setDataNascEdit(associado.pessoa.dataNascimento)
    }
  }, [associado])

  useEffect(() => {
    if (!associado) {
      setDependentes([])
      setErroDeps('')
      return
    }

    let ativo = true
    setCarregandoDeps(true)
    setErroDeps('')

    associadoService
      .meusDependentes()
      .then((lista) => {
        if (ativo) setDependentes(lista)
      })
      .catch((e) => {
        if (ativo) {
          setErroDeps(
            mensagemErroApi(e, 'Não foi possível carregar os dependentes.'),
          )
        }
      })
      .finally(() => {
        if (ativo) setCarregandoDeps(false)
      })

    return () => {
      ativo = false
    }
  }, [associado])

  function selecionarAba(aba: AbaAtiva) {
    setEditando(false)
    setMensagemErro('')
    setSearchParams({ aba }, { replace: true })
  }

  async function handleSalvarDados() {
    if (!nomeEdit.trim()) {
      setMensagemErro('O nome não pode ser vazio.')
      return
    }
    if (!dataNascEdit) {
      setMensagemErro('A data de nascimento é obrigatória.')
      return
    }

    setSalvando(true)
    setMensagemErro('')
    setMensagemSucesso('')

    try {
      const atualizado = await associadoService.atualizarMeusDados({
        nome: nomeEdit.trim(),
        dataNascimento: dataNascEdit,
      })
      atualizarLocal(atualizado)
      setEditando(false)
      setMensagemSucesso('Dados pessoais atualizados com sucesso!')
      setTimeout(() => setMensagemSucesso(''), 3500)
    } catch (e) {
      setMensagemErro(mensagemErroApi(e, 'Erro ao atualizar dados pessoais.'))
    } finally {
      setSalvando(false)
    }
  }

  const dependentesAtivos = dependentes.filter((d) => d.status === 'ATIVO')
  const nomeExibicao =
    associado?.pessoa.nome ||
    usuario?.nome ||
    usuario?.email?.split('@')[0] ||
    'Associado'

  return (
    <div className="assoc-mensalidades-page">
      {/* 1. Barra Superior: Voltar + Breadcrumb + Segmented Tabs */}
      <div className="assoc-mens-topbar">
        <div className="assoc-mens-top-left">
          <Link to="/associado" className="assoc-mens-btn-back">
            <ChevronLeft size={16} strokeWidth={2.5} />
            <span>Voltar ao Início</span>
          </Link>

          <nav className="assoc-mens-breadcrumb" aria-label="Breadcrumb">
            <span>Visão geral</span>
            <span>›</span>
            <span className="assoc-mens-breadcrumb-active">
              {BREADCRUMB_LABEL[activeTab]}
            </span>
          </nav>
        </div>

        <div className="assoc-mens-seg-tabs" role="tablist">
          <button
            type="button"
            role="tab"
            aria-selected={activeTab === 'dados'}
            className={`assoc-mens-seg-btn ${activeTab === 'dados' ? 'active' : ''}`}
            onClick={() => selecionarAba('dados')}
          >
            <User size={14} />
            <span>Dados Pessoais</span>
          </button>

          <button
            type="button"
            role="tab"
            aria-selected={activeTab === 'dependentes'}
            className={`assoc-mens-seg-btn ${activeTab === 'dependentes' ? 'active' : ''}`}
            onClick={() => selecionarAba('dependentes')}
          >
            <Users size={14} />
            <span>
              Dependentes{associado ? ` (${dependentesAtivos.length})` : ''}
            </span>
          </button>

          <button
            type="button"
            role="tab"
            aria-selected={activeTab === 'pagamentos'}
            className={`assoc-mens-seg-btn ${activeTab === 'pagamentos' ? 'active' : ''}`}
            onClick={() => selecionarAba('pagamentos')}
          >
            <CreditCard size={14} />
            <span>Histórico de Pagamento</span>
          </button>
        </div>
      </div>

      {/* Container principal */}
      <div className="assoc-mens-container">
        {/* 2. Banner do Associado (dados reais do banco) */}
        <section className="assoc-mens-banner">
          <div className="assoc-mens-banner-left">
            <div className="assoc-mens-avatar">
              <User size={28} />
            </div>
            <div className="assoc-mens-banner-info">
              <h1 className="assoc-mens-banner-name">{nomeExibicao}</h1>
              <div className="assoc-mens-banner-chips">
                {associado ? (
                  <>
                    <span className="assoc-mens-chip assoc-mens-chip--plan">
                      {nomePlano(associado.plano)}
                    </span>
                    <span className="assoc-mens-chip assoc-mens-chip--neutral">
                      Sócio {CATEGORIA_LABEL[associado.categoria]}
                    </span>
                    <span
                      className={`assoc-mens-chip ${
                        associado.situacaoCadastral === 'ATIVO'
                          ? 'assoc-mens-chip--active'
                          : 'assoc-mens-chip--inactive'
                      }`}
                    >
                      Cadastro {SITUACAO_LABEL[associado.situacaoCadastral]}
                    </span>
                  </>
                ) : (
                  <span className="assoc-mens-chip assoc-mens-chip--neutral">
                    {carregando
                      ? 'Carregando cadastro...'
                      : 'Cadastro não vinculado'}
                  </span>
                )}
              </div>
            </div>
          </div>
        </section>

        {/* Feedback global */}
        {mensagemSucesso && (
          <div className="assoc-mens-alert assoc-mens-alert--success">
            <CheckCircle2 size={16} />
            <span>{mensagemSucesso}</span>
          </div>
        )}

        {(mensagemErro || erro) && (
          <div className="assoc-mens-alert assoc-mens-alert--error">
            <AlertCircle size={16} />
            <span>{mensagemErro || erro}</span>
          </div>
        )}

        {/* =========================================
            ABA 1: HISTÓRICO DE PAGAMENTO / MENSALIDADES
           ========================================= */}
        {activeTab === 'pagamentos' && (
          <>
            {associado && (
              <section className="assoc-mens-kpi-grid">
                <div className="assoc-mens-kpi-card">
                  <div className="assoc-mens-kpi-top">
                    <span className="assoc-mens-kpi-label">Plano Contratado</span>
                    <span className="assoc-mens-kpi-icon">
                      <ShieldCheck size={16} />
                    </span>
                  </div>
                  <div className="assoc-mens-kpi-value">
                    {nomePlano(associado.plano)}
                  </div>
                  <span className="assoc-mens-kpi-sub">
                    Situação: {SITUACAO_LABEL[associado.situacaoCadastral]}
                  </span>
                </div>

                <div className="assoc-mens-kpi-card">
                  <div className="assoc-mens-kpi-top">
                    <span className="assoc-mens-kpi-label">Valor Periódico</span>
                    <span className="assoc-mens-kpi-icon assoc-mens-kpi-icon--green">
                      <CreditCard size={16} />
                    </span>
                  </div>
                  <div className="assoc-mens-kpi-value">
                    {formatarMoeda(associado.plano.valorPeriodico)}
                  </div>
                  <span className="assoc-mens-kpi-sub">
                    Admissão em {formatarDataBr(associado.dataAdmissao)}
                  </span>
                </div>

                <div className="assoc-mens-kpi-card">
                  <div className="assoc-mens-kpi-top">
                    <span className="assoc-mens-kpi-label">Limites do Plano</span>
                    <span className="assoc-mens-kpi-icon">
                      <Users size={16} />
                    </span>
                  </div>
                  <div className="assoc-mens-kpi-value">
                    {dependentesAtivos.length} /{' '}
                    {associado.plano.maxDependentesAtivos} dep.
                  </div>
                  <span className="assoc-mens-kpi-sub">
                    Até {associado.plano.limiteMensalReservas} reservas / mês
                  </span>
                </div>
              </section>
            )}

            <section className="assoc-mens-card">
              <div className="assoc-mens-card-header">
                <div>
                  <h2 className="assoc-mens-card-title">
                    Histórico de Pagamentos e Mensalidades
                  </h2>
                  <p className="assoc-mens-card-desc">
                    Integração com o módulo Financeiro e Mensalidades
                  </p>
                </div>

                {/* Botão reservado para integração futura com o módulo Financeiro */}
                <button
                  type="button"
                  className="assoc-mens-btn-pay"
                  onClick={() => setModalPagamentoAberto(true)}
                >
                  <CreditCard size={15} />
                  <span>Fazer pagamento</span>
                </button>
              </div>

              <div className="assoc-mens-placeholder">
                <div className="assoc-mens-placeholder-icon">
                  <Clock size={22} />
                </div>
                <div className="assoc-mens-placeholder-text">
                  <h4>Módulo Financeiro em desenvolvimento</h4>
                  <p>
                    As cobranças de mensalidades, pagamento via PIX/Boleto,
                    histórico de pagamentos e emissão de comprovantes serão
                    exibidos aqui assim que o módulo Financeiro for implementado
                    no back-end.
                  </p>
                </div>
              </div>
            </section>
          </>
        )}

        {/* =========================================
            ABA 2: DEPENDENTES (100% BANCO DE DADOS)
           ========================================= */}
        {activeTab === 'dependentes' && (
          <section className="assoc-mens-card">
            {carregando ? (
              <div className="assoc-mens-empty">
                <p>Carregando dados do cadastro...</p>
              </div>
            ) : !associado ? (
              <div className="assoc-mens-empty">
                <p>
                  Os dependentes aparecerão aqui assim que seu cadastro de
                  associado titular for vinculado pela recepção.
                </p>
              </div>
            ) : (
              <>
                <div className="assoc-mens-card-header">
                  <div>
                    <h2 className="assoc-mens-card-title">
                      Dependentes Vinculados
                    </h2>
                    <p className="assoc-mens-card-desc">
                      Dependentes cadastrados no seu título para acesso ao clube
                      e reservas
                    </p>
                  </div>
                  <div className="assoc-mens-dep-counter">
                    <Users size={14} />
                    <span>
                      {dependentesAtivos.length} de{' '}
                      {associado.plano.maxDependentesAtivos} vagas ocupadas
                    </span>
                  </div>
                </div>

                {erroDeps && (
                  <div className="assoc-mens-alert assoc-mens-alert--error">
                    <AlertCircle size={16} />
                    <span>{erroDeps}</span>
                  </div>
                )}

                {carregandoDeps ? (
                  <div className="assoc-mens-empty">
                    <p>Carregando dependentes...</p>
                  </div>
                ) : dependentes.length > 0 ? (
                  <div className="assoc-mens-dep-list">
                    {dependentes.map((vinc) => {
                      const idade = calcularIdade(vinc.dependente.dataNascimento)
                      const isAtivo = vinc.status === 'ATIVO'

                      return (
                        <div
                          key={vinc.id}
                          className={`assoc-mens-dep-card ${isAtivo ? '' : 'inactive'}`}
                        >
                          <div className="assoc-mens-dep-avatar">
                            <Users size={18} />
                          </div>

                          <div className="assoc-mens-dep-info">
                            <div className="assoc-mens-dep-header">
                              <span className="assoc-mens-dep-name">
                                {vinc.dependente.nome}
                              </span>
                              <span
                                className={`assoc-mens-dep-badge ${isAtivo ? 'active' : 'inactive'}`}
                              >
                                {isAtivo
                                  ? 'Vínculo Ativo'
                                  : vinc.motivoInativacao === 'IDADE_LIMITE'
                                    ? 'Inativo (Limite 21 anos)'
                                    : 'Inativo'}
                              </span>
                            </div>

                            <div className="assoc-mens-dep-meta">
                              <span>
                                Parentesco: <strong>{vinc.parentesco}</strong>
                              </span>
                              <span className="assoc-mens-dep-dot">•</span>
                              <span>
                                CPF: {formatarCpf(vinc.dependente.cpf)}
                              </span>
                              <span className="assoc-mens-dep-dot">•</span>
                              <span>
                                Nascimento:{' '}
                                {formatarDataBr(vinc.dependente.dataNascimento)}{' '}
                                ({idade} anos)
                              </span>
                              {vinc.dataInicio && (
                                <>
                                  <span className="assoc-mens-dep-dot">•</span>
                                  <span>
                                    Início: {formatarDataBr(vinc.dataInicio)}
                                  </span>
                                </>
                              )}
                              {!isAtivo && vinc.dataFim && (
                                <>
                                  <span className="assoc-mens-dep-dot">•</span>
                                  <span>
                                    Encerrado em: {formatarDataBr(vinc.dataFim)}
                                  </span>
                                </>
                              )}
                            </div>

                            {isAtivo && idade >= 21 && (
                              <div className="assoc-mens-dep-warning">
                                <AlertCircle size={13} />
                                <span>
                                  Dependente com 21 anos completados (RF-AS04).
                                </span>
                              </div>
                            )}
                          </div>
                        </div>
                      )
                    })}
                  </div>
                ) : (
                  <div className="assoc-mens-empty">
                    <p>Nenhum dependente vinculado ao seu cadastro.</p>
                  </div>
                )}
              </>
            )}
          </section>
        )}

        {/* =========================================
            ABA 3: DADOS PESSOAIS (100% BANCO DE DADOS)
           ========================================= */}
        {activeTab === 'dados' && (
          <section className="assoc-mens-card">
            {carregando ? (
              <div className="assoc-mens-empty">
                <p>Carregando dados cadastrais...</p>
              </div>
            ) : naoVinculado || !associado ? (
              <>
                <div className="assoc-mens-card-header">
                  <div>
                    <h2 className="assoc-mens-card-title">Dados da Conta</h2>
                    <p className="assoc-mens-card-desc">
                      Informações da conta autenticada
                    </p>
                  </div>
                </div>

                <div className="assoc-mens-grid">
                  <div className="assoc-mens-field">
                    <span className="assoc-mens-label">Nome da Conta</span>
                    <div className="assoc-mens-value strong">
                      {usuario?.nome || '—'}
                    </div>
                  </div>
                  <div className="assoc-mens-field">
                    <span className="assoc-mens-label">E-mail de Acesso</span>
                    <div className="assoc-mens-value">
                      {usuario?.email || '—'}
                    </div>
                  </div>
                </div>

                <div className="assoc-mens-footnote">
                  <AlertCircle size={15} />
                  <span>
                    Sua conta ainda não está vinculada a um cadastro de associado
                    titular no clube. Procure a recepção ou administração para
                    concluir seu cadastro (RF-AS01).
                  </span>
                </div>
              </>
            ) : (
              <>
                <div className="assoc-mens-card-header">
                  <div>
                    <h2 className="assoc-mens-card-title">
                      Informações Pessoais
                    </h2>
                    <p className="assoc-mens-card-desc">
                      Dados do associado titular registrados no clube
                    </p>
                  </div>

                  {!editando ? (
                    <button
                      type="button"
                      className="assoc-mens-btn-edit"
                      onClick={() => setEditando(true)}
                    >
                      <Edit2 size={14} />
                      <span>Editar dados</span>
                    </button>
                  ) : (
                    <div className="assoc-mens-edit-actions">
                      <button
                        type="button"
                        className="assoc-mens-btn-cancel"
                        onClick={() => {
                          setEditando(false)
                          setNomeEdit(associado.pessoa.nome)
                          setDataNascEdit(associado.pessoa.dataNascimento)
                          setMensagemErro('')
                        }}
                      >
                        Cancelar
                      </button>
                      <button
                        type="button"
                        className="assoc-mens-btn-save"
                        onClick={() => void handleSalvarDados()}
                        disabled={salvando}
                      >
                        <Save size={14} />
                        <span>{salvando ? 'Salvando...' : 'Salvar'}</span>
                      </button>
                    </div>
                  )}
                </div>

                <div className="assoc-mens-grid">
                  <div className="assoc-mens-field">
                    <label className="assoc-mens-label">Nome Completo</label>
                    {editando ? (
                      <input
                        type="text"
                        className="assoc-mens-input"
                        value={nomeEdit}
                        onChange={(e) => setNomeEdit(e.target.value)}
                      />
                    ) : (
                      <div className="assoc-mens-value strong">
                        {associado.pessoa.nome}
                      </div>
                    )}
                  </div>

                  <div className="assoc-mens-field">
                    <label className="assoc-mens-label">CPF (Titular)</label>
                    <div className="assoc-mens-value code">
                      {formatarCpf(associado.pessoa.cpf)}
                    </div>
                  </div>

                  <div className="assoc-mens-field">
                    <label className="assoc-mens-label">
                      Data de Nascimento
                    </label>
                    {editando ? (
                      <input
                        type="date"
                        className="assoc-mens-input"
                        value={dataNascEdit}
                        onChange={(e) => setDataNascEdit(e.target.value)}
                      />
                    ) : (
                      <div className="assoc-mens-value">
                        {formatarDataBr(associado.pessoa.dataNascimento)} (
                        {calcularIdade(associado.pessoa.dataNascimento)} anos)
                      </div>
                    )}
                  </div>

                  <div className="assoc-mens-field">
                    <label className="assoc-mens-label">
                      E-mail Cadastrado
                    </label>
                    <div className="assoc-mens-value">
                      {usuario?.email || '—'}
                    </div>
                  </div>

                  <div className="assoc-mens-field">
                    <label className="assoc-mens-label">
                      Categoria de Associado
                    </label>
                    <div className="assoc-mens-value">
                      Sócio {CATEGORIA_LABEL[associado.categoria]}
                    </div>
                  </div>

                  <div className="assoc-mens-field">
                    <label className="assoc-mens-label">Data de Admissão</label>
                    <div className="assoc-mens-value">
                      {formatarDataBr(associado.dataAdmissao)}
                    </div>
                  </div>
                </div>

                {/* Plano Vinculado (dados reais do back-end) */}
                <div className="assoc-mens-plan-box">
                  <div className="assoc-mens-plan-top">
                    <div>
                      <span className="assoc-mens-plan-badge">
                        Plano Vinculado
                      </span>
                      <h3 className="assoc-mens-plan-title">
                        {nomePlano(associado.plano)}
                      </h3>
                    </div>
                    <div className="assoc-mens-plan-price">
                      {formatarMoeda(associado.plano.valorPeriodico)}
                      <span className="assoc-mens-plan-period">/mês</span>
                    </div>
                  </div>

                  <div className="assoc-mens-plan-features">
                    <div className="assoc-mens-plan-pill">
                      <ShieldCheck size={14} />
                      <span>
                        Até {associado.plano.limiteMensalReservas} reservas/mês
                      </span>
                    </div>
                    <div className="assoc-mens-plan-pill">
                      <Users size={14} />
                      <span>
                        Até {associado.plano.maxDependentesAtivos} dependente(s)
                        ativo(s)
                      </span>
                    </div>
                  </div>
                </div>
              </>
            )}
          </section>
        )}
      </div>

      {/* Modal informativa até a integração com o módulo Financeiro */}
      <Modal
        isOpen={modalPagamentoAberto}
        onClose={() => setModalPagamentoAberto(false)}
        title="Fazer Pagamento"
        subtitle="Módulo Financeiro em desenvolvimento"
        maxWidth={460}
      >
        <div className="assoc-mens-pay-modal-body">
          <div className="assoc-mens-pay-modal-alert">
            <Info size={20} className="assoc-mens-pay-modal-icon" />
            <p className="assoc-mens-pay-modal-text">
              O fluxo de pagamento de mensalidades será integrado ao módulo
              Financeiro assim que estiver disponível.
            </p>
          </div>

          <div className="assoc-mens-pay-modal-actions">
            <button
              type="button"
              className="assoc-mens-btn-save"
              onClick={() => setModalPagamentoAberto(false)}
            >
              Entendi
            </button>
          </div>
        </div>
      </Modal>
    </div>
  )
}
