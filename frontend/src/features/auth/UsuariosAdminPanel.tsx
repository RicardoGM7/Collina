import {
  AlertCircle,
  CheckCircle2,
  Eye,
  EyeOff,
  RefreshCw,
  Search,
  Shield,
  UserPlus,
} from 'lucide-react'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { Modal } from '../../components/Modal/Modal'
import { useAuth } from '../../context/AuthContext'
import { getApiErrorMessage } from '../../services/api'
import { usuariosService } from '../../services/usuariosService'
import {
  PERFIS,
  PERFIL_LABEL,
  type PerfilUsuario,
  type UsuarioApi,
} from '../../types/user'

export function UsuariosAdminPanel() {
  const { usuario: usuarioLogado } = useAuth()

  const [usuarios, setUsuarios] = useState<UsuarioApi[]>([])
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState('')
  const [sucesso, setSucesso] = useState('')

  // Filtros
  const [busca, setBusca] = useState('')
  const [filtroPerfil, setFiltroPerfil] = useState<string>('TODOS')
  const [filtroStatus, setFiltroStatus] = useState<string>('TODOS')

  // Modal: criar novo usuário
  const [modalCriarAberto, setModalCriarAberto] = useState(false)
  const [novoNome, setNovoNome] = useState('')
  const [novoEmail, setNovoEmail] = useState('')
  const [novaSenha, setNovaSenha] = useState('')
  const [mostrarSenha, setMostrarSenha] = useState(false)
  const [novoPerfil, setNovoPerfil] = useState<PerfilUsuario>('RECEPCAO')
  const [criando, setCriando] = useState(false)
  const [erroCriar, setErroCriar] = useState('')

  // Modal: alterar perfil
  const [usuarioEditandoPerfil, setUsuarioEditandoPerfil] =
    useState<UsuarioApi | null>(null)
  const [perfilSelecionado, setPerfilSelecionado] =
    useState<PerfilUsuario>('ASSOCIADO')
  const [salvandoPerfil, setSalvandoPerfil] = useState(false)
  const [erroPerfil, setErroPerfil] = useState('')

  // Controle de loading ao alternar status por uid
  const [alterandoStatusUid, setAlterandoStatusUid] = useState<string | null>(
    null,
  )

  const carregarUsuarios = useCallback(async () => {
    setCarregando(true)
    setErro('')
    try {
      const lista = await usuariosService.listar()
      setUsuarios(lista)
    } catch (err) {
      setErro(
        getApiErrorMessage(
          err,
          'Não foi possível carregar a lista de usuários.',
        ),
      )
    } finally {
      setCarregando(false)
    }
  }, [])

  useEffect(() => {
    void carregarUsuarios()
  }, [carregarUsuarios])

  const usuariosFiltrados = useMemo(() => {
    const termo = busca.trim().toLowerCase()
    return usuarios.filter((u) => {
      const matchPerfil =
        filtroPerfil === 'TODOS' || u.perfil === filtroPerfil
      const matchStatus =
        filtroStatus === 'TODOS' ||
        (filtroStatus === 'ATIVO' ? u.ativo : !u.ativo)
      const matchBusca =
        !termo ||
        (u.nome ?? '').toLowerCase().includes(termo) ||
        u.email.toLowerCase().includes(termo)
      return matchPerfil && matchStatus && matchBusca
    })
  }, [usuarios, busca, filtroPerfil, filtroStatus])

  function exibirSucessoTemporario(msg: string) {
    setSucesso(msg)
    window.setTimeout(() => setSucesso(''), 4500)
  }

  function abrirModalCriar() {
    setNovoNome('')
    setNovoEmail('')
    setNovaSenha('')
    setMostrarSenha(false)
    setNovoPerfil('RECEPCAO')
    setErroCriar('')
    setModalCriarAberto(true)
  }

  async function handleCriarUsuario(e: React.FormEvent) {
    e.preventDefault()
    setErroCriar('')

    if (!novoNome.trim() || !novoEmail.trim() || !novaSenha) {
      setErroCriar('Preencha nome, e-mail e senha.')
      return
    }
    if (novaSenha.length < 6) {
      setErroCriar('A senha deve ter pelo menos 6 caracteres.')
      return
    }

    setCriando(true)
    try {
      const criado = await usuariosService.criar({
        nome: novoNome.trim(),
        email: novoEmail.trim(),
        senha: novaSenha,
        perfil: novoPerfil,
      })
      setUsuarios((prev) => [criado, ...prev])
      setModalCriarAberto(false)
      exibirSucessoTemporario(
        `Conta criada para ${criado.nome ?? criado.email} (${PERFIL_LABEL[novoPerfil]}).`,
      )
    } catch (err) {
      setErroCriar(
        getApiErrorMessage(err, 'Não foi possível criar o usuário.'),
      )
    } finally {
      setCriando(false)
    }
  }

  function abrirModalPerfil(item: UsuarioApi) {
    setUsuarioEditandoPerfil(item)
    setPerfilSelecionado(item.perfil ?? 'ASSOCIADO')
    setErroPerfil('')
  }

  async function handleSalvarPerfil(e: React.FormEvent) {
    e.preventDefault()
    if (!usuarioEditandoPerfil) return
    setErroPerfil('')
    setSalvandoPerfil(true)
    try {
      const atualizado = await usuariosService.alterarPerfil(
        usuarioEditandoPerfil.uid,
        perfilSelecionado,
      )
      setUsuarios((prev) =>
        prev.map((u) => (u.uid === atualizado.uid ? atualizado : u)),
      )
      setUsuarioEditandoPerfil(null)
      exibirSucessoTemporario(
        `Perfil de ${atualizado.nome ?? atualizado.email} atualizado para ${PERFIL_LABEL[perfilSelecionado]}.`,
      )
    } catch (err) {
      setErroPerfil(
        getApiErrorMessage(err, 'Não foi possível alterar o perfil.'),
      )
    } finally {
      setSalvandoPerfil(false)
    }
  }

  async function handleAlternarStatus(item: UsuarioApi) {
    setErro('')
    setSucesso('')
    setAlterandoStatusUid(item.uid)
    const novoStatus = !item.ativo
    try {
      const atualizado = await usuariosService.alterarStatus(
        item.uid,
        novoStatus,
      )
      setUsuarios((prev) =>
        prev.map((u) => (u.uid === atualizado.uid ? atualizado : u)),
      )
      exibirSucessoTemporario(
        `Conta de ${atualizado.nome ?? atualizado.email} ${novoStatus ? 'ativada' : 'desativada'} com sucesso.`,
      )
    } catch (err) {
      setErro(
        getApiErrorMessage(
          err,
          'Não foi possível atualizar o status da conta.',
        ),
      )
    } finally {
      setAlterandoStatusUid(null)
    }
  }

  return (
    <div className="admin-content-card">
      <div
        className="form-card-header"
        style={{
          flexDirection: 'row',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: '12px',
          flexWrap: 'wrap',
        }}
      >
        <div
          style={{
            display: 'flex',
            flexDirection: 'column',
            gap: '4px',
            textAlign: 'left',
          }}
        >
          <h2 className="form-card-title">Usuários e Perfis de Acesso</h2>
          <p className="form-card-desc">
            Crie contas para funcionários e administradores, ajuste perfis de
            acesso e ative ou desative usuários
          </p>
        </div>

        <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
          <button
            type="button"
            className="btn-rotina-idade"
            onClick={() => void carregarUsuarios()}
            disabled={carregando}
            title="Recarregar lista de usuários"
          >
            <RefreshCw size={14} className={carregando ? 'spin-icon' : ''} />
            <span>Atualizar</span>
          </button>

          <button
            type="button"
            className="btn-novo-associado"
            onClick={abrirModalCriar}
          >
            <UserPlus size={15} />
            <span>Novo Usuário</span>
          </button>
        </div>
      </div>

      {sucesso && (
        <div className="profile-alert success">
          <CheckCircle2 size={16} />
          <span>{sucesso}</span>
        </div>
      )}

      {erro && (
        <div className="profile-alert error">
          <AlertCircle size={16} />
          <span>{erro}</span>
        </div>
      )}

      <div className="admin-filters-bar">
        <div className="admin-search-input-wrap">
          <Search size={16} className="search-icon" />
          <input
            type="text"
            placeholder="Buscar por nome ou e-mail..."
            value={busca}
            onChange={(e) => setBusca(e.target.value)}
            className="admin-search-input"
          />
        </div>

        <div className="admin-filters-group">
          <div className="filter-select-wrap">
            <span>Perfil:</span>
            <select
              className="filter-select"
              value={filtroPerfil}
              onChange={(e) => setFiltroPerfil(e.target.value)}
            >
              <option value="TODOS">Todos</option>
              {PERFIS.map((p) => (
                <option key={p} value={p}>
                  {PERFIL_LABEL[p]}
                </option>
              ))}
            </select>
          </div>

          <div className="filter-select-wrap">
            <span>Status:</span>
            <select
              className="filter-select"
              value={filtroStatus}
              onChange={(e) => setFiltroStatus(e.target.value)}
            >
              <option value="TODOS">Todos</option>
              <option value="ATIVO">Ativos</option>
              <option value="INATIVO">Inativos</option>
            </select>
          </div>
        </div>
      </div>

      <div className="admin-table-wrap">
        <table className="admin-table">
          <thead>
            <tr>
              <th>Usuário</th>
              <th>E-mail</th>
              <th>Perfil</th>
              <th>Status</th>
              <th>Ações</th>
            </tr>
          </thead>
          <tbody>
            {carregando ? (
              <tr>
                <td colSpan={5} className="table-empty">
                  Carregando usuários...
                </td>
              </tr>
            ) : usuariosFiltrados.length > 0 ? (
              usuariosFiltrados.map((item) => {
                const ehProprioUsuario = usuarioLogado?.uid === item.uid
                const ocupandoStatus = alterandoStatusUid === item.uid

                return (
                  <tr key={item.uid}>
                    <td>
                      <div className="assoc-name-cell">
                        <span className="assoc-name">
                          {item.nome?.trim() || 'Sem nome'}
                          {ehProprioUsuario ? ' (Você)' : ''}
                        </span>
                      </div>
                    </td>
                    <td>{item.email}</td>
                    <td>
                      <span className="cat-pill">
                        {item.perfil
                          ? PERFIL_LABEL[item.perfil]
                          : 'Sem perfil'}
                      </span>
                    </td>
                    <td>
                      <span
                        className={`status-pill ${item.ativo ? 'status-ativo' : 'status-inativo'}`}
                      >
                        {item.ativo ? 'Ativo' : 'Inativo'}
                      </span>
                    </td>
                    <td>
                      <div className="table-actions">
                        <button
                          type="button"
                          className="btn-action-small"
                          onClick={() => abrirModalPerfil(item)}
                          disabled={ehProprioUsuario}
                          title={
                            ehProprioUsuario
                              ? 'Você não pode alterar o seu próprio perfil de administrador'
                              : 'Alterar perfil de acesso'
                          }
                        >
                          <Shield size={13} />
                          <span>Perfil</span>
                        </button>

                        <button
                          type="button"
                          className="btn-action-small"
                          disabled={ehProprioUsuario || ocupandoStatus}
                          onClick={() => void handleAlternarStatus(item)}
                          title={
                            ehProprioUsuario
                              ? 'Você não pode desativar a sua própria conta'
                              : item.ativo
                                ? 'Desativar acesso do usuário'
                                : 'Reativar acesso do usuário'
                          }
                        >
                          {ocupandoStatus
                            ? 'Salvando...'
                            : item.ativo
                              ? 'Desativar'
                              : 'Ativar'}
                        </button>
                      </div>
                    </td>
                  </tr>
                )
              })
            ) : (
              <tr>
                <td colSpan={5} className="table-empty">
                  Nenhum usuário encontrado com os filtros informados.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {/* Modal: Criar novo usuário */}
      <Modal
        isOpen={modalCriarAberto}
        onClose={() => setModalCriarAberto(false)}
        title="Criar Novo Usuário"
        subtitle="A conta será criada com acesso imediato no perfil selecionado"
        maxWidth={500}
      >
        <form
          className="modal-edit-body"
          onSubmit={(e) => void handleCriarUsuario(e)}
        >
          {erroCriar && (
            <div className="profile-alert error">
              <AlertCircle size={16} />
              <span>{erroCriar}</span>
            </div>
          )}

          <div className="profile-field">
            <label className="profile-label">Nome Completo *</label>
            <input
              type="text"
              required
              className="profile-input"
              placeholder="Ex.: Carlos Ferreira"
              value={novoNome}
              onChange={(e) => setNovoNome(e.target.value)}
            />
          </div>

          <div className="profile-field">
            <label className="profile-label">E-mail de Acesso *</label>
            <input
              type="email"
              required
              className="profile-input"
              placeholder="usuario@collina.com"
              value={novoEmail}
              onChange={(e) => setNovoEmail(e.target.value)}
            />
          </div>

          <div className="profile-field">
            <label className="profile-label">Senha Inicial *</label>
            <div style={{ position: 'relative', display: 'flex' }}>
              <input
                type={mostrarSenha ? 'text' : 'password'}
                required
                minLength={6}
                className="profile-input"
                style={{ width: '100%', paddingRight: '38px' }}
                placeholder="Mínimo 6 caracteres"
                value={novaSenha}
                onChange={(e) => setNovaSenha(e.target.value)}
              />
              <button
                type="button"
                onClick={() => setMostrarSenha((prev) => !prev)}
                aria-label={mostrarSenha ? 'Ocultar senha' : 'Mostrar senha'}
                style={{
                  position: 'absolute',
                  right: '10px',
                  top: '50%',
                  transform: 'translateY(-50%)',
                  background: 'none',
                  border: 'none',
                  color: '#64748B',
                  cursor: 'pointer',
                  padding: '4px',
                  display: 'flex',
                }}
              >
                {mostrarSenha ? <EyeOff size={16} /> : <Eye size={16} />}
              </button>
            </div>
          </div>

          <div className="profile-field">
            <label className="profile-label">Perfil de Acesso *</label>
            <select
              className="profile-input"
              value={novoPerfil}
              onChange={(e) => setNovoPerfil(e.target.value as PerfilUsuario)}
            >
              {PERFIS.map((p) => (
                <option key={p} value={p}>
                  {PERFIL_LABEL[p]}
                </option>
              ))}
            </select>
          </div>

          <div className="modal-edit-actions">
            <button
              type="button"
              className="profile-btn-cancel"
              onClick={() => setModalCriarAberto(false)}
            >
              Cancelar
            </button>
            <button
              type="submit"
              className="profile-btn-save"
              disabled={criando}
            >
              {criando ? 'Criando...' : 'Criar Usuário'}
            </button>
          </div>
        </form>
      </Modal>

      {/* Modal: Alterar perfil */}
      {usuarioEditandoPerfil && (
        <Modal
          isOpen={!!usuarioEditandoPerfil}
          onClose={() => setUsuarioEditandoPerfil(null)}
          title={`Alterar Perfil — ${usuarioEditandoPerfil.nome ?? usuarioEditandoPerfil.email}`}
          subtitle="A alteração de perfil passa a valer imediatamente na próxima requisição"
          maxWidth={460}
        >
          <form
            className="modal-edit-body"
            onSubmit={(e) => void handleSalvarPerfil(e)}
          >
            {erroPerfil && (
              <div className="profile-alert error">
                <AlertCircle size={16} />
                <span>{erroPerfil}</span>
              </div>
            )}

            <p className="modal-edit-text">
              E-mail: <strong>{usuarioEditandoPerfil.email}</strong>
            </p>

            <div className="profile-field">
              <label className="profile-label">Novo Perfil de Acesso</label>
              <select
                className="profile-input"
                value={perfilSelecionado}
                onChange={(e) =>
                  setPerfilSelecionado(e.target.value as PerfilUsuario)
                }
              >
                {PERFIS.map((p) => (
                  <option key={p} value={p}>
                    {PERFIL_LABEL[p]}
                  </option>
                ))}
              </select>
            </div>

            <div className="modal-edit-actions">
              <button
                type="button"
                className="profile-btn-cancel"
                onClick={() => setUsuarioEditandoPerfil(null)}
              >
                Cancelar
              </button>
              <button
                type="submit"
                className="profile-btn-save"
                disabled={salvandoPerfil}
              >
                {salvandoPerfil ? 'Salvando...' : 'Salvar Perfil'}
              </button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  )
}
