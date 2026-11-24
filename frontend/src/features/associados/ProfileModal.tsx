import {
  AlertCircle,
  CheckCircle2,
  Clock,
  CreditCard,
  Edit2,
  Save,
  ShieldCheck,
  User,
  Users,
} from 'lucide-react'
import { useEffect, useState } from 'react'
import { Modal } from '../../components/Modal/Modal'
import { useAuth } from '../../context/AuthContext'
import { associadoService } from '../../services/associadoService'
import {
  CATEGORIA_LABEL,
  SITUACAO_LABEL,
  nomePlano,
  type VinculoDependencia,
} from '../../types/associado'
import { PERFIL_LABEL } from '../../types/user'
import { useMeuCadastro } from './MeuCadastroContext'
import {
  calcularIdade,
  formatarCpf,
  formatarDataBr,
  formatarMoeda,
  mensagemErroApi,
} from './associadoUtils'
import './ProfileModal.css'

interface ProfileModalProps {
  isOpen: boolean
  onClose: () => void
  initialTab?: 'dados' | 'dependentes' | 'pagamentos'
}

export function ProfileModal({
  isOpen,
  onClose,
  initialTab = 'dados',
}: ProfileModalProps) {
  const { usuario } = useAuth()
  const { associado, carregando, naoVinculado, erro, atualizarLocal } =
    useMeuCadastro()

  const ehAssociado = usuario?.perfil === 'ASSOCIADO'

  const [activeTab, setActiveTab] = useState<
    'dados' | 'dependentes' | 'pagamentos'
  >(initialTab)
  const [editando, setEditando] = useState(false)
  const [nomeEdit, setNomeEdit] = useState('')
  const [dataNascEdit, setDataNascEdit] = useState('')
  const [salvando, setSalvando] = useState(false)
  const [mensagemSucesso, setMensagemSucesso] = useState('')
  const [mensagemErro, setMensagemErro] = useState('')

  // Dependentes reais buscados do back-end (GET /api/associados/me/dependentes)
  const [dependentes, setDependentes] = useState<VinculoDependencia[]>([])
  const [carregandoDeps, setCarregandoDeps] = useState(false)
  const [erroDeps, setErroDeps] = useState('')

  useEffect(() => {
    if (isOpen) {
      setActiveTab(initialTab)
      setEditando(false)
      setMensagemSucesso('')
      setMensagemErro('')
      if (associado) {
        setNomeEdit(associado.pessoa.nome)
        setDataNascEdit(associado.pessoa.dataNascimento)
      }
    }
  }, [isOpen, initialTab, associado])

  useEffect(() => {
    if (!isOpen || !ehAssociado || !associado) return

    let ativo = true
    setCarregandoDeps(true)
    setErroDeps('')

    associadoService
      .meusDependentes()
      .then((lista) => {
        if (ativo) setDependentes(lista)
      })
      .catch((e) => {
        if (ativo)
          setErroDeps(
            mensagemErroApi(e, 'Não foi possível carregar os dependentes.'),
          )
      })
      .finally(() => {
        if (ativo) setCarregandoDeps(false)
      })

    return () => {
      ativo = false
    }
  }, [isOpen, ehAssociado, associado])

  if (!usuario) return null

  // Perfil para não-associados (Admin / Funcionários)
  if (!ehAssociado) {
    return (
      <Modal
        isOpen={isOpen}
        onClose={onClose}
        title="Meu Perfil"
        subtitle="Informações da conta autenticada"
        maxWidth={520}
      >
        <div className="profile-modal-content">
          <div className="profile-hero">
            <div className="profile-hero-avatar">
              <User size={30} />
            </div>
            <div className="profile-hero-info">
              <h2 className="profile-hero-name">{usuario.nome}</h2>
              <div className="profile-hero-badges">
                <span className="badge-chip plan">
                  {PERFIL_LABEL[usuario.perfil]}
                </span>
              </div>
            </div>
          </div>

          <div className="profile-grid">
            <div className="profile-field">
              <label className="profile-label">Nome</label>
              <div className="profile-value strong">{usuario.nome}</div>
            </div>
            <div className="profile-field">
              <label className="profile-label">E-mail</label>
              <div className="profile-value">{usuario.email}</div>
            </div>
            <div className="profile-field">
              <label className="profile-label">Perfil de Acesso</label>
              <div className="profile-value">{PERFIL_LABEL[usuario.perfil]}</div>
            </div>
            <div className="profile-field">
              <label className="profile-label">UID da Conta</label>
              <div className="profile-value code">{usuario.uid}</div>
            </div>
          </div>
        </div>
      </Modal>
    )
  }

  const dependentesAtivos = dependentes.filter((d) => d.status === 'ATIVO')
  const nomeExibicao = associado?.pessoa.nome ?? usuario.nome

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

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Perfil do Associado"
      subtitle="Informações cadastrais, dependentes e histórico de pagamentos"
      maxWidth={720}
    >
      <div className="profile-modal-content">
        {/* Top Profile Summary Card */}
        <div className="profile-hero">
          <div className="profile-hero-avatar">
            <User size={32} />
          </div>
          <div className="profile-hero-info">
            <h2 className="profile-hero-name">{nomeExibicao}</h2>
            <div className="profile-hero-badges">
              {associado ? (
                <>
                  <span className="badge-chip plan">
                    {nomePlano(associado.plano)}
                  </span>
                  <span
                    className={`badge-chip ${associado.situacaoCadastral === 'ATIVO' ? 'active' : 'inactive'}`}
                  >
                    Cadastro {SITUACAO_LABEL[associado.situacaoCadastral]}
                  </span>
                </>
              ) : (
                <span className="badge-chip active">
                  {carregando
                    ? 'Carregando cadastro...'
                    : 'Cadastro não vinculado'}
                </span>
              )}
            </div>
          </div>
        </div>

        {/* Tab Navigation */}
        <div className="profile-tabs">
          <button
            type="button"
            className={`profile-tab-btn ${activeTab === 'dados' ? 'active' : ''}`}
            onClick={() => setActiveTab('dados')}
          >
            <User size={16} />
            <span>Dados Pessoais</span>
          </button>

          <button
            type="button"
            className={`profile-tab-btn ${activeTab === 'dependentes' ? 'active' : ''}`}
            onClick={() => setActiveTab('dependentes')}
          >
            <Users size={16} />
            <span>
              Dependentes{associado ? ` (${dependentesAtivos.length})` : ''}
            </span>
          </button>

          <button
            type="button"
            className={`profile-tab-btn ${activeTab === 'pagamentos' ? 'active' : ''}`}
            onClick={() => setActiveTab('pagamentos')}
          >
            <CreditCard size={16} />
            <span>Histórico de Pagamento</span>
          </button>
        </div>

        {mensagemSucesso && (
          <div className="profile-alert success">
            <CheckCircle2 size={16} />
            <span>{mensagemSucesso}</span>
          </div>
        )}

        {(mensagemErro || erro) && (
          <div className="profile-alert error">
            <AlertCircle size={16} />
            <span>{mensagemErro || erro}</span>
          </div>
        )}

        {/* TAB 1: DADOS PESSOAIS */}
        {activeTab === 'dados' && (
          <div className="profile-section">
            {carregando ? (
              <div className="profile-empty-state">
                <p>Carregando dados cadastrais...</p>
              </div>
            ) : naoVinculado || !associado ? (
              <>
                <div className="profile-grid">
                  <div className="profile-field">
                    <label className="profile-label">Nome da Conta</label>
                    <div className="profile-value strong">{usuario.nome}</div>
                  </div>
                  <div className="profile-field">
                    <label className="profile-label">E-mail de Acesso</label>
                    <div className="profile-value">{usuario.email}</div>
                  </div>
                </div>
                <div className="profile-info-footnote">
                  <AlertCircle size={15} />
                  <span>
                    Sua conta ainda não está vinculada a um cadastro de associado
                    titular no clube. Procure a recepção ou administração para
                    concluir seu cadastro.
                  </span>
                </div>
              </>
            ) : (
              <>
                <div className="profile-section-header">
                  <div>
                    <h3 className="profile-section-title">
                      Informações Pessoais
                    </h3>
                    <p className="profile-section-desc">
                      Dados do associado titular registrados no clube
                    </p>
                  </div>
                  {!editando ? (
                    <button
                      type="button"
                      className="profile-btn-edit"
                      onClick={() => setEditando(true)}
                    >
                      <Edit2 size={15} />
                      <span>Editar dados</span>
                    </button>
                  ) : (
                    <div className="profile-edit-actions">
                      <button
                        type="button"
                        className="profile-btn-cancel"
                        onClick={() => {
                          setEditando(false)
                          setNomeEdit(associado.pessoa.nome)
                          setDataNascEdit(associado.pessoa.dataNascimento)
                        }}
                      >
                        Cancelar
                      </button>
                      <button
                        type="button"
                        className="profile-btn-save"
                        onClick={() => void handleSalvarDados()}
                        disabled={salvando}
                      >
                        <Save size={15} />
                        <span>{salvando ? 'Salvando...' : 'Salvar'}</span>
                      </button>
                    </div>
                  )}
                </div>

                <div className="profile-grid">
                  <div className="profile-field">
                    <label className="profile-label">Nome Completo</label>
                    {editando ? (
                      <input
                        type="text"
                        className="profile-input"
                        value={nomeEdit}
                        onChange={(e) => setNomeEdit(e.target.value)}
                      />
                    ) : (
                      <div className="profile-value strong">
                        {associado.pessoa.nome}
                      </div>
                    )}
                  </div>

                  <div className="profile-field">
                    <label className="profile-label">CPF (Titular)</label>
                    <div className="profile-value code">
                      {formatarCpf(associado.pessoa.cpf)}
                    </div>
                  </div>

                  <div className="profile-field">
                    <label className="profile-label">Data de Nascimento</label>
                    {editando ? (
                      <input
                        type="date"
                        className="profile-input"
                        value={dataNascEdit}
                        onChange={(e) => setDataNascEdit(e.target.value)}
                      />
                    ) : (
                      <div className="profile-value">
                        {formatarDataBr(associado.pessoa.dataNascimento)} (
                        {calcularIdade(associado.pessoa.dataNascimento)} anos)
                      </div>
                    )}
                  </div>

                  <div className="profile-field">
                    <label className="profile-label">E-mail Cadastrado</label>
                    <div className="profile-value">{usuario.email}</div>
                  </div>

                  <div className="profile-field">
                    <label className="profile-label">
                      Categoria de Associado
                    </label>
                    <div className="profile-value">
                      Sócio {CATEGORIA_LABEL[associado.categoria]}
                    </div>
                  </div>

                  <div className="profile-field">
                    <label className="profile-label">Data de Admissão</label>
                    <div className="profile-value">
                      {formatarDataBr(associado.dataAdmissao)}
                    </div>
                  </div>
                </div>

                {/* Plano Card Details (valores reais vindos do back-end) */}
                <div className="profile-plan-box">
                  <div className="profile-plan-top">
                    <div>
                      <span className="profile-plan-badge">Plano Vinculado</span>
                      <h4 className="profile-plan-title">
                        {nomePlano(associado.plano)}
                      </h4>
                    </div>
                    <div className="profile-plan-price">
                      {formatarMoeda(associado.plano.valorPeriodico)}
                      <span className="profile-plan-period">/mês</span>
                    </div>
                  </div>

                  <div className="profile-plan-features">
                    <div className="plan-feature-pill">
                      <ShieldCheck size={14} />
                      <span>
                        Até {associado.plano.limiteMensalReservas} reservas/mês
                      </span>
                    </div>
                    <div className="plan-feature-pill">
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
          </div>
        )}

        {/* TAB 2: DEPENDENTES */}
        {activeTab === 'dependentes' && (
          <div className="profile-section">
            {!associado ? (
              <div className="profile-empty-state">
                <p>
                  Os dependentes aparecerão aqui assim que seu cadastro de
                  associado titular for vinculado pela recepção.
                </p>
              </div>
            ) : (
              <>
                <div className="profile-section-header">
                  <div>
                    <h3 className="profile-section-title">
                      Dependentes Vinculados
                    </h3>
                    <p className="profile-section-desc">
                      Dependentes cadastrados no seu título para acesso ao clube
                      e reservas
                    </p>
                  </div>
                  <div className="profile-dependents-counter">
                    <span>
                      {dependentesAtivos.length} de{' '}
                      {associado.plano.maxDependentesAtivos} vagas ocupadas
                    </span>
                  </div>
                </div>

                {erroDeps && (
                  <div className="profile-alert error">
                    <AlertCircle size={16} />
                    <span>{erroDeps}</span>
                  </div>
                )}

                {carregandoDeps ? (
                  <div className="profile-empty-state">
                    <p>Carregando dependentes...</p>
                  </div>
                ) : (
                  <div className="profile-dependents-list">
                    {dependentes.length > 0 ? (
                      dependentes.map((vinc) => {
                        const idade = calcularIdade(
                          vinc.dependente.dataNascimento,
                        )
                        const isAtivo = vinc.status === 'ATIVO'

                        return (
                          <div
                            key={vinc.id}
                            className={`dependent-card ${isAtivo ? '' : 'inactive'}`}
                          >
                            <div className="dependent-card-avatar">
                              <Users size={18} />
                            </div>
                            <div className="dependent-card-info">
                              <div className="dependent-card-header">
                                <span className="dependent-name">
                                  {vinc.dependente.nome}
                                </span>
                                <span
                                  className={`dependent-status-badge ${isAtivo ? 'active' : 'inactive'}`}
                                >
                                  {isAtivo
                                    ? 'Vínculo Ativo'
                                    : vinc.motivoInativacao === 'IDADE_LIMITE'
                                      ? 'Inativo (Limite 21 anos)'
                                      : 'Inativo'}
                                </span>
                              </div>

                              <div className="dependent-meta-row">
                                <span>
                                  Parentesco: <strong>{vinc.parentesco}</strong>
                                </span>
                                <span>•</span>
                                <span>
                                  CPF: {formatarCpf(vinc.dependente.cpf)}
                                </span>
                                <span>•</span>
                                <span>
                                  Nascimento:{' '}
                                  {formatarDataBr(
                                    vinc.dependente.dataNascimento,
                                  )}{' '}
                                  ({idade} anos)
                                </span>
                              </div>

                              {isAtivo && idade >= 21 && (
                                <div className="dependent-age-warning">
                                  <AlertCircle size={13} />
                                  <span>
                                    Dependente com 21 anos completados.
                                  </span>
                                </div>
                              )}
                            </div>
                          </div>
                        )
                      })
                    ) : (
                      <div className="profile-empty-state">
                        <p>Nenhum dependente vinculado ao seu cadastro.</p>
                      </div>
                    )}
                  </div>
                )}
              </>
            )}
          </div>
        )}

        {/* TAB 3: HISTÓRICO DE PAGAMENTO (Placeholder até o módulo Financeiro existir no back) */}
        {activeTab === 'pagamentos' && (
          <div className="profile-section">
            <div className="profile-section-header">
              <div>
                <h3 className="profile-section-title">
                  Histórico de Pagamentos e Mensalidades
                </h3>
                <p className="profile-section-desc">
                  Integração com o módulo Financeiro e Mensalidades
                </p>
              </div>
            </div>

            {associado && (
              <div className="profile-pending-invoice">
                <div className="pending-invoice-left">
                  <div className="pending-invoice-icon">
                    <CreditCard size={20} />
                  </div>
                  <div>
                    <span className="pending-invoice-badge">
                      Valor-base do Plano Atual
                    </span>
                    <h4 className="pending-invoice-ref">
                      {nomePlano(associado.plano)}
                    </h4>
                    <p className="pending-invoice-sub">
                      Definido nos parâmetros vigentes do back-end
                    </p>
                  </div>
                </div>
                <div className="pending-invoice-right">
                  <span className="pending-invoice-value">
                    {formatarMoeda(associado.plano.valorPeriodico)}
                  </span>
                </div>
              </div>
            )}

            <div className="profile-placeholder-box">
              <div className="profile-placeholder-icon">
                <Clock size={24} />
              </div>
              <div className="profile-placeholder-text">
                <h4>Módulo Financeiro em desenvolvimento</h4>
                <p>
                  As cobranças de mensalidades, pagamento via PIX/Boleto,
                  histórico de pagamentos e emissão de comprovantes serão
                  exibidos aqui assim que o módulo Financeiro for implementado no
                  back-end.
                </p>
              </div>
            </div>
          </div>
        )}
      </div>
    </Modal>
  )
}
