// Contexto global de autenticação (usuário, perfil carregado de tb_usuario e expiração por inatividade)
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from 'react'
import {
  createUserWithEmailAndPassword,
  deleteUser,
  onAuthStateChanged,
  sendEmailVerification,
  sendPasswordResetEmail,
  signInWithEmailAndPassword,
  signOut,
  updateProfile,
  type User,
} from 'firebase/auth'
import { auth } from '../config/firebase'
import { api, API_ACTIVITY_EVENT } from '../services/api'
import { isPerfil, type Usuario, type UsuarioApi } from '../types/user'

const TEMPO_INATIVIDADE_MS = 30 * 60 * 1000 // 30 minutos
const INTERVALO_VERIFICACAO_MS = 15 * 1000 // verifica a cada 15s (e imediatamente ao voltar para a aba)
const THROTTLE_ATIVIDADE_MS = 5 * 1000
const STORAGE_ULTIMA_ATIVIDADE = 'collina:ultima-atividade'
const EVENTOS_INTERACAO: ReadonlyArray<keyof WindowEventMap> = [
  'mousemove',
  'mousedown',
  'keydown',
  'scroll',
  'touchstart',
  'click',
]

function lerUltimaAtividade(): number | null {
  try {
    const raw = window.localStorage.getItem(STORAGE_ULTIMA_ATIVIDADE)
    if (!raw) return null
    const parsed = Number(raw)
    return Number.isFinite(parsed) ? parsed : null
  } catch {
    return null
  }
}

function salvarUltimaAtividade(timestamp: number): void {
  try {
    window.localStorage.setItem(STORAGE_ULTIMA_ATIVIDADE, String(timestamp))
  } catch {
    // localStorage indisponível
  }
}

function limparUltimaAtividade(): void {
  try {
    window.localStorage.removeItem(STORAGE_ULTIMA_ATIVIDADE)
  } catch {
    // ignora
  }
}

interface AuthContextValue {
  usuario: Usuario | null
  /** true enquanto o Firebase restaura a sessão (primeiro carregamento da página) */
  carregando: boolean
  /** true quando a última sessão foi encerrada automaticamente por 30 minutos de inatividade */
  sessaoExpiradaPorInatividade: boolean
  limparAvisoSessaoExpirada: () => void
  login: (email: string, senha: string) => Promise<Usuario>
  cadastrar: (
    nome: string,
    email: string,
    senha: string,
    aposSincronizar?: () => Promise<void>,
  ) => Promise<Usuario>
  logout: () => Promise<void>
  recuperarSenha: (email: string) => Promise<void>
}

const AuthContext = createContext<AuthContextValue | null>(null)

function montarUsuario(fb: User, dadosApi: UsuarioApi): Usuario | null {
  if (!dadosApi.ativo || !isPerfil(dadosApi.perfil)) {
    return null
  }
  const email = dadosApi.email || fb.email || ''
  return {
    uid: dadosApi.uid || fb.uid,
    email,
    nome: dadosApi.nome ?? fb.displayName ?? email,
    perfil: dadosApi.perfil,
    emailVerificado: dadosApi.emailVerificado ?? fb.emailVerified,
  }
}

/**
 * Resolve o perfil e status do usuário diretamente no back-end (tb_usuario).
 * - forcarSync = true (login/cadastro): chama POST /api/auth/sincronizar.
 * - forcarSync = false (restauração de sessão): chama GET /api/auth/me e, se a conta ainda não
 *   estiver migrada em tb_usuario (perfil === null), chama POST /api/auth/sincronizar.
 */
async function resolverUsuario(
  fb: User,
  forcarSync: boolean,
): Promise<Usuario | null> {
  if (forcarSync) {
    const { data } = await api.post<UsuarioApi>('/api/auth/sincronizar')
    return montarUsuario(fb, data)
  }

  const { data } = await api.get<UsuarioApi>('/api/auth/me')
  if (!data.perfil) {
    const sincronizado = await api.post<UsuarioApi>('/api/auth/sincronizar')
    return montarUsuario(fb, sincronizado.data)
  }
  return montarUsuario(fb, data)
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [usuario, setUsuario] = useState<Usuario | null>(null)
  const [carregando, setCarregando] = useState(true)
  const [sessaoExpiradaPorInatividade, setSessaoExpiradaPorInatividade] =
    useState(false)
  // Durante login/cadastro o próprio fluxo resolve o usuário; o listener do Firebase fica de fora para não competir
  const fluxoManualRef = useRef(false)
  const ultimaAtividadeRef = useRef<number>(Date.now())

  useEffect(() => {
    return onAuthStateChanged(auth, async (fb) => {
      if (fluxoManualRef.current) return

      if (!fb) {
        limparUltimaAtividade()
        setUsuario(null)
        setCarregando(false)
        return
      }

      // Verifica se a sessão já excedeu o limite de inatividade enquanto a aba/navegador estava fechado
      const ultimaSalva = lerUltimaAtividade()
      const ultimoLoginMs = fb.metadata.lastSignInTime
        ? Date.parse(fb.metadata.lastSignInTime)
        : NaN
      const referenciaAtividade =
        ultimaSalva ?? (Number.isFinite(ultimoLoginMs) ? ultimoLoginMs : null)

      if (
        referenciaAtividade !== null &&
        Date.now() - referenciaAtividade >= TEMPO_INATIVIDADE_MS
      ) {
        limparUltimaAtividade()
        await signOut(auth).catch(() => undefined)
        setSessaoExpiradaPorInatividade(true)
        setUsuario(null)
        setCarregando(false)
        return
      }

      try {
        const resolvido = await resolverUsuario(fb, false)
        if (!resolvido) {
          limparUltimaAtividade()
          await signOut(auth).catch(() => undefined)
          setUsuario(null)
        } else {
          const agora = Date.now()
          ultimaAtividadeRef.current = agora
          salvarUltimaAtividade(agora)
          setUsuario(resolvido)
        }
      } catch {
        // Não foi possível obter o perfil (ex.: conta inativa ou API fora do ar): encerra a sessão
        limparUltimaAtividade()
        await signOut(auth).catch(() => undefined)
        setUsuario(null)
      } finally {
        setCarregando(false)
      }
    })
  }, [])

  // Monitora inatividade (interações na interface e chamadas à API) e encerra a sessão após 30 minutos
  useEffect(() => {
    if (!usuario) return

    const agoraInicial = Date.now()
    ultimaAtividadeRef.current = agoraInicial
    salvarUltimaAtividade(agoraInicial)
    let encerrando = false

    const registrarAtividade = () => {
      const agora = Date.now()
      if (agora - ultimaAtividadeRef.current >= THROTTLE_ATIVIDADE_MS) {
        ultimaAtividadeRef.current = agora
        salvarUltimaAtividade(agora)
      }
    }

    const registrarAtividadeApi = () => {
      const agora = Date.now()
      ultimaAtividadeRef.current = agora
      salvarUltimaAtividade(agora)
    }

    const verificarInatividade = () => {
      if (encerrando) return
      const ultima = Math.max(
        ultimaAtividadeRef.current,
        lerUltimaAtividade() ?? 0,
      )
      if (Date.now() - ultima >= TEMPO_INATIVIDADE_MS) {
        encerrando = true
        limparUltimaAtividade()
        setSessaoExpiradaPorInatividade(true)
        setUsuario(null)
        void signOut(auth).catch(() => undefined)
      }
    }

    const handleVisibilityChange = () => {
      if (document.visibilityState === 'visible') {
        verificarInatividade()
      }
    }

    for (const evento of EVENTOS_INTERACAO) {
      window.addEventListener(evento, registrarAtividade, { passive: true })
    }
    window.addEventListener(API_ACTIVITY_EVENT, registrarAtividadeApi)
    window.addEventListener('focus', verificarInatividade)
    document.addEventListener('visibilitychange', handleVisibilityChange)

    const intervalo = window.setInterval(
      verificarInatividade,
      INTERVALO_VERIFICACAO_MS,
    )

    return () => {
      for (const evento of EVENTOS_INTERACAO) {
        window.removeEventListener(evento, registrarAtividade)
      }
      window.removeEventListener(API_ACTIVITY_EVENT, registrarAtividadeApi)
      window.removeEventListener('focus', verificarInatividade)
      document.removeEventListener('visibilitychange', handleVisibilityChange)
      window.clearInterval(intervalo)
    }
  }, [usuario])

  const limparAvisoSessaoExpirada = useCallback(() => {
    setSessaoExpiradaPorInatividade(false)
  }, [])

  const concluirAcesso = useCallback(async (fb: User): Promise<Usuario> => {
    const resolvido = await resolverUsuario(fb, true)
    if (!resolvido) {
      throw new Error('E-mail ou senha incorretos.')
    }
    const agora = Date.now()
    ultimaAtividadeRef.current = agora
    salvarUltimaAtividade(agora)
    setSessaoExpiradaPorInatividade(false)
    setUsuario(resolvido)
    return resolvido
  }, [])

  const login = useCallback(
    async (email: string, senha: string) => {
      fluxoManualRef.current = true
      setSessaoExpiradaPorInatividade(false)
      try {
        const credencial = await signInWithEmailAndPassword(
          auth,
          email.trim(),
          senha,
        )
        return await concluirAcesso(credencial.user)
      } catch (erro) {
        limparUltimaAtividade()
        setUsuario(null)
        await signOut(auth).catch(() => undefined)
        throw erro
      } finally {
        fluxoManualRef.current = false
      }
    },
    [concluirAcesso],
  )

  const cadastrar = useCallback(
    async (
      nome: string,
      email: string,
      senha: string,
      aposSincronizar?: () => Promise<void>,
    ) => {
      fluxoManualRef.current = true
      setSessaoExpiradaPorInatividade(false)
      let usuarioCriado: User | null = null
      try {
        const credencial = await createUserWithEmailAndPassword(
          auth,
          email.trim(),
          senha,
        )
        usuarioCriado = credencial.user
        await updateProfile(credencial.user, { displayName: nome.trim() })
        // E-mail de verificação é opcional para o uso comum, mas necessário para virar ADMIN por collina.auth.admin-emails
        await sendEmailVerification(credencial.user).catch(() => undefined)
        const resolvido = await resolverUsuario(credencial.user, true)
        if (!resolvido) {
          throw new Error(
            'Não foi possível configurar o perfil de acesso. Procure um administrador do clube.',
          )
        }
        if (aposSincronizar) {
          await aposSincronizar()
        }
        const agora = Date.now()
        ultimaAtividadeRef.current = agora
        salvarUltimaAtividade(agora)
        setUsuario(resolvido)
        return resolvido
      } catch (erro) {
        if (usuarioCriado) {
          // Evita deixar conta órfã no Firebase se o cadastro do associado no banco falhar (ex.: CPF duplicado)
          await deleteUser(usuarioCriado).catch(() => undefined)
        }
        limparUltimaAtividade()
        setUsuario(null)
        await signOut(auth).catch(() => undefined)
        throw erro
      } finally {
        fluxoManualRef.current = false
      }
    },
    [],
  )

  const logout = useCallback(async () => {
    limparUltimaAtividade()
    setSessaoExpiradaPorInatividade(false)
    await signOut(auth)
    setUsuario(null)
  }, [])

  const recuperarSenha = useCallback(async (email: string) => {
    await sendPasswordResetEmail(auth, email.trim())
  }, [])

  const valor = useMemo<AuthContextValue>(
    () => ({
      usuario,
      carregando,
      sessaoExpiradaPorInatividade,
      limparAvisoSessaoExpirada,
      login,
      cadastrar,
      logout,
      recuperarSenha,
    }),
    [
      usuario,
      carregando,
      sessaoExpiradaPorInatividade,
      limparAvisoSessaoExpirada,
      login,
      cadastrar,
      logout,
      recuperarSenha,
    ],
  )

  return <AuthContext.Provider value={valor}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const contexto = useContext(AuthContext)
  if (!contexto) {
    throw new Error('useAuth deve ser usado dentro de <AuthProvider>.')
  }
  return contexto
}
